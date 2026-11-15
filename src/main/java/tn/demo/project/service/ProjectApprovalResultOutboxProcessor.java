package tn.demo.project.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.demo.outbox.ClaimedOutboxItem;
import tn.demo.outbox.OutboxItemProcessor;
import tn.demo.project.domain.ProjectId;
import tn.demo.project.repository.ProjectRepository;
import tn.demo.projectapproval.events.ProjectApprovedEvent;
import tn.demo.projectapproval.events.ProjectRejectedEvent;

@Component
public class ProjectApprovalResultOutboxProcessor implements OutboxItemProcessor {
    public static final String LISTENER_ID = "project-approval-result";
    private final ObjectMapper mapper;
    private final ProjectRepository projects;

    public ProjectApprovalResultOutboxProcessor(ObjectMapper mapper, ProjectRepository projects) {
        this.mapper = mapper;
        this.projects = projects;
    }

    @Override
    public String listenerId() { return LISTENER_ID; }

    @Override
    @Transactional
    public void process(ClaimedOutboxItem item) {
        if (item.eventVersion() != 1) {
            throw new IllegalArgumentException("Unsupported approval result version: " + item.eventVersion());
        }
        try {
            ProjectId id;
            boolean approved;
            if (ProjectApprovedEvent.class.getName().equals(item.eventType())) {
                id = mapper.readValue(item.payload(), ProjectApprovedEvent.class).getProjectId();
                approved = true;
            } else if (ProjectRejectedEvent.class.getName().equals(item.eventType())) {
                id = mapper.readValue(item.payload(), ProjectRejectedEvent.class).getProjectId();
                approved = false;
            } else {
                throw new IllegalArgumentException("Unsupported approval result: " + item.eventType());
            }
            var project = projects.findById(id.value())
                    .orElseThrow(() -> new IllegalStateException("Project not found: " + id));
            projects.save(approved ? project.approve() : project.reject());
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Could not deserialize approval result", ex);
        }
    }
}
