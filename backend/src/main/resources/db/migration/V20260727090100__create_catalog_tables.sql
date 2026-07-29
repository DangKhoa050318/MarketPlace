-- V2: catalog (merged, 2-tier SPU/SKU).
--   categories : code (StockPulse internal) + slug (OrderFlow URL) + parent_id (hierarchy)
--   products   : SPU level (NO sku/price/stock here)
--   product_variants : SKU level — sku + price + stock-related settings live here

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20)  NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    parent_id BIGINT REFERENCES categories(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category_id BIGINT REFERENCES categories(id),
    unit VARCHAR(20) NOT NULL DEFAULT 'PCS',
    image_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    search_vector TSVECTOR,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE product_variants (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    sku VARCHAR(50) NOT NULL UNIQUE,
    variant_name VARCHAR(255) NOT NULL,
    color VARCHAR(50),
    size  VARCHAR(50),
    price DECIMAL(12,2) NOT NULL,
    image_url VARCHAR(500),
    min_stock INT NOT NULL DEFAULT 10,
    max_stock INT NOT NULL DEFAULT 1000,
    reorder_point    INT NOT NULL DEFAULT 20,
    reorder_quantity INT NOT NULL DEFAULT 100,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_variant_product_name UNIQUE (product_id, variant_name)
);

-- ---- Seed categories ---------------------------------------------------------
INSERT INTO categories (id, name, code, slug, parent_id, created_at, updated_at) VALUES
(1, 'Electronics',                'ELEC',  'electronics',                NULL, NOW(), NOW()),
(2, 'Computers & Laptops',        'COMP',  'computers-laptops',          1,    NOW(), NOW()),
(3, 'Smartphones & Accessories',  'PHONE', 'smartphones-accessories',    1,    NOW(), NOW()),
(4, 'Audio & Headphones',         'AUDIO', 'audio-headphones',           1,    NOW(), NOW()),
(5, 'Gaming & Consoles',          'GAME',  'gaming-consoles',            1,    NOW(), NOW()),
(6, 'Smart Home Devices',         'HOME',  'smart-home-devices',         1,    NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));

-- ---- Seed products (SPU) -----------------------------------------------------
INSERT INTO products (id, slug, name, description, category_id, unit, image_url, active, created_at, updated_at) VALUES
(1, 'macbook-pro-16-m3-max',      'MacBook Pro 16" M3 Max',              'Apple M3 Max, 16-core CPU, 40-core GPU, 36GB, 1TB SSD.', 2, 'PCS', 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=800&q=80', true, NOW(), NOW()),
(2, 'iphone-15-pro-max',          'iPhone 15 Pro Max',                   'Titanium design, A17 Pro chip, 48MP Main Camera.',      3, 'PCS', 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?auto=format&fit=crop&w=800&q=80', true, NOW(), NOW()),
(3, 'sony-wh-1000xm5',            'Sony WH-1000XM5 Wireless Headphones', 'Industry-leading noise canceling, 30-hour battery.',    4, 'PCS', 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=800&q=80', true, NOW(), NOW()),
(4, 'playstation-5-slim',         'Sony PlayStation 5 Slim Console',     'Ultra-high speed SSD, 4K gaming, 1TB storage.',         5, 'PCS', 'https://images.unsplash.com/photo-1606813907291-d86efa9b94db?auto=format&fit=crop&w=800&q=80', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));

-- ---- Seed variants (SKU) — mỗi sản phẩm vài mẫu mã --------------------------
INSERT INTO product_variants (id, product_id, sku, variant_name, color, size, price, image_url, min_stock, max_stock, reorder_point, reorder_quantity, active, created_at, updated_at) VALUES
(1, 1, 'MBP16-SLV-1TB', 'Silver / 1TB',      'Silver',      '1TB',  2499.00, NULL, 5, 100, 10, 20, true, NOW(), NOW()),
(2, 1, 'MBP16-SPG-1TB', 'Space Black / 1TB', 'Space Black', '1TB',  2599.00, NULL, 5, 100, 10, 20, true, NOW(), NOW()),
(3, 2, 'IP15PM-256-NT', 'Natural / 256GB',   'Natural',     '256GB',1199.99, NULL, 10,200, 20, 50, true, NOW(), NOW()),
(4, 2, 'IP15PM-512-BL', 'Blue / 512GB',      'Blue',        '512GB',1399.99, NULL, 10,200, 20, 50, true, NOW(), NOW()),
(5, 3, 'WH1000XM5-BLK', 'Black',             'Black',       NULL,    399.99, NULL, 15,300, 30, 60, true, NOW(), NOW()),
(6, 3, 'WH1000XM5-SLV', 'Silver',            'Silver',      NULL,    399.99, NULL, 15,300, 30, 60, true, NOW(), NOW()),
(7, 4, 'PS5-SLIM-DISC', 'Disc Edition',      NULL,          NULL,    499.99, NULL, 10,150, 20, 40, true, NOW(), NOW()),
(8, 4, 'PS5-SLIM-DIGI', 'Digital Edition',   NULL,          NULL,    449.99, NULL, 10,150, 20, 40, true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
SELECT setval('product_variants_id_seq', (SELECT MAX(id) FROM product_variants));
