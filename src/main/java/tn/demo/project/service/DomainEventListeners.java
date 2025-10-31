package tn.demo.project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import tn.demo.common.EmailClientService;
import tn.demo.common.EmailMessage;
import tn.demo.common.domain.ActualSpentTime;
import tn.demo.common.domain.Email;
import tn.demo.project.domain.Project;
import tn.demo.project.domain.ProjectTaskId;
import tn.demo.project.domain.ProjectTaskSnapshot;
import tn.demo.project.events.TaskAddedToProjectEvent;
import tn.demo.project.repository.ProjectRepository;
import tn.demo.team.events.TeamTaskCompletedEvent;

@Component
public class DomainEventListeners {
    private final ProjectRepository projects;
    private final EmailClientService emailClientService;

    private final Email sender;

    private static final Logger log = LoggerFactory.getLogger(DomainEventListeners.class);

    public DomainEventListeners(ProjectRepository projects, EmailClientService emailClientService, @Value("${email.sender}") String sender) {
        this.projects = projects;
        this.emailClientService = emailClientService;
        this.sender = Email.of(sender);
    }

    @Retryable(
            recover = "recoverOptimisticLockingFailureExceptionOnTeamTaskCompletedEvent",
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 200)
    )
    @TransactionalEventListener
    public void on(TeamTaskCompletedEvent teamTaskCompletedEvent) {
        log.atDebug().log(() -> "teamTaskCompletedEvent %s".formatted(teamTaskCompletedEvent));
        projects.findByTaskId(teamTaskCompletedEvent.getProjectTaskId().value())
                .map(project -> markProjectTaskCompleted(teamTaskCompletedEvent, project, teamTaskCompletedEvent.getActualSpentTime()))
                .ifPresentOrElse(projects::save,
                        () -> log.warn("Project by task id not found {}", teamTaskCompletedEvent.getProjectTaskId()));
    }

    @Recover
    public void recoverOptimisticLockingFailureExceptionOnTeamTaskCompletedEvent(OptimisticLockingFailureException ex, TeamTaskCompletedEvent event) {
        log.error("failed to complete task after 5 retries for event: {}", event, ex);
    }

    @TransactionalEventListener
    public void on(TaskAddedToProjectEvent taskAddedToProjectEvent) {
        log.atDebug().log(() -> "taskAddedToProjectEvent %s".formatted(taskAddedToProjectEvent));
        projects.findById(taskAddedToProjectEvent.getToProject().value())
                .ifPresentOrElse(
                        project -> notifyContactPerson(project, taskAddedToProjectEvent.getTaskId()),
                        () -> log.warn("Project not found {}", taskAddedToProjectEvent.getToProject())
                );
    }

    private void notifyContactPerson(Project project, ProjectTaskId taskId) {
        project.getTask(taskId)
                .ifPresentOrElse(
                        task -> attemptToSendEmail(project, taskId, task),
                        () -> log.warn("Task {} not found in project {}", taskId, project.getId())
                );
    }

    private void attemptToSendEmail(Project project, ProjectTaskId taskId, ProjectTaskSnapshot task) {
        project.validContactEmail()
                .ifPresentOrElse(
                        emailRecipient -> emailClientService.send(
                                new EmailMessage(sender, emailRecipient, "Task added", "Task %s was added".formatted(task), false)
                        ),
                        warnInvalidRecipientAddress(project, taskId));
    }

    private Runnable warnInvalidRecipientAddress(Project project, ProjectTaskId taskId) {
        return () -> log.warn("Skipping sending email notification about new task {} for project {}: invalid contact email '{}'",
                taskId, project.getId(), project.contactEmailValue());
    }

    private Project markProjectTaskCompleted(TeamTaskCompletedEvent teamTaskCompletedEvent, Project project, ActualSpentTime actualSpentTime) {
        return project.completeTask((teamTaskCompletedEvent.getProjectTaskId()), actualSpentTime);
    }

}
