package tn.demo.project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import tn.demo.common.EmailClientService;
import tn.demo.common.EmailMessage;
import tn.demo.common.domain.EmailAddress;
import tn.demo.project.domain.EmailNotificationPolicy;
import tn.demo.project.domain.Project;
import tn.demo.project.domain.ProjectTaskId;
import tn.demo.project.domain.ProjectTaskSnapshot;
import tn.demo.project.events.TaskAddedToProjectEvent;
import tn.demo.project.repository.ProjectRepository;

@Component
public class DomainEventListeners {
    private final ProjectRepository projects;
    private final EmailClientService emailClientService;
    private final EmailNotificationPolicy emailNotificationPolicy;

    private final EmailAddress sender;

    private static final Logger log = LoggerFactory.getLogger(DomainEventListeners.class);

    public DomainEventListeners(ProjectRepository projects, EmailClientService emailClientService,
                                EmailNotificationPolicy emailNotificationPolicy,
                                @Value("${email.sender}") String sender) {
        this.projects = projects;
        this.emailClientService = emailClientService;
        this.emailNotificationPolicy = emailNotificationPolicy;
        this.sender = EmailAddress.of(sender);
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
                        emailRecipient -> sendMailIfPolicyAllows(emailRecipient, project, taskId, task),
                        warnInvalidRecipientAddress(project, taskId));
    }

    private void sendMailIfPolicyAllows(EmailAddress emailRecipient, Project project, ProjectTaskId taskId, ProjectTaskSnapshot task) {
        if (!emailNotificationPolicy.notificationToEmailIsAllowed(emailRecipient)) {
            log.info("Skipping sending email about new task {} to project {}, policy denied", taskId, project.getId());
            return;
        }
        emailClientService.send(
                new EmailMessage(sender, emailRecipient, "Task added", "Task %s was added".formatted(task), false));
    }

    private Runnable warnInvalidRecipientAddress(Project project, ProjectTaskId taskId) {
        return () -> log.warn("Skipping sending email notification about new task {} for project {}: invalid contact email",
                taskId, project.getId());
    }

}
