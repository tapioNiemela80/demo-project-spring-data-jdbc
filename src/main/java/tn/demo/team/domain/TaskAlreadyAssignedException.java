package tn.demo.team.domain;

public class TaskAlreadyAssignedException extends RuntimeException{
    public TaskAlreadyAssignedException(String message) {
        super(message);
    }
}
