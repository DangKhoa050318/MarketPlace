-- FEATURE-STP-02 (Week 2 — Campaigns, Collections & Merchandising).
-- Self-contained: no reuse of analytics_events (avoids coupling with STP-04).

-- B-401: Campaign — a scheduled marketing container that may surface (not auto-apply) a coupon.
CREATE TABLE campaigns (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(12) NOT NULL DEFAULT 'DRAFT',          -- DRAFT | PUBLISHED | ARCHIVED
    starts_at TIMESTAMP,
    ends_at   TIMESTAMP,
    promotion_code_id BIGINT REFERENCES promotion_codes(id),  -- nullable: 0..1 coupon (D-1)
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_campaign_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    CONSTRAINT chk_campaign_window CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at >= starts_at)
);

-- B-402: ProductCollection — manual, ordered, publishable product list.
CREATE TABLE product_collections (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(160) NOT NULL UNIQUE,
    description TEXT,
    status VARCHAR(12) NOT NULL DEFAULT 'DRAFT',          -- DRAFT | PUBLISHED
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_collection_status CHECK (status IN ('DRAFT','PUBLISHED'))
);

-- B-406: items with a UNIQUE display order per collection. The order constraint is
-- DEFERRABLE so a full reorder can rewrite every row inside one transaction (dup
-- positions are tolerated mid-transaction, checked at COMMIT).
CREATE TABLE product_collection_items (
    id BIGSERIAL PRIMARY KEY,
    collection_id BIGINT NOT NULL REFERENCES product_collections(id) ON DELETE CASCADE,
    product_id    BIGINT NOT NULL REFERENCES products(id),
    display_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_collection_product UNIQUE (collection_id, product_id),
    CONSTRAINT uk_collection_order   UNIQUE (collection_id, display_order)
        DEFERRABLE INITIALLY DEFERRED
);

-- B-403: Merchandising banner with desktop/mobile art, alt text, target, slot, schedule, status.
CREATE TABLE merchandising_banners (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    image_url_desktop VARCHAR(500) NOT NULL,
    image_url_mobile  VARCHAR(500),
    alt_text VARCHAR(255) NOT NULL,
    target_url VARCHAR(500),
    position VARCHAR(40) NOT NULL,                        -- slot key, e.g. HOME_HERO
    status VARCHAR(12) NOT NULL DEFAULT 'DRAFT',          -- DRAFT | PUBLISHED
    display_order INT NOT NULL DEFAULT 0,
    starts_at TIMESTAMP,
    ends_at   TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_banner_status CHECK (status IN ('DRAFT','PUBLISHED')),
    CONSTRAINT chk_banner_window CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at >= starts_at)
);

-- B-407/408: append-only impression/click log with client-supplied event_id for
-- idempotent dedup; order_id set when an event is attributed to a purchase (D-5).
CREATE TABLE merchandising_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    event_type  VARCHAR(20) NOT NULL,                    -- IMPRESSION | CLICK
    target_type VARCHAR(12) NOT NULL,                    -- CAMPAIGN | COLLECTION | BANNER
    target_id   BIGINT NOT NULL,
    user_id    BIGINT REFERENCES users(id)  ON DELETE SET NULL,
    session_id VARCHAR(128),
    order_id   BIGINT REFERENCES orders(id) ON DELETE SET NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT NOW(),
    received_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_merch_event_type  CHECK (event_type IN ('IMPRESSION','CLICK')),
    CONSTRAINT chk_merch_target_type CHECK (target_type IN ('CAMPAIGN','COLLECTION','BANNER'))
);

CREATE INDEX idx_campaign_status_window   ON campaigns(status, starts_at, ends_at);
CREATE INDEX idx_collection_item_order    ON product_collection_items(collection_id, display_order);
CREATE INDEX idx_banner_position_status   ON merchandising_banners(position, status, display_order);
CREATE INDEX idx_merch_event_target       ON merchandising_events(target_type, target_id, event_type, occurred_at);
CREATE INDEX idx_merch_event_order        ON merchandising_events(order_id);