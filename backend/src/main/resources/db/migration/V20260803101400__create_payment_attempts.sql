CREATE TABLE payment_attempts (
    id                      BIGSERIAL PRIMARY KEY,
    event_id                VARCHAR(64) NOT NULL UNIQUE,
    order_id                BIGINT NOT NULL REFERENCES orders(id),
    amount                  NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    status                  VARCHAR(32) NOT NULL,
    provider_transaction_id VARCHAR(255),
    failure_reason          VARCHAR(500),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payment_attempt_status CHECK (
        status IN ('PROCESSING', 'SUCCEEDED', 'DECLINED', 'REFUND_REQUIRED', 'REFUNDED', 'SKIPPED')
    )
);

CREATE INDEX idx_payment_attempts_order_id ON payment_attempts(order_id);
