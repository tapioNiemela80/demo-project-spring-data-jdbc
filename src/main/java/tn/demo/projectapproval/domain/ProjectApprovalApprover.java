package tn.demo.projectapproval.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Table;
import tn.demo.project.domain.ApproverSnapshot;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Table("project_approval_approvers")
class ProjectApprovalApprover {

    @Id
    private final UUID id;
    private final String name;
    private final String email;
    private final ApproverApprovalStatus status;
    private final LocalDateTime decidedAt;

    @PersistenceCreator
    private ProjectApprovalApprover(
            UUID id,
            String name,
            String email,
            ApproverApprovalStatus status,
            LocalDateTime decidedAt
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.status = status;
        this.decidedAt = decidedAt;
    }

    static ProjectApprovalApprover pending(ApproverSnapshot snapshot) {
        return new ProjectApprovalApprover(
                snapshot.approverId().value(),
                snapshot.name(),
                snapshot.emailAddress(),
                ApproverApprovalStatus.PENDING,
                null
        );
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ProjectApprovalApprover that = (ProjectApprovalApprover) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}