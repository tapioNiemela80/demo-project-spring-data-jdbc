package tn.demo.common.domain;

import java.util.Objects;
import java.util.UUID;

@ValueObject(description ="Represents id of approver")
public record ApproverId(UUID value) {
    public ApproverId {
        Objects.requireNonNull(value, "ApproverId value cannot be null");
    }
}
