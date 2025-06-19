package tn.demo.project.domain;

import java.util.UUID;

public class ProjectAlreadyCompletedException extends RuntimeException {

    private final UUID projectId;
    public ProjectAlreadyCompletedException(UUID projectId) {
        super("Project %s already completed".formatted(projectId));
        this.projectId = projectId;
    }
}
