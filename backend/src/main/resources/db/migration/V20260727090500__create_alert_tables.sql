-- V6: stock alerts + reorder suggestions. Keyed by variant_id.
CREATE TABLE stock_alerts (
    id BIGSERIAL PRIMARY KEY,
    variant_id   BIGINT NOT NULL REFERENCES product_variants(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    alert_type VARCHAR(20) NOT NULL,       -- LOW_STOCK | OUT_OF_STOCK | OVERSTOCK
    current_quantity INT NOT NULL,
    threshold INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP
);

CREATE TABLE reorder_suggestions (
    id BIGSERIAL PRIMARY KEY,
    variant_id   BIGINT NOT NULL REFERENCES product_variants(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    suggested_quantity INT NOT NULL,
    current_stock INT NOT NULL,
    reorder_point INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
