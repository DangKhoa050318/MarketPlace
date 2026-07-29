-- Product-level metadata required by the rule-based similar-product strategy.
-- The change is additive so existing catalog writes and storefront reads remain compatible.
ALTER TABLE products
    ADD COLUMN brand VARCHAR(120),
    ADD COLUMN attributes JSONB NOT NULL DEFAULT '{}'::jsonb;

CREATE INDEX idx_products_brand_active
    ON products (LOWER(brand))
    WHERE active = TRUE AND brand IS NOT NULL;

CREATE INDEX idx_products_attributes
    ON products USING GIN (attributes);

-- Enrich the original demo catalog without changing product or variant identities.
UPDATE products
SET brand = CASE id
        WHEN 1 THEN 'Apple'
        WHEN 2 THEN 'Apple'
        WHEN 3 THEN 'Sony'
        WHEN 4 THEN 'Sony'
        ELSE brand
    END,
    attributes = CASE id
        WHEN 1 THEN '{"productType":"laptop","processor":"Apple M3 Max","storage":"1TB"}'::jsonb
        WHEN 2 THEN '{"productType":"smartphone","processor":"Apple A17 Pro","camera":"48MP"}'::jsonb
        WHEN 3 THEN '{"productType":"headphones","connectivity":"wireless","noiseCancelling":"true"}'::jsonb
        WHEN 4 THEN '{"productType":"game-console","storage":"1TB","resolution":"4K"}'::jsonb
        ELSE attributes
    END
WHERE id IN (1, 2, 3, 4);
