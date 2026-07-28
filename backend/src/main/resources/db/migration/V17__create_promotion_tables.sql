-- V9: promotion codes (coupons) + scope + redemption ledger. Money = DECIMAL(12,2).
-- FEATURE-STP-02 (Week 1 — Promotion Rules & Coupon Engine).
CREATE TABLE promotion_codes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,               -- stored normalized UPPER-CASE
    discount_type VARCHAR(10) NOT NULL,             -- PERCENT | FIXED
    discount_value DECIMAL(12,2) NOT NULL,          -- percent (1..100) when PERCENT; amount when FIXED
    max_discount DECIMAL(12,2),                     -- cap for PERCENT; null = no cap
    min_order_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    scope_type VARCHAR(10) NOT NULL DEFAULT 'CART', -- CART | PRODUCT | CATEGORY
    usage_limit    INT,                             -- null = unlimited total
    per_user_limit INT,                             -- null = unlimited per user
    used_count INT NOT NULL DEFAULT 0,
    starts_at  TIMESTAMP,
    expires_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_promo_type  CHECK (discount_type IN ('PERCENT','FIXED')),
    CONSTRAINT chk_promo_scope CHECK (scope_type IN ('CART','PRODUCT','CATEGORY')),
    CONSTRAINT chk_promo_value CHECK (discount_value > 0),
    CONSTRAINT chk_promo_percent_range CHECK (discount_type <> 'PERCENT' OR discount_value <= 100)
);

-- Extensible scope structure (B-303): one row per product-id or category-id in the coupon's scope.
CREATE TABLE promotion_code_scopes (
    id BIGSERIAL PRIMARY KEY,
    promotion_code_id BIGINT NOT NULL REFERENCES promotion_codes(id),
    ref_type VARCHAR(10) NOT NULL,                  -- PRODUCT | CATEGORY
    ref_id   BIGINT NOT NULL,
    CONSTRAINT uk_promo_scope UNIQUE (promotion_code_id, ref_type, ref_id),
    CONSTRAINT chk_promo_scope_ref CHECK (ref_type IN ('PRODUCT','CATEGORY'))
);

-- Redemption ledger (B-302 per-user limit, B-308 atomic usage). One coupon per order (uk by order_id).
CREATE TABLE promotion_redemptions (
    id BIGSERIAL PRIMARY KEY,
    promotion_code_id BIGINT NOT NULL REFERENCES promotion_codes(id),
    user_id  BIGINT NOT NULL REFERENCES users(id),
    order_id BIGINT NOT NULL REFERENCES orders(id),
    discount_amount DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_promo_redemption_order UNIQUE (order_id)
);

ALTER TABLE orders
    ADD COLUMN promotion_code_id BIGINT REFERENCES promotion_codes(id),
    ADD COLUMN coupon_code       VARCHAR(30),                         -- snapshot of the applied code
    ADD COLUMN discount_amount   DECIMAL(12,2) NOT NULL DEFAULT 0;

CREATE INDEX idx_promo_scope_code      ON promotion_code_scopes(promotion_code_id);
CREATE INDEX idx_promo_redemption_pair ON promotion_redemptions(promotion_code_id, user_id);
