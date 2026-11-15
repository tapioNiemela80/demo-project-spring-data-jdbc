package tn.demo.projectapproval.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import tn.demo.common.DateService;
import tn.demo.common.domain.ApproverId;
import tn.demo.project.domain.*;
import tn.demo.projectapproval.domain.*;
import tn.demo.projectapproval.events.*;
import tn.demo.projectapproval.repository.ProjectApprovalRepository;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectApprovalDecisionTest {
    private final ProjectApprovalRepository repository = mock(ProjectApprovalRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final DateService dates = mock(DateService.class);
    private final ProjectApprovalService service = new ProjectApprovalService(repository, dates, events);
    private final ProjectId id = new ProjectId(UUID.randomUUID());
    private final UUID approver = UUID.randomUUID();
    private ProjectApproval start() {
        return ProjectApproval.start(id, Set.of(new ApproverSnapshot(new ApproverId(approver), "A", "a@example.com")), LocalDateTime.now());
    }

    @Test
    void publishesApprovalAfterSaveAndDoesNotRepublishOnRepeat() {
        var approval = start();
        when(repository.findById(id.value())).thenReturn(Optional.of(approval),
                Optional.of(approval.decide(approver, ApprovalDecision.APPROVE, LocalDateTime.now())));
        service.decide(id, approver, ApprovalDecision.APPROVE);
        service.decide(id, approver, ApprovalDecision.APPROVE);
        var order = inOrder(repository, events);
        order.verify(repository).save(any());
        order.verify(events).publishEvent(isA(ProjectApprovedEvent.class));
        verify(repository, times(1)).save(any());
        verify(events, times(1)).publishEvent(isA(ProjectApprovedEvent.class));
    }

    @Test
    void publishesRejection() {
        when(repository.findById(id.value())).thenReturn(Optional.of(start()));
        service.decide(id, approver, ApprovalDecision.REJECT);
        verify(events).publishEvent(isA(ProjectRejectedEvent.class));
    }

    @Test
    void optimisticLockFailureCannotPublishOutcome() {
        when(repository.findById(id.value())).thenReturn(Optional.of(start()));
        when(repository.save(any())).thenThrow(new OptimisticLockingFailureException("conflict"));
        assertThrows(OptimisticLockingFailureException.class, () -> service.decide(id, approver, ApprovalDecision.APPROVE));
        verifyNoInteractions(events);
    }
}
