-- V12: Create file uploads table (MinIO references)

CREATE TABLE file_uploads (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- MinIO storage info
    bucket VARCHAR(100) NOT NULL, -- 'receipts', 'delivery-photos', 'reconciliation-reports', 'products'
    object_key VARCHAR(500) NOT NULL,

    -- File metadata
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum VARCHAR(64), -- MD5 or SHA256

    -- Uploader info
    uploaded_by_type VARCHAR(20), -- 'SELLER_USER', 'BUYER', 'ADMIN', 'SYSTEM'
    uploaded_by_id UUID,

    -- Reference to related entity
    reference_type VARCHAR(50) NOT NULL, -- 'ORDER_RECEIPT', 'DELIVERY_PROOF', 'PRODUCT_IMAGE', 'RECON_REPORT'
    reference_id UUID NOT NULL,

    -- Access control
    is_public BOOLEAN NOT NULL DEFAULT FALSE,

    -- Expiry (for temporary files)
    expires_at TIMESTAMPTZ,

    -- Soft delete
    deleted_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_file_uploads_bucket ON file_uploads(bucket);
CREATE INDEX idx_file_uploads_reference ON file_uploads(reference_type, reference_id);
CREATE INDEX idx_file_uploads_uploader ON file_uploads(uploaded_by_type, uploaded_by_id);
CREATE INDEX idx_file_uploads_expires ON file_uploads(expires_at) WHERE expires_at IS NOT NULL;

-- Create unique index for object_key within bucket
CREATE UNIQUE INDEX idx_file_uploads_bucket_key ON file_uploads(bucket, object_key)
    WHERE deleted_at IS NULL;
