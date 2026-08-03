-- 1. Backfill product_id in order_items from product_variants where missing
UPDATE order_items oi
SET product_id = pv.product_id
FROM product_variants pv
WHERE oi.variant_id = pv.id
  AND oi.product_id IS NULL;

-- 2. Fix any product_reviews that were incorrectly assigned to a variant_id instead of SPU product_id
UPDATE product_reviews pr
SET product_id = pv.product_id
FROM product_variants pv
WHERE pr.product_id = pv.id
  AND pr.product_id <> pv.product_id;
