package tn.demo.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tn.demo.common.*;
import tn.demo.outbox.*;
import tn.demo.project.domain.*;
import tn.demo.project.repository.ProjectRepository;
import tn.demo.projectapproval.events.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectApprovalResultOutboxProcessorTest {
    @Test
    void deliversApprovedAndRejectedEventsThroughSerializedOutbox() {
        for (boolean approved : new boolean[]{true, false}) {
            var id = new ProjectId(UUID.randomUUID());
            var mapper = new ObjectMapper();
            var outbox = mock(OutboxRepository.class);
            var ids = mock(IDService.class);
            var dates = mock(DateService.class);
            when(ids.newId()).thenReturn(UUID.randomUUID());
            when(dates.now()).thenReturn(LocalDateTime.now());
            var listener = new OutboxEventListener(outbox, mapper, ids, dates);
            if (approved) { listener.on(new ProjectApprovedEvent(id)); }
            else { listener.on(new ProjectRejectedEvent(id)); }
            var captor = ArgumentCaptor.forClass(OutboxEntry.class);
            verify(outbox).save(captor.capture());
            var entry = captor.getValue();
            var projects = mock(ProjectRepository.class);
            var project = mock(Project.class);
            var updated = mock(Project.class);
            when(projects.findById(id.value())).thenReturn(Optional.of(project), Optional.of(updated));
            if (approved) {
                when(project.approve()).thenReturn(updated);
                when(updated.approve()).thenReturn(updated);
            } else {
                when(project.reject()).thenReturn(updated);
                when(updated.reject()).thenReturn(updated);
            }
            var processor = new ProjectApprovalResultOutboxProcessor(mapper, projects);
            assertEquals(processor.listenerId(), entry.getListenerId());
            var item = new ClaimedOutboxItem(entry.getId(), entry.getEventType(), 1, entry.getPayload(),
                    entry.getListenerId(), 1, UUID.randomUUID());
            processor.process(item);
            processor.process(item);
            verify(projects, times(2)).save(updated);
        }
    }
}
