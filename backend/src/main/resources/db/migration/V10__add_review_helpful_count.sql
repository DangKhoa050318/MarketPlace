ALTER TABLE product_reviews
    ADD COLUMN helpful_count BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_reviews_product_helpful
    ON product_reviews(product_id, helpful_count DESC, created_at DESC)
    WHERE status = 'APPROVED' AND deleted_at IS NULL;
