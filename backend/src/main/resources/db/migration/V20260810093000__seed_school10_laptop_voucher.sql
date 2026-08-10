-- Demo voucher surfaced by the shopping chat assistant.
-- SCHOOL10 applies to every product in the Computers & Laptops category.
INSERT INTO promotion_codes (
    code,
    discount_type,
    discount_value,
    max_discount,
    min_order_amount,
    scope_type,
    usage_limit,
    per_user_limit,
    used_count,
    starts_at,
    expires_at,
    active,
    created_at,
    updated_at
)
VALUES (
    'SCHOOL10',
    'PERCENT',
    10.00,
    2000000.00,
    0.00,
    'CATEGORY',
    NULL,
    NULL,
    0,
    TIMESTAMP '2026-01-01 00:00:00',
    TIMESTAMP '2035-12-31 23:59:59',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (code) DO UPDATE SET
    discount_type = EXCLUDED.discount_type,
    discount_value = EXCLUDED.discount_value,
    max_discount = EXCLUDED.max_discount,
    min_order_amount = EXCLUDED.min_order_amount,
    scope_type = EXCLUDED.scope_type,
    usage_limit = EXCLUDED.usage_limit,
    per_user_limit = EXCLUDED.per_user_limit,
    starts_at = EXCLUDED.starts_at,
    expires_at = EXCLUDED.expires_at,
    active = EXCLUDED.active,
    updated_at = NOW();

DELETE FROM promotion_code_scopes
WHERE promotion_code_id = (
    SELECT id FROM promotion_codes WHERE code = 'SCHOOL10'
);

INSERT INTO promotion_code_scopes (promotion_code_id, ref_type, ref_id)
SELECT promotion.id, 'CATEGORY', category.id
FROM promotion_codes promotion
JOIN categories category ON category.code = 'COMP'
WHERE promotion.code = 'SCHOOL10';

UPDATE campaigns
SET description = 'Long-running 10% voucher campaign for computers and laptops.',
    status = 'PUBLISHED',
    starts_at = TIMESTAMP '2026-01-01 00:00:00',
    ends_at = TIMESTAMP '2035-12-31 23:59:59',
    promotion_code_id = (SELECT id FROM promotion_codes WHERE code = 'SCHOOL10'),
    active = TRUE,
    updated_at = NOW()
WHERE name = 'SCHOOL10 Laptop Campaign';

INSERT INTO campaigns (
    name,
    description,
    status,
    starts_at,
    ends_at,
    promotion_code_id,
    active,
    created_at,
    updated_at
)
SELECT
    'SCHOOL10 Laptop Campaign',
    'Long-running 10% voucher campaign for computers and laptops.',
    'PUBLISHED',
    TIMESTAMP '2026-01-01 00:00:00',
    TIMESTAMP '2035-12-31 23:59:59',
    promotion.id,
    TRUE,
    NOW(),
    NOW()
FROM promotion_codes promotion
WHERE promotion.code = 'SCHOOL10'
  AND NOT EXISTS (
      SELECT 1 FROM campaigns WHERE name = 'SCHOOL10 Laptop Campaign'
  );
