ALTER TABLE return_requests
    ADD COLUMN IF NOT EXISTS evidence_image_urls TEXT;
