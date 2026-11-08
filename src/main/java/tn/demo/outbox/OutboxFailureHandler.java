package tn.demo.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tn.demo.common.DateService;

import java.time.Duration;

@Service
public class OutboxFailureHandler {

    private final OutboxClaimRepository outbox;
    private final DateService dates;
    private final int maxAttempts;
    private final Duration retryDelay;

    public OutboxFailureHandler(
            OutboxClaimRepository outbox,
            DateService dates,
            @Value("${outbox.processing.max-attempts:5}") int maxAttempts,
            @Value("${outbox.processing.retry-delay-seconds:10}")
            long retryDelaySeconds
    ) {
        this.outbox = outbox;
        this.dates = dates;
        this.maxAttempts = maxAttempts;
        this.retryDelay = Duration.ofSeconds(retryDelaySeconds);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean handle(ClaimedOutboxItem item, Exception failure) {
        var now = dates.now();
        boolean exhausted = item.attempts() >= maxAttempts;

        String error = failure.getMessage() == null
                ? failure.getClass().getName()
                : failure.getMessage();

        return outbox.rescheduleOrFail(
                item,
                exhausted ? now : now.plus(retryDelay),
                exhausted ? now : null,
                exhausted ? "FAILED" : "PENDING",
                error
        ) == 1;
    }
}