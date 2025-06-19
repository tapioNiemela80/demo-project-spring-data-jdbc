package tn.demo.team.domain;

import tn.demo.common.domain.ValueObject;

import java.util.Objects;
import java.util.UUID;

@ValueObject(description ="Represents id of team member")
public record TeamMemberId(UUID value) {
    public TeamMemberId {
        Objects.requireNonNull(value, "TeamMemberId value cannot be null");
    }
}
