package tn.demo.projectapproval.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tn.demo.common.DateService;
import tn.demo.common.IDService;
import tn.demo.common.domain.ApproverId;
import tn.demo.outbox.*;
import tn.demo.project.domain.ApproverSnapshot;
import tn.demo.project.domain.ProjectId;
import tn.demo.project.events.ProjectCompletedEvent;
import tn.demo.project.events.ProjectCreatedEvent;
import tn.demo.projectapproval.repository.ProjectApprovalRepository;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectApprovalOutboxProcessorTest {
    @Test
    void completedEventSurvivesOutboxSerializationAndStartsApprovalOnlyOnce() {
        var mapper = new ObjectMapper();
        var outbox = mock(OutboxRepository.class);
        var ids = mock(IDService.class);
        var dates = mock(DateService.class);
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(dates.now()).thenReturn(LocalDateTime.now());
        var projectId = new ProjectId(UUID.randomUUID());
        var approvers = Set.of(new ApproverSnapshot(new ApproverId(UUID.randomUUID()), "Approver", "a@example.com"));
        new OutboxEventListener(outbox, mapper, ids, dates).on(new ProjectCompletedEvent(projectId, approvers));
        var captor = ArgumentCaptor.forClass(OutboxEntry.class);
        verify(outbox).save(captor.capture());
        var entry = captor.getValue();
        var item = new ClaimedOutboxItem(entry.getId(), entry.getEventType(), entry.getEventVersion(),
                entry.getPayload(), entry.getListenerId(), 1, UUID.randomUUID());
        var repository = mock(ProjectApprovalRepository.class);
        when(repository.existsById(projectId.value())).thenReturn(false, true);
        var processor = new ProjectApprovalOutboxProcessor(mapper, new ProjectApprovalService(repository, dates));
        assertEquals(processor.listenerId(), entry.getListenerId());
        processor.process(item);
        processor.process(item);
        verify(repository, times(1)).save(any());
    }

    @Test
    void projectCreatedEventCannotStartApproval() {
        var approvals = mock(ProjectApprovalService.class);
        var processor = new ProjectApprovalOutboxProcessor(new ObjectMapper(), approvals);
        var item = new ClaimedOutboxItem(UUID.randomUUID(), ProjectCreatedEvent.class.getName(), 1,
                "{}", processor.listenerId(), 1, UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () -> processor.process(item));
        verifyNoInteractions(approvals);
    }
}
