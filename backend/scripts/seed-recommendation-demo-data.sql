-- Opt-in, deterministic development data for FEATURE-STP-03.
--
-- The scenarios exercise:
--   * SIMILAR metadata and active-SKU eligibility filtering
--   * BEST_SELLER status/lookback/category behavior and tie-breaking
--   * CO_PURCHASED distinct-order aggregation
--   * CO_VIEWED distinct session/user aggregation and lookback exclusion
--   * recommendation impression -> click -> cart -> purchase attribution
--
-- This is deliberately a standalone script outside Flyway's db/migration
-- directory. Run it manually only against a local development database.
-- Stable slugs, notes, SKUs and event UUIDs keep it safe to replay. No
-- production identity is used.

-- ---------------------------------------------------------------------------
-- 1. Similar-product metadata
-- ---------------------------------------------------------------------------

WITH metadata(sku, brand, attributes) AS (VALUES
    ('DEMO-SKU-01', 'Dell',
        '{"productType":"laptop","audience":"professional","display":"OLED","connectivity":"Thunderbolt"}'::jsonb),
    ('DEMO-SKU-02', 'ASUS',
        '{"productType":"laptop","audience":"gaming","display":"OLED","connectivity":"Wi-Fi 6E"}'::jsonb),
    ('DEMO-SKU-03', 'Samsung',
        '{"productType":"smartphone","platform":"Android","camera":"200MP","feature":"stylus"}'::jsonb),
    ('DEMO-SKU-04', 'Google',
        '{"productType":"smartphone","platform":"Android","camera":"AI camera","feature":"clean software"}'::jsonb),
    ('DEMO-SKU-05', 'Apple',
        '{"productType":"tablet","audience":"creative","display":"OLED","connectivity":"Thunderbolt"}'::jsonb),
    ('DEMO-SKU-06', 'Sony',
        '{"productType":"camera","sensor":"full-frame","resolution":"33MP","video":"4K"}'::jsonb),
    ('DEMO-SKU-07', 'Fujifilm',
        '{"productType":"camera","sensor":"APS-C","resolution":"40MP","feature":"stabilization"}'::jsonb),
    ('DEMO-SKU-08', 'Bose',
        '{"productType":"headphones","connectivity":"wireless","noiseCancelling":"true","feature":"spatial audio"}'::jsonb),
    ('DEMO-SKU-09', 'Marshall',
        '{"productType":"speaker","connectivity":"wireless","usage":"home","feature":"wide soundstage"}'::jsonb),
    ('DEMO-SKU-10', 'Apple',
        '{"productType":"smartwatch","audience":"outdoor","gps":"true","feature":"health tracking"}'::jsonb),
    ('DEMO-SKU-11', 'Garmin',
        '{"productType":"smartwatch","audience":"outdoor","gps":"true","feature":"training insights"}'::jsonb),
    ('DEMO-SKU-12', 'Nintendo',
        '{"productType":"game-console","mode":"hybrid","display":"OLED","storage":"64GB"}'::jsonb),
    ('DEMO-SKU-13', 'Logitech',
        '{"productType":"mouse","audience":"professional","connectivity":"wireless","feature":"ergonomic"}'::jsonb),
    ('DEMO-SKU-14', 'Keychron',
        '{"productType":"keyboard","audience":"professional","connectivity":"wireless","feature":"mechanical"}'::jsonb),
    ('DEMO-SKU-15', 'LG',
        '{"productType":"monitor","audience":"professional","resolution":"4K","connectivity":"USB-C"}'::jsonb),
    ('DEMO-SKU-16', 'Dyson',
        '{"productType":"air-purifier","usage":"home","feature":"HEPA","connectivity":"smart"}'::jsonb),
    ('DEMO-SKU-17', 'Philips',
        '{"productType":"smart-light","usage":"home","feature":"color lighting","connectivity":"Zigbee"}'::jsonb),
    ('DEMO-SKU-18', 'Ubiquiti',
        '{"productType":"router","usage":"home","feature":"security","connectivity":"Wi-Fi 6"}'::jsonb),
    ('DEMO-SKU-19', 'Anker',
        '{"productType":"power-bank","audience":"mobile","feature":"fast charging","capacity":"20000mAh"}'::jsonb),
    ('DEMO-SKU-20', 'DJI',
        '{"productType":"drone","audience":"creative","video":"4K","feature":"obstacle sensing"}'::jsonb)
)
UPDATE products p
SET brand = metadata.brand,
    attributes = metadata.attributes,
    updated_at = NOW()
FROM product_variants v, metadata
WHERE v.product_id = p.id
  AND v.sku = metadata.sku;

