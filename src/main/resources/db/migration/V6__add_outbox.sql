CREATE TABLE project_demo.outbox (
                        id                UUID PRIMARY KEY,
                        event_type        TEXT NOT NULL,
                        status            TEXT NOT NULL,
                        event_version     INTEGER NOT NULL DEFAULT 1,
                        payload           TEXT NOT NULL,
                        created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                        aggregate_type    TEXT NOT NULL,
                        aggregate_id      UUID NOT NULL,
                        aggregate_version INTEGER NOT NULL,
                        listener_id       TEXT NOT NULL,
                        attempts          INTEGER NOT NULL DEFAULT 0,
                        available_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                        claimed_until     TIMESTAMP WITHOUT TIME ZONE,
                        claim_token       UUID,
                        completed_at      TIMESTAMP WITHOUT TIME ZONE,
                        failed_at         TIMESTAMP WITHOUT TIME ZONE,
                        last_error        TEXT,

                        CONSTRAINT chk_outbox_event_version
                            CHECK (event_version >= 1),

                        CONSTRAINT chk_outbox_attempts
                            CHECK (attempts >= 0),

                        CONSTRAINT chk_outbox_claim
                            CHECK (
                                (claimed_until IS NULL AND claim_token IS NULL)
                                    OR
                                (claimed_until IS NOT NULL AND claim_token IS NOT NULL)
                                ),

                        CONSTRAINT chk_outbox_terminal_state
                            CHECK (
                                NOT (
                                    completed_at IS NOT NULL
                                        AND failed_at IS NOT NULL
                                    )
                                ),

                        CONSTRAINT chk_outbox_terminal_without_claim
                            CHECK (
                                (completed_at IS NULL AND failed_at IS NULL)
                                    OR
                                (claimed_until IS NULL AND claim_token IS NULL)
                                )
);

CREATE INDEX idx_outbox_pending
    ON project_demo.outbox (available_at, id)
    WHERE completed_at IS NULL
      AND failed_at IS NULL;

CREATE INDEX idx_outbox_aggregate
    ON project_demo.outbox (aggregate_type, aggregate_id, aggregate_version);

CREATE INDEX idx_outbox_completed
    ON project_demo.outbox (completed_at)
    WHERE completed_at IS NOT NULL;