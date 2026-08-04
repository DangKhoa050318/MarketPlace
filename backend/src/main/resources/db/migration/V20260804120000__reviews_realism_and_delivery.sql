-- FEATURE-03 realism upgrades (G3/G4/G5)

-- G4: seller/shop reply to a customer review
ALTER TABLE product_reviews ADD COLUMN seller_reply TEXT;
ALTER TABLE product_reviews ADD COLUMN seller_reply_at TIMESTAMP;

-- G3/G5: record when an order was delivered (drives customer "confirm received" + review window).
-- Backfill existing delivered orders using their last update time so historical reviews stay valid.
ALTER TABLE orders ADD COLUMN delivered_at TIMESTAMP;
UPDATE orders SET delivered_at = updated_at WHERE status = 'DELIVERED' AND delivered_at IS NULL;
