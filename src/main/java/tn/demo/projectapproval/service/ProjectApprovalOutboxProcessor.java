package tn.demo.projectapproval.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.demo.outbox.ClaimedOutboxItem;
import tn.demo.outbox.OutboxItemProcessor;
import tn.demo.project.events.ProjectCreatedEvent;

import java.util.Set;

@Component
public class ProjectApprovalOutboxProcessor implements OutboxItemProcessor {

    public static final String LISTENER_ID = "project-start-approval-process";

    private static final Set<String> PROJECT_CREATED_EVENT_TYPES = Set.of(
            ProjectCreatedEvent.class.getName(),
            "ProjectCreatedEvent"
    );

    private final ObjectMapper objectMapper;
    private final ProjectApprovalService approvals;

    public ProjectApprovalOutboxProcessor(
            ObjectMapper objectMapper,
            ProjectApprovalService approvals) {
        this.objectMapper = objectMapper;
        this.approvals = approvals;
    }

    @Override
    public String listenerId() {
        return LISTENER_ID;
    }

    @Override
    @Transactional
    public void process(ClaimedOutboxItem item) {
        if (!PROJECT_CREATED_EVENT_TYPES.contains(item.eventType())) {
            throw new IllegalArgumentException(
                    "Unsupported event type " + item.eventType());
        }
        if (item.eventVersion() != 1) {
            throw new IllegalArgumentException(
                    "Unsupported ProjectCreatedEvent version " + item.eventVersion());
        }

        try {
            ProjectCreatedEvent event =
                    objectMapper.readValue(item.payload(), ProjectCreatedEvent.class);

            approvals.startIfNotStarted(
                    event.getProjectId(),
                    event.getApprovers());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not deserialize ProjectCreatedEvent", e);
        }
    }
}