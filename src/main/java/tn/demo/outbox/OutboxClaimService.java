package tn.demo.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tn.demo.common.DateService;
import tn.demo.common.IDService;

import java.time.Duration;
import java.util.List;

@Service
public class OutboxClaimService {

    private static final int BATCH_SIZE = 10;
    private static final Duration CLAIM_DURATION = Duration.ofSeconds(30);

    private final OutboxClaimRepository outbox;
    private final IDService ids;
    private final DateService dates;

    public OutboxClaimService(
            OutboxClaimRepository outbox,
            IDService ids,
            DateService dates
    ) {
        this.outbox = outbox;
        this.ids = ids;
        this.dates = dates;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ClaimedOutboxItem> claimBatch(String listenerId) {
        var now = dates.now();

        return outbox.claimBatch(
                listenerId,
                BATCH_SIZE,
                now,
                now.plus(CLAIM_DURATION),
                ids.newId()
        );
    }
}