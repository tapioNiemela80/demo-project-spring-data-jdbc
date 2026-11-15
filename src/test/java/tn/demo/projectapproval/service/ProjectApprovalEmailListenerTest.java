package tn.demo.projectapproval.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tn.demo.common.*;
import tn.demo.common.domain.ApproverId;
import tn.demo.common.domain.EmailAddress;
import tn.demo.project.domain.*;
import tn.demo.projectapproval.events.ProjectApprovalStartedEvent;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectApprovalEmailListenerTest {
    @Test
    void attemptsEveryRecipientDespiteFailureAndIncludesPostInstructions() {
        var emails = mock(EmailClientService.class);
        var policy = mock(EmailNotificationPolicy.class);
        when(policy.notificationToEmailIsAllowed(any())).thenReturn(true);
        doThrow(new RuntimeException("mail unavailable")).doNothing().when(emails).send(any());
        var projectId = new ProjectId(UUID.randomUUID());
        var first = new ApproverSnapshot(new ApproverId(UUID.randomUUID()), "A", "a@example.com");
        var second = new ApproverSnapshot(new ApproverId(UUID.randomUUID()), "B", "b@example.com");
        var listener = new ProjectApprovalEmailListener(emails, policy, "sender@example.com", "http://localhost:8080");
        assertDoesNotThrow(() -> listener.on(new ProjectApprovalStartedEvent(projectId, Set.of(first, second))));
        var captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emails, times(2)).send(captor.capture());
        for (var message : captor.getAllValues()) {
            assertTrue(message.content().contains("POST http://localhost:8080/projects/" + projectId.value() + "/approvals/"));
            assertTrue(message.content().contains("APPROVE or REJECT"));
        }
    }
    @Test
    void skipsDeniedRecipientAndSendsToAllowedRecipient() {
        var emails = mock(EmailClientService.class);
        var policy = mock(EmailNotificationPolicy.class);
        var allowed = EmailAddress.of("allowed@example.com");
        var denied = EmailAddress.of("denied@example.com");
        when(policy.notificationToEmailIsAllowed(allowed)).thenReturn(true);
        when(policy.notificationToEmailIsAllowed(denied)).thenReturn(false);
        var event = new ProjectApprovalStartedEvent(new ProjectId(UUID.randomUUID()), Set.of(
                new ApproverSnapshot(new ApproverId(UUID.randomUUID()), "Allowed", allowed.value()),
                new ApproverSnapshot(new ApproverId(UUID.randomUUID()), "Denied", denied.value())));
        new ProjectApprovalEmailListener(emails, policy, "sender@example.com", "http://localhost:8080").on(event);
        var captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emails).send(captor.capture());
        assertEquals(allowed, captor.getValue().to());
        verify(policy).notificationToEmailIsAllowed(denied);
    }

    @Test
    void doesNotSendWhenPolicyCheckFails() {
        var emails = mock(EmailClientService.class);
        var policy = mock(EmailNotificationPolicy.class);
        when(policy.notificationToEmailIsAllowed(any())).thenThrow(new RuntimeException("policy unavailable"));
        var event = new ProjectApprovalStartedEvent(new ProjectId(UUID.randomUUID()), Set.of(
                new ApproverSnapshot(new ApproverId(UUID.randomUUID()), "Approver", "a@example.com")));
        var listener = new ProjectApprovalEmailListener(emails, policy, "sender@example.com", "http://localhost:8080");
        assertDoesNotThrow(() -> listener.on(event));
        verifyNoInteractions(emails);
    }
}
