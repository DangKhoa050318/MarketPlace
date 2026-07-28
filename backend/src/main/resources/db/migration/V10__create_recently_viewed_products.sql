CREATE TABLE recently_viewed_products (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    session_id VARCHAR(128),
    product_id BIGINT NOT NULL,
    viewed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_recently_viewed_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_recently_viewed_product
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT chk_recently_viewed_owner
        CHECK (
            (user_id IS NOT NULL AND session_id IS NULL)
            OR (user_id IS NULL AND session_id IS NOT NULL)
        )
);

CREATE UNIQUE INDEX uk_recently_viewed_user_product
    ON recently_viewed_products (user_id, product_id)
    WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX uk_recently_viewed_session_product
    ON recently_viewed_products (session_id, product_id)
    WHERE session_id IS NOT NULL;

CREATE INDEX idx_recently_viewed_user_viewed_at
    ON recently_viewed_products (user_id, viewed_at DESC)
    WHERE user_id IS NOT NULL;

CREATE INDEX idx_recently_viewed_session_viewed_at
    ON recently_viewed_products (session_id, viewed_at DESC)
    WHERE session_id IS NOT NULL;
