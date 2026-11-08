package tn.demo.project.events;

import org.springframework.context.ApplicationEvent;
import tn.demo.project.domain.ApproverSnapshot;
import tn.demo.project.domain.ProjectId;

import java.util.Set;

public class ProjectCreatedEvent extends ApplicationEvent {
    private final ProjectId projectId;
    private final Set<ApproverSnapshot> approvers;

    public ProjectCreatedEvent(ProjectId projectId, Set<ApproverSnapshot> approvers){
        super(projectId);
        this.projectId = projectId;
        this.approvers = approvers;
    }

    public ProjectId getProjectId() {
        return projectId;
    }

    public Set<ApproverSnapshot> getApprovers() {
        return approvers;
    }
}
