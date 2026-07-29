-- V8: stock summary materialized view + full-text search trigger on products.
-- Summary is at the variant × warehouse grain (joins variant + product + category).
CREATE MATERIALIZED VIEW mv_stock_summary AS
SELECT
    v.id            AS variant_id,
    v.sku,
    v.variant_name,
    p.id            AS product_id,
    p.name          AS product_name,
    c.name          AS category_name,
    w.id            AS warehouse_id,
    w.name          AS warehouse_name,
    sl.quantity,
    sl.reserved_quantity,
    sl.quantity - sl.reserved_quantity AS available_quantity,
    v.min_stock,
    v.reorder_point,
    CASE
        WHEN sl.quantity = 0             THEN 'OUT_OF_STOCK'
        WHEN sl.quantity <= v.min_stock  THEN 'LOW_STOCK'
        WHEN sl.quantity >= v.max_stock  THEN 'OVERSTOCK'
        ELSE 'NORMAL'
    END AS stock_status
FROM stock_levels sl
JOIN product_variants v ON v.id = sl.variant_id
JOIN products p         ON p.id = v.product_id
JOIN warehouses w       ON w.id = sl.warehouse_id
LEFT JOIN categories c  ON c.id = p.category_id
WHERE p.active = TRUE AND v.active = TRUE;

CREATE UNIQUE INDEX idx_mv_stock_summary ON mv_stock_summary(variant_id, warehouse_id);

-- Full-text search: keep products.search_vector in sync with name + description.
CREATE OR REPLACE FUNCTION products_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('simple', coalesce(NEW.name, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(NEW.description, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_products_search_vector
    BEFORE INSERT OR UPDATE OF name, description ON products
    FOR EACH ROW EXECUTE FUNCTION products_search_vector_update();

-- Backfill for rows seeded in V2 (trigger only fires on future writes).
UPDATE products SET name = name;
