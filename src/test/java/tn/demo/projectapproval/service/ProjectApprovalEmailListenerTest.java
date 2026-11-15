package tn.demo.projectapproval.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tn.demo.common.*;
import tn.demo.common.domain.ApproverId;
import tn.demo.project.domain.*;
import tn.demo.projectapproval.events.ProjectApprovalStartedEvent;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectApprovalEmailListenerTest {
    @Test
    void attemptsEveryRecipientDespiteFailureAndIncludesPostInstructions() {
        var emails = mock(EmailClientService.class);
        doThrow(new RuntimeException("mail unavailable")).doNothing().when(emails).send(any());
        var projectId = new ProjectId(UUID.randomUUID());
        var first = new ApproverSnapshot(new ApproverId(UUID.randomUUID()), "A", "a@example.com");
        var second = new ApproverSnapshot(new ApproverId(UUID.randomUUID()), "B", "b@example.com");
        var listener = new ProjectApprovalEmailListener(emails, "sender@example.com", "http://localhost:8080");
        assertDoesNotThrow(() -> listener.on(new ProjectApprovalStartedEvent(projectId, Set.of(first, second))));
        var captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emails, times(2)).send(captor.capture());
        for (var message : captor.getAllValues()) {
            assertTrue(message.content().contains("POST http://localhost:8080/projects/" + projectId.value() + "/approvals/"));
            assertTrue(message.content().contains("APPROVE or REJECT"));
        }
    }
}
