-- Add PayGate session tracking fields to orders table
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS paygate_token VARCHAR(100),
    ADD COLUMN IF NOT EXISTS paygate_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS paygate_expires_at TIMESTAMP;
