CREATE TABLE anonymous_wishlist_items (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(128) NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_anonymous_wishlist_product
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uk_anonymous_wishlist_session_product UNIQUE (session_id, product_id)
);

CREATE INDEX idx_anonymous_wishlist_session_created_at
    ON anonymous_wishlist_items (session_id, created_at DESC);

CREATE TABLE analytics_daily_aggregates (
    id BIGSERIAL PRIMARY KEY,
    aggregate_date DATE NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    product_id BIGINT,
    category_id BIGINT,
    campaign VARCHAR(120),
    device_type VARCHAR(80),
    event_count BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_analytics_daily_aggregates_dimensions
    ON analytics_daily_aggregates (
        aggregate_date,
        event_type,
        (COALESCE(product_id, -1)),
        (COALESCE(category_id, -1)),
        (COALESCE(campaign, '')),
        (COALESCE(device_type, ''))
    );

CREATE INDEX idx_analytics_daily_aggregates_lookup
    ON analytics_daily_aggregates (aggregate_date, event_type, category_id, product_id);
