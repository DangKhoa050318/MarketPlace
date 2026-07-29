-- Development/demo dataset: 20 coherent rows for each core business table.
-- Prefixes make the migration idempotent for manually restored databases and
-- keep demo records easy to identify.

-- Password for every demo customer is "admin123".
INSERT INTO users (username, email, password, full_name, role, active)
SELECT
    'demo_customer_' || LPAD(i::text, 2, '0'),
    'demo_customer_' || LPAD(i::text, 2, '0') || '@marketplace.test',
    '$2a$10$ZC4eiPn25JMEntSoCC/0uuUlxO.dmifem0QW0LrY5ImHaUaBQwSFe',
    'Demo Customer ' || LPAD(i::text, 2, '0'),
    'CUSTOMER',
    TRUE
FROM generate_series(1, 20) AS g(i)
ON CONFLICT (username) DO NOTHING;

INSERT INTO categories (name, code, slug)
SELECT
    'Demo Category ' || LPAD(i::text, 2, '0'),
    'DEMO' || LPAD(i::text, 2, '0'),
    'demo-category-' || LPAD(i::text, 2, '0')
FROM generate_series(1, 20) AS g(i)
ON CONFLICT (code) DO NOTHING;

INSERT INTO products (slug, name, description, category_id, unit, image_url, active)
SELECT
    'demo-product-' || LPAD(i::text, 2, '0'),
    'Demo Product ' || LPAD(i::text, 2, '0'),
    'Consistent sample product ' || i || ' for storefront, inventory and review testing.',
    (SELECT id FROM categories WHERE code = 'DEMO' || LPAD(i::text, 2, '0')),
    'PCS',
    'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=800&q=80',
    TRUE
FROM generate_series(1, 20) AS g(i)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO product_variants (
    product_id, sku, variant_name, color, size, price, image_url,
    min_stock, max_stock, reorder_point, reorder_quantity, active
)
SELECT
    (SELECT id FROM products WHERE slug = 'demo-product-' || LPAD(i::text, 2, '0')),
    'DEMO-SKU-' || LPAD(i::text, 2, '0'),
    'Standard / ' || LPAD(i::text, 2, '0'),
    CASE (i % 4) WHEN 0 THEN 'Black' WHEN 1 THEN 'Silver' WHEN 2 THEN 'Blue' ELSE 'White' END,
    CASE (i % 3) WHEN 0 THEN 'Large' WHEN 1 THEN 'Small' ELSE 'Medium' END,
    (49.99 + i * 10)::DECIMAL(12, 2),
    NULL,
    5,
    500,
    15,
    50,
    TRUE
FROM generate_series(1, 20) AS g(i)
ON CONFLICT (sku) DO NOTHING;

INSERT INTO warehouses (name, code, address, active)
SELECT
    'Demo Warehouse ' || LPAD(i::text, 2, '0'),
    'DEMO-WH-' || LPAD(i::text, 2, '0'),
    'Demo address ' || i || ', Viet Nam',
    TRUE
FROM generate_series(1, 20) AS g(i)
ON CONFLICT (code) DO NOTHING;

INSERT INTO stock_levels (variant_id, warehouse_id, quantity, reserved_quantity, version)
SELECT
    (SELECT id FROM product_variants WHERE sku = 'DEMO-SKU-' || LPAD(i::text, 2, '0')),
    (SELECT id FROM warehouses WHERE code = 'DEMO-WH-' || LPAD(i::text, 2, '0')),
    40 + i,
    i % 5,
    0
FROM generate_series(1, 20) AS g(i)
ON CONFLICT (variant_id, warehouse_id) DO NOTHING;

INSERT INTO orders (
    user_id, warehouse_id, status, total_amount, shipping_address, note,
    created_at, updated_at
)
SELECT
    (SELECT id FROM users WHERE username = 'demo_customer_' || LPAD(i::text, 2, '0')),
    (SELECT id FROM warehouses WHERE code = 'DEMO-WH-' || LPAD(i::text, 2, '0')),
    'DELIVERED',
    (49.99 + i * 10)::DECIMAL(12, 2),
    'Demo customer address ' || i,
    'Demo delivered order ' || i,
    NOW() - (i || ' days')::INTERVAL,
    NOW() - ((i - 1)::text || ' days')::INTERVAL
FROM generate_series(1, 20) AS g(i);

