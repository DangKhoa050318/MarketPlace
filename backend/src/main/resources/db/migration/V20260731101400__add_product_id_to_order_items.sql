-- Add product_id column to order_items table
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS product_id BIGINT;
