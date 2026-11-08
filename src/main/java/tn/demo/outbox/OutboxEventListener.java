package tn.demo.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tn.demo.common.DateService;
import tn.demo.common.IDService;
import tn.demo.project.events.ProjectCreatedEvent;

@Component
public class OutboxEventListener {

    private final OutboxRepository outbox;
    private final ObjectMapper objectMapper;
    private final IDService ids;
    private final DateService dates;

    public OutboxEventListener(
            OutboxRepository outbox,
            ObjectMapper objectMapper,
            IDService ids,
            DateService dates
    ) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.ids = ids;
        this.dates = dates;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(ProjectCreatedEvent event) {
        outbox.save(OutboxEntry.pending(
                ids.newId(),
                "ProjectCreatedEvent",
                1,
                serialize(event),
                dates.now(),
                "Project",
                event.getProjectId().value(),
                1,
                "project-approval-process"
        ));
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize outbox event", e);
        }
    }
}