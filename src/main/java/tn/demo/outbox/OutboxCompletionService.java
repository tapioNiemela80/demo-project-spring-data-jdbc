package tn.demo.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.demo.common.DateService;

@Service
public class OutboxCompletionService {

    private final OutboxClaimRepository outbox;
    private final DateService dates;

    public OutboxCompletionService(
            OutboxClaimRepository outbox,
            DateService dates
    ) {
        this.outbox = outbox;
        this.dates = dates;
    }

    @Transactional
    public boolean complete(ClaimedOutboxItem item) {
        return outbox.markCompleted(item, dates.now()) == 1;
    }
}