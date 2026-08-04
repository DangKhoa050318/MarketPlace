CREATE TABLE deliveries (
    id                 BIGSERIAL PRIMARY KEY,
    order_id           BIGINT NOT NULL,
    carrier            VARCHAR(100) NOT NULL,
    tracking_code      VARCHAR(100) NOT NULL,
    status             VARCHAR(30) NOT NULL,
    estimated_delivery DATE NOT NULL,
    delivered_at       TIMESTAMP,
    created_by         BIGINT NOT NULL,
    version            BIGINT NOT NULL DEFAULT 0,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_deliveries_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_deliveries_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT uq_deliveries_order UNIQUE (order_id),
    CONSTRAINT ck_deliveries_status
        CHECK (status IN ('PENDING', 'IN_TRANSIT', 'DELIVERED', 'FAILED'))
);

CREATE UNIQUE INDEX uq_deliveries_carrier_tracking
    ON deliveries (LOWER(carrier), UPPER(tracking_code));

CREATE TABLE delivery_events (
    id          BIGSERIAL PRIMARY KEY,
    delivery_id BIGINT NOT NULL,
    event_type  VARCHAR(40) NOT NULL,
    status      VARCHAR(30) NOT NULL,
    note        TEXT,
    recorded_by BIGINT NOT NULL,
    request_id  UUID NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_events_delivery
        FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE,
    CONSTRAINT fk_delivery_events_recorded_by FOREIGN KEY (recorded_by) REFERENCES users(id),
    CONSTRAINT uq_delivery_events_request UNIQUE (delivery_id, request_id),
    CONSTRAINT ck_delivery_events_status
        CHECK (status IN ('PENDING', 'IN_TRANSIT', 'DELIVERED', 'FAILED')),
    CONSTRAINT ck_delivery_events_type
        CHECK (event_type IN (
            'DELIVERY_CREATED', 'READY_FOR_PICKUP', 'PICKED_UP', 'IN_TRANSIT',
            'OUT_FOR_DELIVERY', 'DELIVERED', 'DELIVERY_FAILED'
        ))
);

CREATE INDEX idx_delivery_events_timeline
    ON delivery_events (delivery_id, occurred_at, id);
