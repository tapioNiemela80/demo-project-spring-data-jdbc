package tn.demo.outbox;

import java.util.UUID;

public record ClaimedOutboxItem(
        UUID id,
        String eventType,
        int eventVersion,
        String payload,
        String listenerId,
        int attempts,
        UUID claimToken
) {
}