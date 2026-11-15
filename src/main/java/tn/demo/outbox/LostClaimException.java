package tn.demo.outbox;

public class LostClaimException extends RuntimeException {

    public LostClaimException(String message) {
        super(message);
    }
}
