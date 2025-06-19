package tn.demo.project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import tn.demo.common.EmailClientService;
import tn.demo.common.EmailMessage;
import tn.demo.common.domain.ActualSpentTime;
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

    private final String sender;

    private static final Logger log = LoggerFactory.getLogger(DomainEventListeners.class);

    public DomainEventListeners(ProjectRepository projects, EmailClientService emailClientService,     @Value("${email.sender}") String sender) {
        this.projects = projects;
        this.emailClientService = emailClientService;
        this.sender = sender;
    }

    @Retryable(
            value = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 200)
    )
    @TransactionalEventListener
    public void on(TeamTaskCompletedEvent teamTaskCompletedEvent){
        log.info("teamTaskCompletedEvent " + teamTaskCompletedEvent.getProjectTaskId());
        projects.findByTaskId(teamTaskCompletedEvent.getProjectTaskId().value())
                .map(project -> markProjectTaskCompleted(teamTaskCompletedEvent, project, teamTaskCompletedEvent.getActualSpentTime()))
                .map(project -> projects.save(project));
    }

    @TransactionalEventListener
    public void on(TaskAddedToProjectEvent event){
        log.info("GOT TaskAddedToProjectEvent projectId = %s, taskId = %s".formatted(event.getToProject(), event.getTaskId()));
        projects.findById(event.getToProject().value())
                .ifPresent(project -> sendEmail(project, event.getTaskId()));
    }

    private void sendEmail(Project project, ProjectTaskId taskId) {
        project.getTask(taskId)
                .ifPresent(task -> sendEmail(project.getContactPersonEmail(), task));
    }

    private void sendEmail(String contactPersonEmail, ProjectTaskSnapshot task) {
        emailClientService.send(new EmailMessage(sender, contactPersonEmail, "Task added", "Task %s was added".formatted(task), false));
    }

    private Project markProjectTaskCompleted(TeamTaskCompletedEvent teamTaskCompletedEvent, Project project, ActualSpentTime actualSpentTime) {
        return project.completeTask((teamTaskCompletedEvent.getProjectTaskId()), actualSpentTime);
    }

}
