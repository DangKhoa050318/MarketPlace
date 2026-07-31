-- Backfill stock levels at WH-CENTRAL (warehouse 1) for all variants that lack one.
-- The default warehouseId for order fulfilment is the smallest active warehouse id,
-- which is WH-CENTRAL (id = 1).  The original demo seed (V20260729100400) only
-- created stock_level rows at warehouse 3+ for demo variants 9..28, so any
-- checkout that hits one of those variants fails with
-- "No stock record for variant X at warehouse 1".
-- This migration ensures every variant has a record at WH-CENTRAL.

INSERT INTO stock_levels (variant_id, warehouse_id, quantity, reserved_quantity, version)
SELECT
    v.id,
    1,                                         -- WH-CENTRAL
    100,                                       -- generous on-hand
    0,
    0
FROM product_variants v
WHERE NOT EXISTS (
    SELECT 1 FROM stock_levels sl
    WHERE sl.variant_id = v.id
      AND sl.warehouse_id = 1
)
ON CONFLICT (variant_id, warehouse_id) DO NOTHING;
