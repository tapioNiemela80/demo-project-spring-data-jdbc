package tn.demo.team.domain;

import tn.demo.common.domain.ValueObject;

import java.util.Objects;
import java.util.UUID;

@ValueObject(description ="Represents id of task of team")
public record TeamTaskId(UUID value) {
    public TeamTaskId {
        Objects.requireNonNull(value, "TeamTaskId value cannot be null");
    }
}
