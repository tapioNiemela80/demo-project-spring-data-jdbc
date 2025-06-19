package tn.demo.team.domain;

public class TaskTransitionNotAllowedException extends RuntimeException {
    public TaskTransitionNotAllowedException(String message) {
        super(message);
    }
}
