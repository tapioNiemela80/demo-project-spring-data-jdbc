package tn.demo.projectapproval.controller;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.server.ResponseStatusException;
import tn.demo.project.domain.ProjectId;
import tn.demo.projectapproval.domain.ApprovalDecision;
import tn.demo.projectapproval.service.ProjectApprovalService;
import java.util.UUID;
import java.util.NoSuchElementException;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectApprovalControllerTest {
    private final ProjectApprovalService service = mock(ProjectApprovalService.class);
    private final ProjectApprovalController controller = new ProjectApprovalController(service);
    private final UUID project = UUID.randomUUID();
    private final UUID approver = UUID.randomUUID();

    @Test
    void acceptsPlainDecisionsAndRejectsInvalidInput() {
        controller.decide(project, approver, "APPROVE");
        verify(service).decide(new ProjectId(project), approver, ApprovalDecision.APPROVE);
        controller.decide(project, approver, "REJECT");
        verify(service).decide(new ProjectId(project), approver, ApprovalDecision.REJECT);
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> controller.decide(project, approver, "PENDING")).getStatusCode().value());
    }

    @Test
    void mapsUnknownResourcesAndConcurrentOrFinalDecisions() {
        doThrow(new NoSuchElementException()).when(service).decide(any(), any(), any());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> controller.decide(project, approver, "APPROVE")).getStatusCode().value());
        doThrow(new OptimisticLockingFailureException("conflict")).when(service).decide(any(), any(), any());
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> controller.decide(project, approver, "APPROVE")).getStatusCode().value());
        doThrow(new IllegalStateException("finished")).when(service).decide(any(), any(), any());
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> controller.decide(project, approver, "APPROVE")).getStatusCode().value());
    }
}
