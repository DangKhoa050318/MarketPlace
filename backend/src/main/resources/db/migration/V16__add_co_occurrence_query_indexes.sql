-- Supports distinct-session PRODUCT_VIEW co-occurrence queries.
CREATE INDEX idx_analytics_product_view_product_time
    ON analytics_events (product_id, occurred_at)
    WHERE event_type = 'PRODUCT_VIEW';

CREATE INDEX idx_analytics_product_view_session_time
    ON analytics_events (session_id, occurred_at)
    WHERE event_type = 'PRODUCT_VIEW' AND session_id IS NOT NULL;

CREATE INDEX idx_analytics_product_view_user_time
    ON analytics_events (user_id, occurred_at)
    WHERE event_type = 'PRODUCT_VIEW' AND user_id IS NOT NULL;
