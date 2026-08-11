CREATE TABLE product_attribute_terms (
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    term       VARCHAR(500) NOT NULL,
    PRIMARY KEY (product_id, term)
);

CREATE INDEX idx_product_attribute_terms_term_product
    ON product_attribute_terms (term, product_id);

CREATE TABLE product_similarities (
    source_product_id    BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    candidate_product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    score                DOUBLE PRECISION NOT NULL,
    reason               VARCHAR(255) NOT NULL,
    rank                 INTEGER NOT NULL,
    computed_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (source_product_id, candidate_product_id),
    CONSTRAINT chk_product_similarity_not_self
        CHECK (source_product_id <> candidate_product_id),
    CONSTRAINT chk_product_similarity_rank_positive CHECK (rank > 0)
);

CREATE UNIQUE INDEX idx_product_similarities_source_rank
    ON product_similarities (source_product_id, rank);

CREATE TABLE product_similarity_status (
    source_product_id BIGINT PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    computed_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION refresh_product_attribute_terms()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM product_attribute_terms WHERE product_id = NEW.id;

    INSERT INTO product_attribute_terms (product_id, term)
    SELECT NEW.id,
           LOWER(BTRIM(attribute.key)) || '=' || LOWER(BTRIM(attribute.value))
      FROM jsonb_each_text(COALESCE(NEW.attributes, '{}'::jsonb)) AS attribute
     WHERE BTRIM(attribute.key) <> ''
       AND BTRIM(attribute.value) <> ''
    ON CONFLICT DO NOTHING;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_products_refresh_attribute_terms
AFTER INSERT OR UPDATE OF attributes ON products
FOR EACH ROW
EXECUTE FUNCTION refresh_product_attribute_terms();

INSERT INTO product_attribute_terms (product_id, term)
SELECT p.id,
       LOWER(BTRIM(attribute.key)) || '=' || LOWER(BTRIM(attribute.value))
  FROM products p
 CROSS JOIN LATERAL jsonb_each_text(COALESCE(p.attributes, '{}'::jsonb)) AS attribute
 WHERE BTRIM(attribute.key) <> ''
   AND BTRIM(attribute.value) <> ''
ON CONFLICT DO NOTHING;

CREATE INDEX idx_products_category_active_id
    ON products (category_id, id)
    WHERE active = TRUE;

CREATE INDEX idx_products_normalized_brand_active_id
    ON products (LOWER(BTRIM(brand)), id)
    WHERE active = TRUE AND brand IS NOT NULL;

CREATE INDEX idx_variants_active_product_price
    ON product_variants (product_id, price)
    WHERE active = TRUE;
