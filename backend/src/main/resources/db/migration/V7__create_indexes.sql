-- V7: indexes (merged from both projects, retargeted to the variant model).
-- Catalog
CREATE INDEX idx_products_category   ON products(category_id);
CREATE INDEX idx_products_active     ON products(active) WHERE active = TRUE;
CREATE INDEX idx_products_search     ON products USING GIN(search_vector);
CREATE INDEX idx_variants_product    ON product_variants(product_id);
CREATE INDEX idx_variants_sku        ON product_variants(sku);
-- Stock
CREATE INDEX idx_stock_variant       ON stock_levels(variant_id);
CREATE INDEX idx_stock_warehouse     ON stock_levels(warehouse_id);
CREATE INDEX idx_stock_low           ON stock_levels(quantity) WHERE quantity < 20;
-- Movements
CREATE INDEX idx_movements_warehouse    ON stock_movements(warehouse_id);
CREATE INDEX idx_movements_type_status  ON stock_movements(type, status);
CREATE INDEX idx_movements_created      ON stock_movements(created_at);
CREATE INDEX idx_movement_items_variant ON stock_movement_items(variant_id);
-- Orders
CREATE INDEX idx_orders_user         ON orders(user_id);
CREATE INDEX idx_orders_status       ON orders(status);
CREATE INDEX idx_orders_user_status  ON orders(user_id, status);
CREATE INDEX idx_order_items_order   ON order_items(order_id);
-- Alerts
CREATE INDEX idx_alerts_active       ON stock_alerts(status) WHERE status = 'ACTIVE';
-- Reorder: partial unique index backing ReorderSuggestionRepository.insertPendingIfAbsent (ON CONFLICT)
CREATE UNIQUE INDEX uq_reorder_pending ON reorder_suggestions(variant_id, warehouse_id) WHERE status = 'PENDING';
