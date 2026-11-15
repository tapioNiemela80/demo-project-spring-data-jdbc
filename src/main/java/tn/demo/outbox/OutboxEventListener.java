package tn.demo.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tn.demo.common.DateService;
import tn.demo.common.IDService;
import tn.demo.project.domain.Project;
import tn.demo.project.service.ProjectApprovalResultOutboxProcessor;
import tn.demo.projectapproval.domain.ProjectApproval;
import tn.demo.projectapproval.events.ProjectApprovedEvent;
import tn.demo.projectapproval.events.ProjectRejectedEvent;
import org.springframework.context.ApplicationEvent;
import tn.demo.project.domain.ProjectId;
import tn.demo.project.events.ProjectCompletedEvent;
import tn.demo.project.service.TeamTaskCompletedOutboxProcessor;
import tn.demo.team.domain.Team;
import tn.demo.team.events.TeamTaskCompletedEvent;
import tn.demo.team.events.TeamTaskCompletedPayload;

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
    public void on(ProjectCompletedEvent event) {
        outbox.save(OutboxEntry.pending(
                ids.newId(),
                event.getClass(),
                1,
                serialize(event),
                dates.now(),
                Project.class,
                event.getProjectId().value(),
                null,
                "project-start-approval-process"
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(TeamTaskCompletedEvent event) {
        outbox.save(OutboxEntry.pending(
                ids.newId(),
                TeamTaskCompletedEvent.class,
                1,
                serialize(TeamTaskCompletedPayload.from(event)),
                dates.now(),
                Team.class,
                event.getTeamId(),
                null,
                TeamTaskCompletedOutboxProcessor.LISTENER_ID
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(ProjectApprovedEvent event) { saveApprovalResult(event, event.getProjectId()); }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(ProjectRejectedEvent event) { saveApprovalResult(event, event.getProjectId()); }

    private void saveApprovalResult(ApplicationEvent event, ProjectId projectId) {
        outbox.save(OutboxEntry.pending(ids.newId(), event.getClass(), 1, serialize(event), dates.now(),
                ProjectApproval.class, projectId.value(), null, ProjectApprovalResultOutboxProcessor.LISTENER_ID));
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize outbox event", e);
        }
    }
}
