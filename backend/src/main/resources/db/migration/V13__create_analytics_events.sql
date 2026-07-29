-- REQ-STP-B-502: durable ingestion store for canonical analytics event schema v1.
CREATE TABLE analytics_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    schema_version INT NOT NULL,
    event_type VARCHAR(50) NOT NULL,

    user_id BIGINT,
    session_id VARCHAR(128),
    product_id BIGINT,
    variant_id BIGINT,

    source VARCHAR(50),
    placement VARCHAR(80),
    recommendation_request_id UUID,
    strategy VARCHAR(50),
    position INT,

    quantity INT,
    order_id BIGINT,
    unit_price DECIMAL(12, 2),

    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    properties JSONB NOT NULL DEFAULT '{}'::jsonb,

    CONSTRAINT fk_analytics_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_analytics_product
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL,
    CONSTRAINT fk_analytics_variant
        FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE SET NULL
);

CREATE INDEX idx_analytics_type_product_time
    ON analytics_events(event_type, product_id, occurred_at DESC);

CREATE INDEX idx_analytics_session_time
    ON analytics_events(session_id, occurred_at DESC);

CREATE INDEX idx_analytics_recommendation_request
    ON analytics_events(recommendation_request_id, event_type);
