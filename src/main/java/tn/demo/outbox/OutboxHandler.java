package tn.demo.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OutboxHandler {

    private static final Logger log = LoggerFactory.getLogger(OutboxHandler.class);

    private final OutboxClaimService claims;
    private final OutboxItemProcessingService itemProcessing;
    private final OutboxFailureHandler failures;
    private final Map<String, OutboxItemProcessor> processorsByListenerId;

    public OutboxHandler(
            OutboxClaimService claims,
            OutboxItemProcessingService itemProcessing,
            OutboxFailureHandler failures,
            List<OutboxItemProcessor> processors
    ) {
        this.claims = claims;
        this.itemProcessing = itemProcessing;
        this.failures = failures;
        this.processorsByListenerId = processors.stream()
                .collect(Collectors.toUnmodifiableMap(
                        OutboxItemProcessor::listenerId,
                        Function.identity()
                ));
    }

    public void handle(String listenerId) {
        OutboxItemProcessor processor = processorsByListenerId.get(listenerId);
        if (processor == null) {
            throw new IllegalArgumentException(
                    "No outbox processor for listenerId " + listenerId);
        }
        var items = claims.claimBatch(listenerId);
        log.info("Processing " + items.size() + " items for listener " + listenerId);
        for (ClaimedOutboxItem item : items) {
            log.atDebug().log(() -> "item %s".formatted(item));
            process(item, processor);
        }
    }

    private void process(ClaimedOutboxItem item, OutboxItemProcessor processor) {
        try {
            //processes and completes item in new transaction
            itemProcessing.processAndComplete(item, processor);
        } catch (Exception failure) {
            log.error(
                    "Processing outbox item {} for listener {} failed",
                    item.id(),
                    item.listenerId(),
                    failure
            );

            try {
                if (!failures.handle(item, failure)) {
                    log.warn(
                            "Could not update failed outbox item {}; claim is no longer owned",
                            item.id());
                }
            } catch (Exception failureUpdateError) {
                log.error(
                        "Could not persist failure for outbox item {}; it can be reclaimed after lease expiry",
                        item.id(),
                        failureUpdateError);
            }
        }
    }
}