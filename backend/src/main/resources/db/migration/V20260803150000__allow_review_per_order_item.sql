-- Remove 1-review-per-product constraint and allow 1 review per order_item (per purchase)
ALTER TABLE product_reviews DROP CONSTRAINT IF EXISTS uk_user_product;

-- Add unique index so each order_item can only be reviewed once per user
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_order_item
    ON product_reviews(user_id, order_item_id)
    WHERE order_item_id IS NOT NULL AND deleted_at IS NULL;
