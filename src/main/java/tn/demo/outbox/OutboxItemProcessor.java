package tn.demo.outbox;

public interface OutboxItemProcessor {

    String listenerId();

    void process(ClaimedOutboxItem item);
}