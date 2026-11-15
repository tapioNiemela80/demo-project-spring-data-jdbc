package tn.demo.projectapproval.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.context.ApplicationEvent;
import tn.demo.project.domain.ProjectId;

@JsonIgnoreProperties({"source", "timestamp"})
public class ProjectRejectedEvent extends ApplicationEvent {
    private final ProjectId projectId;

    @JsonCreator
    public ProjectRejectedEvent(@JsonProperty("projectId") ProjectId projectId) {
        super(projectId);
        this.projectId = projectId;
    }

    public ProjectId getProjectId() { return projectId; }
}
