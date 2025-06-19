package tn.demo.project.domain;

import tn.demo.common.domain.ValueObject;

import java.util.Objects;
import java.util.UUID;

@ValueObject(description ="Represents id of project")
public record ProjectId(UUID value) {
    public ProjectId {
        Objects.requireNonNull(value, "ProjectId value cannot be null");
    }
}
