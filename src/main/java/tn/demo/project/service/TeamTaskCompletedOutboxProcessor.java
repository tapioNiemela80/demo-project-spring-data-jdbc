package tn.demo.project.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.demo.common.domain.ActualSpentTime;
import tn.demo.outbox.ClaimedOutboxItem;
import tn.demo.outbox.OutboxItemProcessor;
import tn.demo.project.domain.ProjectTaskId;
import tn.demo.project.repository.ProjectRepository;
import tn.demo.team.events.TeamTaskCompletedEvent;
import tn.demo.team.events.TeamTaskCompletedPayload;

@Component
public class TeamTaskCompletedOutboxProcessor implements OutboxItemProcessor {
    public static final String LISTENER_ID = "project-complete-task";

    private final ObjectMapper objectMapper;
    private final ProjectRepository projects;

    public TeamTaskCompletedOutboxProcessor(ObjectMapper objectMapper, ProjectRepository projects) {
        this.objectMapper = objectMapper;
        this.projects = projects;
    }

    @Override
    public String listenerId() {
        return LISTENER_ID;
    }

    @Override
    @Transactional
    public void process(ClaimedOutboxItem item) {
        if (!TeamTaskCompletedEvent.class.getName().equals(item.eventType())) {
            throw new IllegalArgumentException("Unsupported event type " + item.eventType());
        }
        if (item.eventVersion() != 1) {
            throw new IllegalArgumentException("Unsupported TeamTaskCompletedEvent version " + item.eventVersion());
        }
        try {
            var event = objectMapper.readValue(item.payload(), TeamTaskCompletedPayload.class);
            var project = projects.findByTaskId(event.projectTaskId())
                    .orElseThrow(() -> new IllegalStateException("Project by task id not found: " + event.projectTaskId()));
            // Completion assigns status and time; replaying the same event does not accumulate time.
            projects.save(project.completeTask(new ProjectTaskId(event.projectTaskId()),
                    new ActualSpentTime(event.hours(), event.minutes())));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not deserialize TeamTaskCompletedEvent", e);
        }
    }
}
