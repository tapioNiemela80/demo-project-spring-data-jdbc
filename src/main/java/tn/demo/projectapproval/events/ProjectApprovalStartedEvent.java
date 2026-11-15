package tn.demo.projectapproval.events;

import tn.demo.project.domain.ApproverSnapshot;
import tn.demo.project.domain.ProjectId;
import java.util.Set;

public record ProjectApprovalStartedEvent(ProjectId projectId, Set<ApproverSnapshot> approvers) {
    public ProjectApprovalStartedEvent { approvers = Set.copyOf(approvers); }
}
