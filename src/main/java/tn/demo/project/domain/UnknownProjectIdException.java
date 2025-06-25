package tn.demo.project.domain;

import java.util.UUID;

public class UnknownProjectIdException extends RuntimeException{
    private final ProjectId projectId;

    public UnknownProjectIdException(ProjectId projectId) {
        super("Unknown project id %s".formatted(projectId));
        this.projectId = projectId;
    }

    public ProjectId getProjectId() {
        return projectId;
    }
}
