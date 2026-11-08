package tn.demo.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxItemProcessingService {

    private final OutboxCompletionService completions;

    public OutboxItemProcessingService(OutboxCompletionService completions) {
        this.completions = completions;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processAndComplete(
            ClaimedOutboxItem item,
            OutboxItemProcessor processor
    ) {
        processor.process(item);

        if (!completions.complete(item)) {
            throw new IllegalStateException(
                    "Could not complete outbox item because claim is no longer owned: "
                            + item.id()
            );
        }
    }
}