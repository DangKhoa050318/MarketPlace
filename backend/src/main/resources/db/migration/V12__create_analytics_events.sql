CREATE TABLE analytics_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    schema_version VARCHAR(16) NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    user_id BIGINT,
    session_id VARCHAR(128),
    source VARCHAR(80),
    device_type VARCHAR(40),
    campaign VARCHAR(120),
    product_id BIGINT,
    variant_id BIGINT,
    quantity INTEGER,
    path VARCHAR(500),
    search_query VARCHAR(300),
    properties JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMP NOT NULL,
    received_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_analytics_events_event_id UNIQUE (event_id),
    CONSTRAINT fk_analytics_events_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_analytics_events_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL,
    CONSTRAINT fk_analytics_events_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE SET NULL,
    CONSTRAINT chk_analytics_events_quantity CHECK (quantity IS NULL OR quantity > 0)
);

CREATE INDEX idx_analytics_events_type_occurred_at
    ON analytics_events (event_type, occurred_at DESC);

CREATE INDEX idx_analytics_events_user_occurred_at
    ON analytics_events (user_id, occurred_at DESC)
    WHERE user_id IS NOT NULL;

CREATE INDEX idx_analytics_events_session_occurred_at
    ON analytics_events (session_id, occurred_at DESC)
    WHERE session_id IS NOT NULL;

CREATE INDEX idx_analytics_events_product_occurred_at
    ON analytics_events (product_id, occurred_at DESC)
    WHERE product_id IS NOT NULL;
