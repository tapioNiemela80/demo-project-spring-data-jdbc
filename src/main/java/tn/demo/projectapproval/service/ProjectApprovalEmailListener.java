package tn.demo.projectapproval.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import tn.demo.common.EmailClientService;
import tn.demo.common.EmailMessage;
import tn.demo.common.domain.EmailAddress;
import tn.demo.projectapproval.events.ProjectApprovalStartedEvent;

@Component
public class ProjectApprovalEmailListener {
    private static final Logger log = LoggerFactory.getLogger(ProjectApprovalEmailListener.class);
    private final EmailClientService emails;
    private final EmailAddress sender;
    private final String baseUrl;

    public ProjectApprovalEmailListener(EmailClientService emails, @Value("${email.sender}") String sender,
                                       @Value("${project-approval.base-url:http://localhost:8080}") String baseUrl) {
        this.emails = emails;
        this.sender = EmailAddress.of(sender);
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @TransactionalEventListener
    public void on(ProjectApprovalStartedEvent event) {
        for (var approver : event.approvers()) {
            try {
                String endpoint = baseUrl + "/projects/" + event.projectId().value()
                        + "/approvals/" + approver.approverId().value();
                emails.send(new EmailMessage(sender, EmailAddress.of(approver.emailAddress()),
                        "Project ready for approval", "Project " + event.projectId().value()
                        + " is completed. Submit your decision using POST " + endpoint
                        + " with Content-Type: text/plain and body APPROVE or REJECT.", false));
            } catch (Exception failure) {
                log.warn("Could not send approval request for project {} to approver {}",
                        event.projectId(), approver.approverId(), failure);
            }
        }
    }
}
