package tn.demo.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import tn.demo.project.events.ProjectCompletedEvent;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.OptimisticLockingFailureException;
import tn.demo.common.DateService;
import tn.demo.common.IDService;
import tn.demo.common.domain.ActualSpentTime;
import tn.demo.outbox.*;
import tn.demo.project.domain.Project;
import tn.demo.project.domain.ProjectTaskId;
import tn.demo.project.repository.ProjectRepository;
import tn.demo.team.domain.TeamTaskId;
import tn.demo.team.events.TeamTaskCompletedEvent;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamTaskCompletedOutboxProcessorTest {
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final TeamTaskCompletedOutboxProcessor processor = new TeamTaskCompletedOutboxProcessor(mapper, projects, events);
    private final ProjectTaskId taskId = new ProjectTaskId(UUID.randomUUID());

    private ClaimedOutboxItem enqueue() {
        var repository = mock(OutboxRepository.class);
        var ids = mock(IDService.class);
        var dates = mock(DateService.class);
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(dates.now()).thenReturn(LocalDateTime.now());
        var event = new TeamTaskCompletedEvent(UUID.randomUUID(),
                new TeamTaskId(UUID.randomUUID()), taskId, ActualSpentTime.fromMinutes(75));
        new OutboxEventListener(repository, mapper, ids, dates).on(event);
        var captor = ArgumentCaptor.forClass(OutboxEntry.class);
        verify(repository).save(captor.capture());
        var entry = captor.getValue();
        assertEquals(processor.listenerId(), entry.getListenerId());
        return new ClaimedOutboxItem(entry.getId(), entry.getEventType(), entry.getEventVersion(),
                entry.getPayload(), entry.getListenerId(), 1, UUID.randomUUID());
    }

    @Test
    void processesSerializedCompletion() {
        var item = enqueue();
        var project = mock(Project.class);
        var completed = mock(Project.class);
        when(projects.findByTaskId(taskId.value())).thenReturn(Optional.of(project));
        when(project.completeTask(taskId, ActualSpentTime.fromMinutes(75))).thenReturn(completed);
        processor.process(item);
        verify(projects).save(completed);
        verifyNoInteractions(events);
    }

    @Test
    void propagatesOptimisticLockFailureForOutboxRetry() {
        var item = enqueue();
        var project = mock(Project.class);
        when(projects.findByTaskId(taskId.value())).thenReturn(Optional.of(project));
        when(project.completeTask(taskId, ActualSpentTime.fromMinutes(75))).thenReturn(project);
        var failure = new OptimisticLockingFailureException("concurrent update");
        when(projects.save(project)).thenThrow(failure);
        assertSame(failure, assertThrows(OptimisticLockingFailureException.class, () -> processor.process(item)));
        verifyNoInteractions(events);
    }

    @Test
    void missingProjectIsNotAcknowledgedAsSuccess() {
        var item = enqueue();
        when(projects.findByTaskId(taskId.value())).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> processor.process(item));
        verify(projects, never()).save(any());
    }
    @Test
    void publishesCompletionOnlyOnTransitionToCompleted() {
        var item = enqueue();
        var project = mock(Project.class);
        var completed = mock(Project.class);
        var projectId = UUID.randomUUID();
        when(projects.findByTaskId(taskId.value())).thenReturn(Optional.of(project), Optional.of(completed));
        when(project.completeTask(taskId, ActualSpentTime.fromMinutes(75))).thenReturn(completed);
        when(completed.completeTask(taskId, ActualSpentTime.fromMinutes(75))).thenReturn(completed);
        when(completed.isCompleted()).thenReturn(true);
        when(completed.getId()).thenReturn(projectId);
        when(completed.approversSnapshot()).thenReturn(java.util.Set.of());
        processor.process(item);
        processor.process(item);
        var captor = ArgumentCaptor.forClass(ProjectCompletedEvent.class);
        verify(events, times(1)).publishEvent(captor.capture());
        assertEquals(projectId, captor.getValue().getProjectId().value());
    }
}
