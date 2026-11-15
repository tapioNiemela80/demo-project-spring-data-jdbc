package tn.demo.project.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.context.ApplicationEvent;
import tn.demo.project.domain.ApproverSnapshot;
import tn.demo.project.domain.ProjectId;

import java.util.Set;

@JsonIgnoreProperties({"source", "timestamp"})
public class ProjectCompletedEvent extends ApplicationEvent {
    private final ProjectId projectId;
    private final Set<ApproverSnapshot> approvers;

    @JsonCreator
    public ProjectCompletedEvent(@JsonProperty("projectId") ProjectId projectId,
                                 @JsonProperty("approvers") Set<ApproverSnapshot> approvers) {
        super(projectId);
        this.projectId = projectId;
        this.approvers = Set.copyOf(approvers);
    }

    public ProjectId getProjectId() {
        return projectId;
    }

    public Set<ApproverSnapshot> getApprovers() {
        return approvers;
    }
}
