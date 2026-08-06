ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS paygate_transaction_ref VARCHAR(100);

CREATE TABLE IF NOT EXISTS return_requests (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(30) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_return_requests_order_open
    ON return_requests(order_id)
    WHERE status IN ('REQUESTED', 'APPROVED');

CREATE TABLE IF NOT EXISTS refund_requests (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    idempotency_key VARCHAR(160) NOT NULL UNIQUE,
    transaction_ref VARCHAR(100),
    amount NUMERIC(12, 2) NOT NULL,
    reason TEXT,
    status VARCHAR(30) NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_refund_requests_order
    ON refund_requests(order_id);