-- Extra price-range coverage for products that originally had one demo SKU.
INSERT INTO product_variants (
    product_id, sku, variant_name, color, size, price, image_url,
    min_stock, max_stock, reorder_point, reorder_quantity, active
)
SELECT p.id, seed.sku, seed.variant_name, seed.color, seed.size,
       seed.price, NULL, 5, 250, 15, 40, TRUE
FROM (VALUES
    ('dell-xps-14-9440', 'REC-DELL-XPS14-64', 'Graphite / 64GB / 2TB', 'Graphite', '64GB / 2TB', 2299.99::DECIMAL(12, 2)),
    ('samsung-galaxy-s24-ultra', 'REC-S24U-512-TI', 'Titanium / 512GB', 'Titanium', '512GB', 1419.99::DECIMAL(12, 2)),
    ('bose-quietcomfort-ultra', 'REC-QCU-WHT', 'White Smoke', 'White', NULL, 429.99::DECIMAL(12, 2)),
    ('logitech-mx-master-3s', 'REC-MX3S-MAC', 'Pale Grey / Mac', 'Pale Grey', 'Mac', 109.99::DECIMAL(12, 2))
) AS seed(slug, sku, variant_name, color, size, price)
JOIN products p ON p.slug = seed.slug
ON CONFLICT (sku) DO NOTHING;

-- Eligibility edge cases: one hidden product and one visible product without
-- any active SKU. Both deliberately resemble the Dell source product.
INSERT INTO products (
    slug, name, description, category_id, unit, image_url, active, brand, attributes
)
SELECT
    seed.slug,
    seed.name,
    seed.description,
    c.id,
    'PCS',
    NULL,
    seed.active,
    'Dell',
    '{"productType":"laptop","audience":"professional","display":"OLED","connectivity":"Thunderbolt"}'::jsonb
FROM (VALUES
    ('rec-edge-hidden-laptop', 'Recommendation Edge Hidden Laptop',
        'Inactive product used to verify storefront recommendation filtering.', FALSE),
    ('rec-edge-no-active-sku', 'Recommendation Edge No Active SKU',
        'Active product with only an inactive SKU used to verify sellability filtering.', TRUE)
) AS seed(slug, name, description, active)
JOIN categories c ON c.code = 'COMP'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO product_variants (
    product_id, sku, variant_name, color, size, price, image_url,
    min_stock, max_stock, reorder_point, reorder_quantity, active
)
SELECT
    p.id,
    seed.sku,
    'Test configuration',
    'Graphite',
    '32GB / 1TB',
    1999.99,
    NULL,
    5,
    100,
    10,
    20,
    seed.active
FROM (VALUES
    ('rec-edge-hidden-laptop', 'REC-EDGE-HIDDEN', TRUE),
    ('rec-edge-no-active-sku', 'REC-EDGE-INACTIVE-SKU', FALSE)
) AS seed(slug, sku, active)
JOIN products p ON p.slug = seed.slug
ON CONFLICT (sku) DO NOTHING;

