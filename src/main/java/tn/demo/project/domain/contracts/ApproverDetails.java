package tn.demo.project.domain.contracts;

import tn.demo.common.domain.ApproverId;
import tn.demo.common.domain.EmailAddress;

import java.util.Objects;

public record ApproverDetails(ApproverId id, String name, EmailAddress email) {
    public ApproverDetails {
        Objects.requireNonNull(name);
        Objects.requireNonNull(email);

        if (!email.isValid()) {
            throw new IllegalArgumentException("Invalid approver email");
        }
    }
}
