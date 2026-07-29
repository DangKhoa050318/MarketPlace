-- V4: orders + order_items. Order items are keyed by variant_id (with snapshots).
-- Created before movements (V5) because stock_movements.source_order_id references orders(id).
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    warehouse_id BIGINT REFERENCES warehouses(id),   -- kho xuất; MVP = kho mặc định
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(12,2) NOT NULL,
    shipping_address TEXT,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id   BIGINT NOT NULL REFERENCES orders(id),
    variant_id BIGINT NOT NULL REFERENCES product_variants(id),
    product_name VARCHAR(255) NOT NULL,   -- snapshot
    variant_name VARCHAR(255) NOT NULL,   -- snapshot
    sku VARCHAR(50) NOT NULL,             -- snapshot
    quantity   INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,     -- snapshot
    subtotal   DECIMAL(12,2) NOT NULL
);
