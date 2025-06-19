package tn.demo.project.domain;

import java.util.UUID;

public class UnknownProjectIdException extends RuntimeException{
    private final UUID givenId;

    public UnknownProjectIdException(UUID givenId) {
        super("Unknown project id %s".formatted(givenId));
        this.givenId = givenId;
    }
}