INSERT INTO order_items (
    order_id, variant_id, product_name, variant_name, sku, quantity,
    unit_price, subtotal
)
SELECT
    (
        SELECT id FROM orders
        WHERE note = 'Demo delivered order ' || i
        ORDER BY id DESC LIMIT 1
    ),
    v.id,
    p.name,
    v.variant_name,
    v.sku,
    1,
    v.price,
    v.price
FROM generate_series(1, 20) AS g(i)
JOIN product_variants v ON v.sku = 'DEMO-SKU-' || LPAD(i::text, 2, '0')
JOIN products p ON p.id = v.product_id;

INSERT INTO stock_movements (
    reference_no, type, status, warehouse_id, source_order_id, notes,
    created_by, approved_by
)
SELECT
    'DEMO-MOV-' || LPAD(i::text, 2, '0'),
    'EXPORT',
    'COMPLETED',
    (SELECT id FROM warehouses WHERE code = 'DEMO-WH-' || LPAD(i::text, 2, '0')),
    (
        SELECT id FROM orders
        WHERE note = 'Demo delivered order ' || i
        ORDER BY id DESC LIMIT 1
    ),
    'Demo stock export ' || i,
    (SELECT id FROM users WHERE username = 'staff'),
    (SELECT id FROM users WHERE username = 'manager')
FROM generate_series(1, 20) AS g(i)
ON CONFLICT (reference_no) DO NOTHING;

INSERT INTO stock_movement_items (
    movement_id, variant_id, quantity, unit_cost, batch_number, notes
)
SELECT
    (SELECT id FROM stock_movements WHERE reference_no = 'DEMO-MOV-' || LPAD(i::text, 2, '0')),
    (SELECT id FROM product_variants WHERE sku = 'DEMO-SKU-' || LPAD(i::text, 2, '0')),
    1,
    (39.99 + i * 8)::DECIMAL(12, 2),
    'DEMO-BATCH-' || LPAD(i::text, 2, '0'),
    'Movement item generated for testing'
FROM generate_series(1, 20) AS g(i);

INSERT INTO stock_alerts (
    variant_id, warehouse_id, alert_type, current_quantity, threshold,
    status, created_at, resolved_at
)
SELECT
    (SELECT id FROM product_variants WHERE sku = 'DEMO-SKU-' || LPAD(i::text, 2, '0')),
    (SELECT id FROM warehouses WHERE code = 'DEMO-WH-' || LPAD(i::text, 2, '0')),
    CASE WHEN i % 3 = 0 THEN 'OUT_OF_STOCK' ELSE 'LOW_STOCK' END,
    i % 4,
    15,
    CASE WHEN i % 5 = 0 THEN 'RESOLVED' ELSE 'ACTIVE' END,
    NOW() - (i || ' hours')::INTERVAL,
    CASE WHEN i % 5 = 0 THEN NOW() ELSE NULL END
FROM generate_series(1, 20) AS g(i);

INSERT INTO reorder_suggestions (
    variant_id, warehouse_id, suggested_quantity, current_stock,
    reorder_point, status
)
SELECT
    (SELECT id FROM product_variants WHERE sku = 'DEMO-SKU-' || LPAD(i::text, 2, '0')),
    (SELECT id FROM warehouses WHERE code = 'DEMO-WH-' || LPAD(i::text, 2, '0')),
    50 + i,
    i % 4,
    15,
    'PENDING'
FROM generate_series(1, 20) AS g(i)
ON CONFLICT DO NOTHING;

INSERT INTO product_reviews (
    user_id, product_id, order_item_id, rating, title, content, status,
    is_verified_purchase, is_edited, helpful_count, created_at, updated_at
)
SELECT
    u.id,
    p.id,
    oi.id,
    ((i - 1) % 5) + 1,
    'Demo review ' || LPAD(i::text, 2, '0'),
    'This is a verified sample review with enough detail for UI and pagination testing.',
    'APPROVED',
    TRUE,
    i % 4 = 0,
    (20 - i),
    NOW() - (i || ' days')::INTERVAL,
    NOW() - ((i - 1)::text || ' days')::INTERVAL
FROM generate_series(1, 20) AS g(i)
JOIN users u ON u.username = 'demo_customer_' || LPAD(i::text, 2, '0')
JOIN products p ON p.slug = 'demo-product-' || LPAD(i::text, 2, '0')
JOIN product_variants v ON v.product_id = p.id
JOIN order_items oi ON oi.variant_id = v.id
JOIN orders o ON o.id = oi.order_id AND o.user_id = u.id
ON CONFLICT (user_id, product_id) DO NOTHING;
