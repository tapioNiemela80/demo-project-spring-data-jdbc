package tn.demo.project.domain;

import java.util.UUID;

public class ProjectAlreadyCompletedException extends RuntimeException {
    private final ProjectId projectId;
    public ProjectAlreadyCompletedException(ProjectId projectId) {
        super("Project %s already completed".formatted(projectId));
        this.projectId = projectId;
    }

    public ProjectId getProjectId() {
        return projectId;
    }
}
