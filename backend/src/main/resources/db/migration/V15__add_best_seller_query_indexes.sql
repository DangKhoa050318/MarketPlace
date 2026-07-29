-- Supports best-seller aggregation by valid order status and configurable time window.
CREATE INDEX idx_orders_status_created
    ON orders (status, created_at);

CREATE INDEX idx_order_items_variant_order
    ON order_items (variant_id, order_id);
