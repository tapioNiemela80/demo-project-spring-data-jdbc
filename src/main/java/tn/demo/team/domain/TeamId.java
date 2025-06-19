package tn.demo.team.domain;

import tn.demo.common.domain.ValueObject;

import java.util.Objects;
import java.util.UUID;

@ValueObject(description ="Represents id of team")
public record TeamId(UUID value) {
    public TeamId {
        Objects.requireNonNull(value, "TeamId value cannot be null");
    }
}
