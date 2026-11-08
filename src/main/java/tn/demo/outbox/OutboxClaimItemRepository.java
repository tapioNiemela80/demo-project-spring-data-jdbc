package tn.demo.outbox;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
class OutboxClaimRepository {

    private static final String CLAIM_BATCH_SQL = """
            WITH candidates AS (
                SELECT id
                FROM outbox
                WHERE listener_id = :listenerId
                  AND completed_at IS NULL
                  AND failed_at IS NULL
                  AND available_at <= :now
                  AND (claimed_until IS NULL OR claimed_until <= :now)
                ORDER BY available_at, id
                LIMIT :batchSize
                FOR UPDATE SKIP LOCKED
            )
            UPDATE outbox o
            SET status = 'PROCESSING',
                attempts = o.attempts + 1,
                claimed_until = :claimedUntil,
                claim_token = :claimToken
            FROM candidates
            WHERE o.id = candidates.id
            RETURNING o.id, o.event_type, o.event_version, o.payload,
                      o.listener_id, o.attempts, o.claim_token
            """;

    private static final RowMapper<ClaimedOutboxItem> ROW_MAPPER =
            (resultSet, rowNumber) -> new ClaimedOutboxItem(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getString("event_type"),
                    resultSet.getInt("event_version"),
                    resultSet.getString("payload"),
                    resultSet.getString("listener_id"),
                    resultSet.getInt("attempts"),
                    resultSet.getObject("claim_token", UUID.class)
            );

    private final NamedParameterJdbcTemplate jdbc;

    OutboxClaimRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<ClaimedOutboxItem> claimBatch(
            String listenerId,
            int batchSize,
            LocalDateTime now,
            LocalDateTime claimedUntil,
            UUID claimToken
    ) {
        var parameters = new MapSqlParameterSource()
                .addValue("listenerId", listenerId)
                .addValue("batchSize", batchSize)
                .addValue("now", now)
                .addValue("claimedUntil", claimedUntil)
                .addValue("claimToken", claimToken);

        return jdbc.query(CLAIM_BATCH_SQL, parameters, ROW_MAPPER);
    }

    int markCompleted(ClaimedOutboxItem item, LocalDateTime completedAt) {
        return jdbc.update("""
                        UPDATE outbox
                        SET status = 'COMPLETED',
                            completed_at = :completedAt,
                            claimed_until = NULL,
                            claim_token = NULL
                        WHERE id = :id
                          AND claim_token = :claimToken
                          AND completed_at IS NULL
                          AND failed_at IS NULL
                        """,
                new MapSqlParameterSource()
                        .addValue("completedAt", completedAt)
                        .addValue("id", item.id())
                        .addValue("claimToken", item.claimToken()));
    }

    int rescheduleOrFail(
            ClaimedOutboxItem item,
            LocalDateTime availableAt,
            LocalDateTime failedAt,
            String status,
            String lastError
    ) {
        return jdbc.update("""
                        UPDATE outbox
                        SET status = :status,
                            available_at = :availableAt,
                            claimed_until = NULL,
                            claim_token = NULL,
                            failed_at = :failedAt,
                            last_error = :lastError
                        WHERE id = :id
                          AND claim_token = :claimToken
                          AND completed_at IS NULL
                          AND failed_at IS NULL
                        """,
                new MapSqlParameterSource()
                        .addValue("status", status)
                        .addValue("availableAt", availableAt)
                        .addValue("failedAt", failedAt)
                        .addValue("lastError", lastError)
                        .addValue("id", item.id())
                        .addValue("claimToken", item.claimToken()));
    }
}