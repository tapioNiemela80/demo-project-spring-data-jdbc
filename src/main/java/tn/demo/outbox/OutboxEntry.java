package tn.demo.outbox;

import org.springframework.context.ApplicationEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Table("outbox")
public final class OutboxEntry implements Persistable<UUID> {

    @Id
    private final UUID id;

    private final String eventType;
    private final int eventVersion;
    private final String payload;
    private final LocalDateTime createdAt;

    private final String aggregateType;
    private final UUID aggregateId;
    private final int aggregateVersion;

    private final String listenerId;
    private final int attempts;
    private final LocalDateTime availableAt;

    private final LocalDateTime claimedUntil;
    private final UUID claimToken;

    private final LocalDateTime completedAt;
    private final LocalDateTime failedAt;
    private final String lastError;
    private final String status;

    @Transient
    private boolean newEntry;

    @PersistenceCreator
    private OutboxEntry(
            UUID id,
            String eventType,
            int eventVersion,
            String payload,
            LocalDateTime createdAt,
            String aggregateType,
            UUID aggregateId,
            int aggregateVersion,
            String listenerId,
            int attempts,
            LocalDateTime availableAt,
            LocalDateTime claimedUntil,
            UUID claimToken,
            LocalDateTime completedAt,
            LocalDateTime failedAt,
            String lastError,
            String status
    ) {
        this.id = id;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.payload = payload;
        this.createdAt = createdAt;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.aggregateVersion = aggregateVersion;
        this.listenerId = listenerId;
        this.attempts = attempts;
        this.availableAt = availableAt;
        this.claimedUntil = claimedUntil;
        this.claimToken = claimToken;
        this.completedAt = completedAt;
        this.failedAt = failedAt;
        this.lastError = lastError;
        this.newEntry = false;
        this.status = status;
    }

    public static OutboxEntry pending(
            UUID id,
            Class<? extends ApplicationEvent> eventType,
            int eventVersion,
            String payload,
            LocalDateTime createdAt,
            Class<?> aggregateType,
            UUID aggregateId,
            int aggregateVersion,
            String listenerId
    ) {
        OutboxEntry entry = new OutboxEntry(
                id,
                eventType.getName(),
                eventVersion,
                payload,
                createdAt,
                aggregateType.getName(),
                aggregateId,
                aggregateVersion,
                listenerId,
                0,
                createdAt,
                null,
                null,
                null,
                null,
                null,
                "PENDING"
        );

        entry.newEntry = true;
        return entry;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newEntry;
    }

    public String getEventType() {
        return eventType;
    }

    public int getEventVersion() {
        return eventVersion;
    }

    public String getPayload() {
        return payload;
    }

    public String getListenerId() {
        return listenerId;
    }

    public UUID getClaimToken() {
        return claimToken;
    }

    public int getAttempts() {
        return attempts;
    }

    public LocalDateTime getClaimedUntil() {
        return claimedUntil;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        OutboxEntry that = (OutboxEntry) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}