INSERT INTO stock_levels (
    variant_id, warehouse_id, quantity, reserved_quantity, version
)
SELECT v.id, w.id, 75, 0, 0
FROM product_variants v
JOIN warehouses w ON w.code = 'WH-CENTRAL'
WHERE v.sku IN (
    'REC-DELL-XPS14-64',
    'REC-S24U-512-TI',
    'REC-QCU-WHT',
    'REC-MX3S-MAC'
)
ON CONFLICT (variant_id, warehouse_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Recent, excluded and expired order cohorts
-- ---------------------------------------------------------------------------

-- 36 recent valid multi-item orders. Dell and Logitech deliberately tie on
-- distinct order count; Logitech wins the units-sold secondary sort.
INSERT INTO orders (
    user_id, warehouse_id, status, total_amount, shipping_address, note,
    created_at, updated_at
)
SELECT
    u.id,
    w.id,
    CASE (i % 4)
        WHEN 0 THEN 'CONFIRMED'
        WHEN 1 THEN 'PROCESSING'
        WHEN 2 THEN 'SHIPPED'
        ELSE 'DELIVERED'
    END,
    0,
    'Recommendation demo address ' || i,
    'REC-DEMO-BUNDLE-' || LPAD(i::text, 2, '0'),
    NOW() - (((i - 1) % 24 + 1) || ' days')::INTERVAL,
    NOW() - (((i - 1) % 24) || ' days')::INTERVAL
FROM generate_series(1, 36) AS g(i)
JOIN users u
  ON u.username = 'demo_customer_' || LPAD((((i - 1) % 20) + 1)::text, 2, '0')
JOIN warehouses w ON w.code = 'WH-CENTRAL'
WHERE NOT EXISTS (
    SELECT 1
    FROM orders existing
    WHERE existing.note = 'REC-DEMO-BUNDLE-' || LPAD(i::text, 2, '0')
);

WITH item_seed(note, sku, quantity) AS (
    SELECT 'REC-DEMO-BUNDLE-' || LPAD(i::text, 2, '0'), 'DEMO-SKU-01', 1
    FROM generate_series(1, 36) AS g(i)
    UNION ALL
    SELECT 'REC-DEMO-BUNDLE-' || LPAD(i::text, 2, '0'), 'DEMO-SKU-13',
           1 + CASE WHEN i % 3 = 0 THEN 1 ELSE 0 END
    FROM generate_series(1, 36) AS g(i)
    UNION ALL
    SELECT 'REC-DEMO-BUNDLE-' || LPAD(i::text, 2, '0'), 'DEMO-SKU-14', 1
    FROM generate_series(1, 28) AS g(i)
    UNION ALL
    SELECT 'REC-DEMO-BUNDLE-' || LPAD(i::text, 2, '0'), 'DEMO-SKU-15', 1
    FROM generate_series(1, 18) AS g(i)
    UNION ALL
    SELECT 'REC-DEMO-BUNDLE-' || LPAD(i::text, 2, '0'), 'DEMO-SKU-19', 2
    FROM generate_series(1, 12) AS g(i)
    UNION ALL
    SELECT 'REC-DEMO-BUNDLE-' || LPAD(i::text, 2, '0'), 'DEMO-SKU-08', 1
    FROM generate_series(1, 8) AS g(i)
)
INSERT INTO order_items (
    order_id, variant_id, product_name, variant_name, sku,
    quantity, unit_price, subtotal
)
SELECT
    o.id,
    v.id,
    p.name,
    v.variant_name,
    v.sku,
    item_seed.quantity,
    v.price,
    v.price * item_seed.quantity
FROM item_seed
JOIN orders o ON o.note = item_seed.note
JOIN product_variants v ON v.sku = item_seed.sku
JOIN products p ON p.id = v.product_id
WHERE NOT EXISTS (
    SELECT 1
    FROM order_items existing
    WHERE existing.order_id = o.id
      AND existing.variant_id = v.id
);

-- A second cohort creates a different co-purchase neighborhood around Samsung.
INSERT INTO orders (
    user_id, warehouse_id, status, total_amount, shipping_address, note,
    created_at, updated_at
)
SELECT
    u.id,
    w.id,
    CASE WHEN i % 2 = 0 THEN 'DELIVERED' ELSE 'SHIPPED' END,
    0,
    'Recommendation phone cohort address ' || i,
    'REC-DEMO-PHONE-' || LPAD(i::text, 2, '0'),
    NOW() - ((i + 2) || ' days')::INTERVAL,
    NOW() - ((i + 1) || ' days')::INTERVAL
FROM generate_series(1, 12) AS g(i)
JOIN users u
  ON u.username = 'demo_customer_' || LPAD((((i + 7) % 20) + 1)::text, 2, '0')
JOIN warehouses w ON w.code = 'WH-CENTRAL'
WHERE NOT EXISTS (
    SELECT 1
    FROM orders existing
    WHERE existing.note = 'REC-DEMO-PHONE-' || LPAD(i::text, 2, '0')
);

WITH item_seed(note, sku, quantity) AS (
    SELECT 'REC-DEMO-PHONE-' || LPAD(i::text, 2, '0'), 'DEMO-SKU-03', 1
    FROM generate_series(1, 12) AS g(i)
    UNION ALL
    SELECT 'REC-DEMO-PHONE-' || LPAD(i::text, 2, '0'), 'DEMO-SKU-04', 1
    FROM generate_series(1, 12) AS g(i)
    UNION ALL
    SELECT 'REC-DEMO-PHONE-' || LPAD(i::text, 2, '0'), 'DEMO-SKU-19', 1
    FROM generate_series(1, 8) AS g(i)
    UNION ALL
    SELECT 'REC-DEMO-PHONE-' || LPAD(i::text, 2, '0'), 'IP15PM-256-NT', 1
    FROM generate_series(1, 6) AS g(i)
)
INSERT INTO order_items (
    order_id, variant_id, product_name, variant_name, sku,
    quantity, unit_price, subtotal
)
SELECT
    o.id,
    v.id,
    p.name,
    v.variant_name,
    v.sku,
    item_seed.quantity,
    v.price,
    v.price * item_seed.quantity
FROM item_seed
JOIN orders o ON o.note = item_seed.note
JOIN product_variants v ON v.sku = item_seed.sku
JOIN products p ON p.id = v.product_id
WHERE NOT EXISTS (
    SELECT 1
    FROM order_items existing
    WHERE existing.order_id = o.id
      AND existing.variant_id = v.id
);

-- High-quantity PENDING/CANCELLED rows must never affect best-seller or
-- co-purchase results.
INSERT INTO orders (
    user_id, warehouse_id, status, total_amount, shipping_address, note,
    created_at, updated_at
)
SELECT
    u.id,
    w.id,
    CASE WHEN i <= 4 THEN 'CANCELLED' ELSE 'PENDING' END,
    0,
    'Recommendation excluded status address ' || i,
    'REC-DEMO-EXCLUDED-' || LPAD(i::text, 2, '0'),
    NOW() - (i || ' hours')::INTERVAL,
    NOW() - (i || ' hours')::INTERVAL
FROM generate_series(1, 8) AS g(i)
JOIN users u
  ON u.username = 'demo_customer_' || LPAD(i::text, 2, '0')
JOIN warehouses w ON w.code = 'WH-CENTRAL'
WHERE NOT EXISTS (
    SELECT 1
    FROM orders existing
    WHERE existing.note = 'REC-DEMO-EXCLUDED-' || LPAD(i::text, 2, '0')
);

WITH item_seed(note, sku, quantity) AS (
    SELECT
        'REC-DEMO-EXCLUDED-' || LPAD(i::text, 2, '0'),
        CASE WHEN i <= 4 THEN 'PS5-SLIM-DISC' ELSE 'DEMO-SKU-12' END,
        50
    FROM generate_series(1, 8) AS g(i)
)
INSERT INTO order_items (
    order_id, variant_id, product_name, variant_name, sku,
    quantity, unit_price, subtotal
)
SELECT
    o.id,
    v.id,
    p.name,
    v.variant_name,
    v.sku,
    item_seed.quantity,
    v.price,
    v.price * item_seed.quantity
FROM item_seed
JOIN orders o ON o.note = item_seed.note
JOIN product_variants v ON v.sku = item_seed.sku
JOIN products p ON p.id = v.product_id
WHERE NOT EXISTS (
    SELECT 1
    FROM order_items existing
    WHERE existing.order_id = o.id
      AND existing.variant_id = v.id
);

-- Delivered orders outside both configured lookback windows.
INSERT INTO orders (
    user_id, warehouse_id, status, total_amount, shipping_address, note,
    created_at, updated_at
)
SELECT
    u.id,
    w.id,
    'DELIVERED',
    0,
    'Recommendation expired cohort address ' || i,
    'REC-DEMO-EXPIRED-' || LPAD(i::text, 2, '0'),
    NOW() - ((110 + i) || ' days')::INTERVAL,
    NOW() - ((109 + i) || ' days')::INTERVAL
FROM generate_series(1, 4) AS g(i)
JOIN users u
  ON u.username = 'demo_customer_' || LPAD((i + 12)::text, 2, '0')
JOIN warehouses w ON w.code = 'WH-CENTRAL'
WHERE NOT EXISTS (
    SELECT 1
    FROM orders existing
    WHERE existing.note = 'REC-DEMO-EXPIRED-' || LPAD(i::text, 2, '0')
);

WITH item_seed(note, sku, quantity) AS (
    SELECT 'REC-DEMO-EXPIRED-' || LPAD(i::text, 2, '0'), 'DEMO-SKU-20', 100
    FROM generate_series(1, 4) AS g(i)
    UNION ALL
    SELECT 'REC-DEMO-EXPIRED-' || LPAD(i::text, 2, '0'), 'DEMO-SKU-06', 100
    FROM generate_series(1, 4) AS g(i)
)
INSERT INTO order_items (
    order_id, variant_id, product_name, variant_name, sku,
    quantity, unit_price, subtotal
)
SELECT
    o.id,
    v.id,
    p.name,
    v.variant_name,
    v.sku,
    item_seed.quantity,
    v.price,
    v.price * item_seed.quantity
FROM item_seed
JOIN orders o ON o.note = item_seed.note
JOIN product_variants v ON v.sku = item_seed.sku
JOIN products p ON p.id = v.product_id
WHERE NOT EXISTS (
    SELECT 1
    FROM order_items existing
    WHERE existing.order_id = o.id
      AND existing.variant_id = v.id
);

UPDATE orders o
SET total_amount = totals.amount,
    updated_at = GREATEST(o.updated_at, NOW())
FROM (
    SELECT oi.order_id, SUM(oi.subtotal) AS amount
    FROM order_items oi
    GROUP BY oi.order_id
) totals
WHERE totals.order_id = o.id
  AND o.note LIKE 'REC-DEMO-%';

-- ---------------------------------------------------------------------------
-- 3. Product-view co-occurrence cohorts
-- ---------------------------------------------------------------------------

-- Fifty distinct anonymous actors view Dell. Candidate cohort sizes produce a
-- deterministic ranking: Logitech 45, Keychron 35, LG 25, Bose 15, Pixel 5.
INSERT INTO analytics_events (
    event_id, schema_version, event_type, session_id, product_id,
    source, occurred_at, received_at, properties
)
SELECT
    md5('rec-v17:dell-source:' || i)::uuid,
    1,
    'PRODUCT_VIEW',
    'rec-dell-session-' || LPAD(i::text, 2, '0'),
    p.id,
    'DIRECT',
    NOW() - (((i - 1) % 20 + 1) || ' days')::INTERVAL,
    NOW(),
    jsonb_build_object('seedScenario', 'co_viewed_dell', 'actorIndex', i)
FROM generate_series(1, 50) AS g(i)
JOIN products p ON p.slug = 'dell-xps-14-9440'
ON CONFLICT (event_id) DO NOTHING;

WITH cohorts(slug, actor_count) AS (VALUES
    ('logitech-mx-master-3s', 45),
    ('keychron-q1-max', 35),
    ('lg-ultrafine-32-4k', 25),
    ('bose-quietcomfort-ultra', 15),
    ('google-pixel-9-pro', 5)
)
INSERT INTO analytics_events (
    event_id, schema_version, event_type, session_id, product_id,
    source, occurred_at, received_at, properties
)
SELECT
    md5('rec-v17:dell-candidate:' || cohorts.slug || ':' || i)::uuid,
    1,
    'PRODUCT_VIEW',
    'rec-dell-session-' || LPAD(i::text, 2, '0'),
    p.id,
    'CATALOG',
    NOW() - (((i + 2) % 20 + 1) || ' days')::INTERVAL,
    NOW(),
    jsonb_build_object('seedScenario', 'co_viewed_dell', 'candidateSlug', cohorts.slug)
FROM cohorts
CROSS JOIN LATERAL generate_series(1, cohorts.actor_count) AS g(i)
JOIN products p ON p.slug = cohorts.slug
ON CONFLICT (event_id) DO NOTHING;

-- Repeated views from the same actor verify distinct-actor deduplication.
INSERT INTO analytics_events (
    event_id, schema_version, event_type, session_id, product_id,
    source, occurred_at, received_at, properties
)
SELECT
    md5('rec-v17:dell-repeat:' || i)::uuid,
    1,
    'PRODUCT_VIEW',
    'rec-dell-session-' || LPAD(i::text, 2, '0'),
    p.id,
    'SEARCH',
    NOW() - (i || ' hours')::INTERVAL,
    NOW(),
    '{"seedScenario":"co_viewed_duplicate_view"}'::jsonb
FROM generate_series(1, 10) AS g(i)
JOIN products p ON p.slug = 'logitech-mx-master-3s'
ON CONFLICT (event_id) DO NOTHING;

-- A separate smartphone neighborhood.
INSERT INTO analytics_events (
    event_id, schema_version, event_type, session_id, product_id,
    source, occurred_at, received_at, properties
)
SELECT
    md5('rec-v17:phone-source:' || i)::uuid,
    1,
    'PRODUCT_VIEW',
    'rec-phone-session-' || LPAD(i::text, 2, '0'),
    p.id,
    'DIRECT',
    NOW() - (((i - 1) % 18 + 1) || ' days')::INTERVAL,
    NOW(),
    '{"seedScenario":"co_viewed_phone"}'::jsonb
FROM generate_series(1, 30) AS g(i)
JOIN products p ON p.slug = 'samsung-galaxy-s24-ultra'
ON CONFLICT (event_id) DO NOTHING;

WITH cohorts(slug, actor_count) AS (VALUES
    ('google-pixel-9-pro', 28),
    ('iphone-15-pro-max', 18),
    ('anker-prime-20000', 12),
    ('apple-watch-ultra-2', 5)
)
INSERT INTO analytics_events (
    event_id, schema_version, event_type, session_id, product_id,
    source, occurred_at, received_at, properties
)
SELECT
    md5('rec-v17:phone-candidate:' || cohorts.slug || ':' || i)::uuid,
    1,
    'PRODUCT_VIEW',
    'rec-phone-session-' || LPAD(i::text, 2, '0'),
    p.id,
    'CATALOG',
    NOW() - (((i + 3) % 18 + 1) || ' days')::INTERVAL,
    NOW(),
    jsonb_build_object('seedScenario', 'co_viewed_phone', 'candidateSlug', cohorts.slug)
FROM cohorts
CROSS JOIN LATERAL generate_series(1, cohorts.actor_count) AS g(i)
JOIN products p ON p.slug = cohorts.slug
ON CONFLICT (event_id) DO NOTHING;

-- Authenticated actors with no session ID exercise the user fallback actor key.
WITH actor_seed(username, candidate_slug) AS (VALUES
    ('demo_customer_01', 'nintendo-switch-oled'),
    ('demo_customer_02', 'nintendo-switch-oled'),
    ('demo_customer_03', 'nintendo-switch-oled'),
    ('demo_customer_04', 'nintendo-switch-oled'),
    ('demo_customer_05', 'nintendo-switch-oled'),
    ('demo_customer_06', 'nintendo-switch-oled'),
    ('demo_customer_07', 'asus-rog-zephyrus-g14'),
    ('demo_customer_08', 'asus-rog-zephyrus-g14'),
    ('demo_customer_09', 'asus-rog-zephyrus-g14'),
    ('demo_customer_10', 'asus-rog-zephyrus-g14')
)
INSERT INTO analytics_events (
    event_id, schema_version, event_type, user_id, product_id,
    source, occurred_at, received_at, properties
)
SELECT
    md5('rec-v17:user-view:source:' || actor_seed.username)::uuid,
    1,
    'PRODUCT_VIEW',
    u.id,
    source_product.id,
    'DIRECT',
    NOW() - INTERVAL '3 days',
    NOW(),
    '{"seedScenario":"co_viewed_user_fallback","role":"source"}'::jsonb
FROM actor_seed
JOIN users u ON u.username = actor_seed.username
JOIN products source_product ON source_product.slug = 'playstation-5-slim'
ON CONFLICT (event_id) DO NOTHING;

WITH actor_seed(username, candidate_slug) AS (VALUES
    ('demo_customer_01', 'nintendo-switch-oled'),
    ('demo_customer_02', 'nintendo-switch-oled'),
    ('demo_customer_03', 'nintendo-switch-oled'),
    ('demo_customer_04', 'nintendo-switch-oled'),
    ('demo_customer_05', 'nintendo-switch-oled'),
    ('demo_customer_06', 'nintendo-switch-oled'),
    ('demo_customer_07', 'asus-rog-zephyrus-g14'),
    ('demo_customer_08', 'asus-rog-zephyrus-g14'),
    ('demo_customer_09', 'asus-rog-zephyrus-g14'),
    ('demo_customer_10', 'asus-rog-zephyrus-g14')
)
INSERT INTO analytics_events (
    event_id, schema_version, event_type, user_id, product_id,
    source, occurred_at, received_at, properties
)
SELECT
    md5('rec-v17:user-view:candidate:' || actor_seed.username || ':' || actor_seed.candidate_slug)::uuid,
    1,
    'PRODUCT_VIEW',
    u.id,
    candidate.id,
    'CATALOG',
    NOW() - INTERVAL '2 days',
    NOW(),
    '{"seedScenario":"co_viewed_user_fallback","role":"candidate"}'::jsonb
FROM actor_seed
JOIN users u ON u.username = actor_seed.username
JOIN products candidate ON candidate.slug = actor_seed.candidate_slug
ON CONFLICT (event_id) DO NOTHING;

-- Valid events outside the 90-day default lookback must not affect ranking.
WITH old_pair(role, slug) AS (VALUES
    ('source', 'dell-xps-14-9440'),
    ('candidate', 'dji-mini-4-pro')
)
INSERT INTO analytics_events (
    event_id, schema_version, event_type, session_id, product_id,
    source, occurred_at, received_at, properties
)
SELECT
    md5('rec-v17:expired-view:' || old_pair.role || ':' || i)::uuid,
    1,
    'PRODUCT_VIEW',
    'rec-expired-session-' || LPAD(i::text, 2, '0'),
    p.id,
    'DIRECT',
    NOW() - ((100 + i) || ' days')::INTERVAL,
    NOW(),
    '{"seedScenario":"co_viewed_expired"}'::jsonb
FROM old_pair
CROSS JOIN generate_series(1, 8) AS g(i)
JOIN products p ON p.slug = old_pair.slug
ON CONFLICT (event_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4. Recommendation attribution funnels
-- ---------------------------------------------------------------------------

-- Twenty-four impressions from a SIMILAR carousel.
WITH journey AS (
    SELECT
        i,
        md5('rec-v17:similar-request:' || i)::uuid AS request_id,
        CASE (i % 3)
            WHEN 0 THEN 'logitech-mx-master-3s'
            WHEN 1 THEN 'keychron-q1-max'
            ELSE 'lg-ultrafine-32-4k'
        END AS candidate_slug
    FROM generate_series(1, 24) AS g(i)
)
INSERT INTO analytics_events (
    event_id, schema_version, event_type, session_id, product_id, variant_id,
    source, placement, recommendation_request_id, strategy, position,
    occurred_at, received_at, properties
)
SELECT
    md5('rec-v17:similar-impression:' || journey.i)::uuid,
    1,
    'RECOMMENDATION_IMPRESSION',
    'rec-funnel-session-' || LPAD(journey.i::text, 2, '0'),
    p.id,
    v.id,
    'RECOMMENDATION',
    'PRODUCT_DETAIL_SIMILAR',
    journey.request_id,
    'SIMILAR',
    (journey.i - 1) % 6,
    NOW() - ((30 - journey.i) || ' hours')::INTERVAL,
    NOW(),
    jsonb_build_object('seedScenario', 'similar_funnel', 'sourceSlug', 'dell-xps-14-9440')
FROM journey
JOIN products p ON p.slug = journey.candidate_slug
JOIN LATERAL (
    SELECT id
    FROM product_variants
    WHERE product_id = p.id AND active = TRUE
    ORDER BY id
    LIMIT 1
) v ON TRUE
ON CONFLICT (event_id) DO NOTHING;

-- Fourteen of the impressions are clicked.
WITH journey AS (
    SELECT
        i,
        md5('rec-v17:similar-request:' || i)::uuid AS request_id,
        CASE (i % 3)
            WHEN 0 THEN 'logitech-mx-master-3s'
            WHEN 1 THEN 'keychron-q1-max'
            ELSE 'lg-ultrafine-32-4k'
        END AS candidate_slug
    FROM generate_series(1, 14) AS g(i)
)
INSERT INTO analytics_events (
    event_id, schema_version, event_type, session_id, product_id, variant_id,
    source, placement, recommendation_request_id, strategy, position,
    occurred_at, received_at, properties
)
SELECT
    md5('rec-v17:similar-click:' || journey.i)::uuid,
    1,
    'RECOMMENDATION_CLICK',
    'rec-funnel-session-' || LPAD(journey.i::text, 2, '0'),
    p.id,
    v.id,
    'RECOMMENDATION',
    'PRODUCT_DETAIL_SIMILAR',
    journey.request_id,
    'SIMILAR',
    (journey.i - 1) % 6,
    NOW() - ((29 - journey.i) || ' hours')::INTERVAL,
    NOW(),
    '{"seedScenario":"similar_funnel"}'::jsonb
FROM journey
JOIN products p ON p.slug = journey.candidate_slug
JOIN LATERAL (
    SELECT id
    FROM product_variants
    WHERE product_id = p.id AND active = TRUE
    ORDER BY id
    LIMIT 1
) v ON TRUE
ON CONFLICT (event_id) DO NOTHING;

-- Ten clicked products are added to cart.
WITH journey AS (
    SELECT
        i,
        md5('rec-v17:similar-request:' || i)::uuid AS request_id,
        CASE (i % 3)
            WHEN 0 THEN 'logitech-mx-master-3s'
            WHEN 1 THEN 'keychron-q1-max'
            ELSE 'lg-ultrafine-32-4k'
        END AS candidate_slug
    FROM generate_series(1, 10) AS g(i)
)
INSERT INTO analytics_events (
    event_id, schema_version, event_type, session_id, product_id, variant_id,
    source, placement, recommendation_request_id, strategy, position, quantity,
    occurred_at, received_at, properties
)
SELECT
    md5('rec-v17:similar-cart:' || journey.i)::uuid,
    1,
    'ADD_TO_CART',
    'rec-funnel-session-' || LPAD(journey.i::text, 2, '0'),
    p.id,
    v.id,
    'RECOMMENDATION',
    'PRODUCT_DETAIL_SIMILAR',
    journey.request_id,
    'SIMILAR',
    (journey.i - 1) % 6,
    1 + CASE WHEN journey.i % 4 = 0 THEN 1 ELSE 0 END,
    NOW() - ((28 - journey.i) || ' hours')::INTERVAL,
    NOW(),
    '{"seedScenario":"similar_funnel"}'::jsonb
FROM journey
JOIN products p ON p.slug = journey.candidate_slug
JOIN LATERAL (
    SELECT id
    FROM product_variants
    WHERE product_id = p.id AND active = TRUE
    ORDER BY id
    LIMIT 1
) v ON TRUE
ON CONFLICT (event_id) DO NOTHING;

-- Eight attributed trusted purchase events complete the funnel.
WITH journey AS (
    SELECT
        i,
        md5('rec-v17:similar-request:' || i)::uuid AS request_id,
        CASE (i % 3)
            WHEN 0 THEN 'logitech-mx-master-3s'
            WHEN 1 THEN 'keychron-q1-max'
            ELSE 'lg-ultrafine-32-4k'
        END AS candidate_slug
    FROM generate_series(1, 8) AS g(i)
)
INSERT INTO analytics_events (
    event_id, schema_version, event_type, user_id, product_id, variant_id,
    source, placement, recommendation_request_id, strategy, position,
    quantity, order_id, unit_price, occurred_at, received_at, properties
)
SELECT
    md5('rec-v17:similar-purchase:' || journey.i)::uuid,
    1,
    'PURCHASE',
    o.user_id,
    p.id,
    v.id,
    'ORDER_SERVICE',
    'PRODUCT_DETAIL_SIMILAR',
    journey.request_id,
    'SIMILAR',
    (journey.i - 1) % 6,
    oi.quantity,
    o.id,
    oi.unit_price,
    o.created_at AT TIME ZONE 'UTC',
    NOW(),
    '{"seedScenario":"similar_funnel","trusted":true}'::jsonb
FROM journey
JOIN orders o
  ON o.note = 'REC-DEMO-BUNDLE-' || LPAD(journey.i::text, 2, '0')
JOIN products p ON p.slug = journey.candidate_slug
JOIN product_variants v ON v.product_id = p.id AND v.sku LIKE 'DEMO-SKU-%'
JOIN order_items oi ON oi.order_id = o.id AND oi.variant_id = v.id
ON CONFLICT (event_id) DO NOTHING;

-- Home best-seller impressions/clicks cover a second placement/strategy pair.
WITH journey AS (
    SELECT
        i,
        md5('rec-v17:best-seller-request:' || i)::uuid AS request_id,
        CASE (i % 3)
            WHEN 0 THEN 'logitech-mx-master-3s'
            WHEN 1 THEN 'dell-xps-14-9440'
            ELSE 'samsung-galaxy-s24-ultra'
        END AS candidate_slug
    FROM generate_series(1, 12) AS g(i)
)
INSERT INTO analytics_events (
    event_id, schema_version, event_type, session_id, product_id,
    source, placement, recommendation_request_id, strategy, position,
    occurred_at, received_at, properties
)
SELECT
    md5('rec-v17:best-seller-impression:' || journey.i)::uuid,
    1,
    'RECOMMENDATION_IMPRESSION',
    'rec-home-session-' || LPAD(journey.i::text, 2, '0'),
    p.id,
    'RECOMMENDATION',
    'HOME_BEST_SELLERS',
    journey.request_id,
    'BEST_SELLER',
    (journey.i - 1) % 4,
    NOW() - (journey.i || ' hours')::INTERVAL,
    NOW(),
    '{"seedScenario":"best_seller_funnel"}'::jsonb
FROM journey
JOIN products p ON p.slug = journey.candidate_slug
ON CONFLICT (event_id) DO NOTHING;

WITH journey AS (
    SELECT
        i,
        md5('rec-v17:best-seller-request:' || i)::uuid AS request_id,
        CASE (i % 3)
            WHEN 0 THEN 'logitech-mx-master-3s'
            WHEN 1 THEN 'dell-xps-14-9440'
            ELSE 'samsung-galaxy-s24-ultra'
        END AS candidate_slug
    FROM generate_series(1, 5) AS g(i)
)
INSERT INTO analytics_events (
    event_id, schema_version, event_type, session_id, product_id,
    source, placement, recommendation_request_id, strategy, position,
    occurred_at, received_at, properties
)
SELECT
    md5('rec-v17:best-seller-click:' || journey.i)::uuid,
    1,
    'RECOMMENDATION_CLICK',
    'rec-home-session-' || LPAD(journey.i::text, 2, '0'),
    p.id,
    'RECOMMENDATION',
    'HOME_BEST_SELLERS',
    journey.request_id,
    'BEST_SELLER',
    (journey.i - 1) % 4,
    NOW() - (journey.i || ' hours')::INTERVAL + INTERVAL '5 minutes',
    NOW(),
    '{"seedScenario":"best_seller_funnel"}'::jsonb
FROM journey
JOIN products p ON p.slug = journey.candidate_slug
ON CONFLICT (event_id) DO NOTHING;
