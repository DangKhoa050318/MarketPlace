-- V5: stock movements + items. Movement items keyed by variant_id.
-- source_order_id links an auto-generated EXPORT movement back to the order that caused it.
CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    reference_no VARCHAR(50) NOT NULL UNIQUE,
    type   VARCHAR(20) NOT NULL,          -- IMPORT | EXPORT | TRANSFER | ADJUSTMENT
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    warehouse_id      BIGINT NOT NULL REFERENCES warehouses(id),
    dest_warehouse_id BIGINT REFERENCES warehouses(id),
    source_order_id   BIGINT REFERENCES orders(id),
    notes TEXT,
    created_by  BIGINT NOT NULL REFERENCES users(id),
    approved_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE stock_movement_items (
    id BIGSERIAL PRIMARY KEY,
    movement_id BIGINT NOT NULL REFERENCES stock_movements(id),
    variant_id  BIGINT NOT NULL REFERENCES product_variants(id),
    quantity INT NOT NULL,
    unit_cost DECIMAL(12,2),
    batch_number VARCHAR(50),
    expiry_date DATE,
    notes TEXT
);
