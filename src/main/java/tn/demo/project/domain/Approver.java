package tn.demo.project.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Table;
import tn.demo.common.domain.ApproverId;
import tn.demo.common.domain.EmailAddress;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Table("project_approvers")
class Approver {
    @Id
    private final UUID id;
    private final String name;
    private final String email;

    @PersistenceCreator
    private Approver(UUID id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    static Approver createNew(ApproverId approverId, String name, EmailAddress email){
        return new Approver(approverId.value(), name, email.value());
    }

    Optional<EmailAddress> validEmail() {
        EmailAddress address = EmailAddress.rehydrate(email);
        return address.isValid()
                ? Optional.of(address)
                : Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Approver other = (Approver) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    ApproverSnapshot toSnapshot() {
        return new ApproverSnapshot(new ApproverId(id), name, email);
    }
}
