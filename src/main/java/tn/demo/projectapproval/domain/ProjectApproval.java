package tn.demo.projectapproval.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;
import tn.demo.project.domain.ApproverSnapshot;
import tn.demo.project.domain.ProjectId;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Table("project_approvals")
public class ProjectApproval implements Persistable<UUID> {

    @Id
    private final UUID projectId;
    private final LocalDateTime startedAt;

    @Version
    private final int version;

    @MappedCollection(idColumn = "project_id", keyColumn = "id")
    private final Set<ProjectApprovalApprover> approvers;

    @Transient
    private final boolean newApproval;

    public static ProjectApproval start(
            ProjectId projectId,
            Set<ApproverSnapshot> approverSnapshots,
            LocalDateTime startedAt
    ) {
        Set<ProjectApprovalApprover> approvers = approverSnapshots.stream()
                .map(ProjectApprovalApprover::pending)
                .collect(Collectors.toSet());

        return new ProjectApproval(
                projectId.value(),
                startedAt,
                0,
                approvers,
                true
        );
    }

    @PersistenceCreator
    private ProjectApproval(
            UUID projectId,
            LocalDateTime startedAt,
            int version,
            Set<ProjectApprovalApprover> approvers
    ) {
        this(projectId, startedAt, version, approvers, false);
    }

    private ProjectApproval(
            UUID projectId,
            LocalDateTime startedAt,
            int version,
            Set<ProjectApprovalApprover> approvers,
            boolean newApproval
    ) {
        this.projectId = projectId;
        this.startedAt = startedAt;
        this.version = version;
        this.approvers = approvers;
        this.newApproval = newApproval;
    }

    @Override
    public UUID getId() {
        return projectId;
    }

    @Override
    public boolean isNew() {
        return newApproval;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        ProjectApproval that = (ProjectApproval) other;
        return Objects.equals(projectId, that.projectId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(projectId);
    }
}