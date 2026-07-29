-- V3: warehouses + stock_levels. Stock is keyed by (variant_id, warehouse_id).
CREATE TABLE warehouses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20)  NOT NULL UNIQUE,
    address TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE stock_levels (
    id BIGSERIAL PRIMARY KEY,
    variant_id   BIGINT NOT NULL REFERENCES product_variants(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    quantity          INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_stock_variant_warehouse UNIQUE (variant_id, warehouse_id)
);

-- Seed warehouses + initial stock so the storefront shows availability out of the box
INSERT INTO warehouses (id, name, code, address, active, created_at, updated_at) VALUES
(1, 'Central Warehouse', 'WH-CENTRAL', 'Ho Chi Minh City',  true, NOW(), NOW()),
(2, 'North Warehouse',   'WH-NORTH',   'Ha Noi',            true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
SELECT setval('warehouses_id_seq', (SELECT MAX(id) FROM warehouses));

INSERT INTO stock_levels (variant_id, warehouse_id, quantity, reserved_quantity, version, updated_at)
SELECT v.id, 1, 50, 0, 0, NOW() FROM product_variants v
ON CONFLICT (variant_id, warehouse_id) DO NOTHING;
