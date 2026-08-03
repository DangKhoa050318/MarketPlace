CREATE TABLE analytics_export_jobs (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    export_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    from_time TIMESTAMPTZ NOT NULL,
    to_time TIMESTAMPTZ NOT NULL,
    category_id BIGINT,
    product_id BIGINT,
    campaign VARCHAR(120),
    placement VARCHAR(80),
    device_type VARCHAR(40),
    file_name VARCHAR(180),
    csv_content TEXT,
    error_message VARCHAR(500),
    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_analytics_export_category
        FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_analytics_export_product
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
);

CREATE INDEX idx_analytics_export_status_created
    ON analytics_export_jobs(status, created_at DESC);

CREATE INDEX idx_analytics_export_expires
    ON analytics_export_jobs(expires_at)
    WHERE expires_at IS NOT NULL;
