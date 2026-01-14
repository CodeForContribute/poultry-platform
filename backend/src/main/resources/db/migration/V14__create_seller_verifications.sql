-- V14: Create seller verifications table for KYC/FSSAI/Bank verification workflow

-- Verification type enum
CREATE TYPE verification_type AS ENUM ('KYC', 'BANK', 'FSSAI');

-- Verification status enum
CREATE TYPE verification_status AS ENUM (
    'PENDING',
    'UNDER_REVIEW',
    'APPROVED',
    'REJECTED',
    'EXPIRED'
);

-- Seller verifications table
CREATE TABLE seller_verifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    seller_id UUID NOT NULL REFERENCES sellers(id) ON DELETE CASCADE,

    -- Verification details
    verification_type verification_type NOT NULL,
    status verification_status NOT NULL DEFAULT 'PENDING',

    -- Documents (references to file_uploads)
    document_ids JSONB DEFAULT '[]'::JSONB,

    -- Review information
    remarks VARCHAR(1000),
    rejection_reason VARCHAR(1000),

    -- Verified by admin
    verified_by UUID REFERENCES admin_users(id),
    verified_at TIMESTAMPTZ,

    -- Expiry (for time-bound verifications like FSSAI license)
    expires_at TIMESTAMPTZ,

    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Ensure one verification per type per seller (only latest matters)
    CONSTRAINT uq_seller_verification_type UNIQUE (seller_id, verification_type)
);

-- Indexes
CREATE INDEX idx_seller_verifications_seller ON seller_verifications(seller_id);
CREATE INDEX idx_seller_verifications_status ON seller_verifications(status);
CREATE INDEX idx_seller_verifications_pending ON seller_verifications(status, created_at)
    WHERE status IN ('PENDING', 'UNDER_REVIEW');
CREATE INDEX idx_seller_verifications_expiring ON seller_verifications(expires_at)
    WHERE status = 'APPROVED' AND expires_at IS NOT NULL;

-- Verification history table (audit trail)
CREATE TABLE seller_verification_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    verification_id UUID NOT NULL REFERENCES seller_verifications(id) ON DELETE CASCADE,

    -- Status change
    from_status verification_status,
    to_status verification_status NOT NULL,

    -- Actor
    changed_by UUID, -- admin_user_id
    changed_by_type VARCHAR(20) NOT NULL, -- 'SELLER', 'ADMIN', 'SYSTEM'

    -- Details
    remarks TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_verification_history_verification ON seller_verification_history(verification_id);

-- Trigger to update updated_at
CREATE TRIGGER update_seller_verifications_updated_at
    BEFORE UPDATE ON seller_verifications
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger to log status changes
CREATE OR REPLACE FUNCTION log_verification_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO seller_verification_history (
            verification_id, from_status, to_status, changed_by, changed_by_type, remarks
        ) VALUES (
            NEW.id, OLD.status, NEW.status, NEW.verified_by, 'ADMIN', NEW.remarks
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER log_verification_status
    AFTER UPDATE ON seller_verifications
    FOR EACH ROW
    EXECUTE FUNCTION log_verification_status_change();

-- Function to check if seller is fully verified
CREATE OR REPLACE FUNCTION is_seller_fully_verified(p_seller_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    kyc_approved BOOLEAN;
    bank_approved BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1 FROM seller_verifications
        WHERE seller_id = p_seller_id
        AND verification_type = 'KYC'
        AND status = 'APPROVED'
    ) INTO kyc_approved;

    SELECT EXISTS (
        SELECT 1 FROM seller_verifications
        WHERE seller_id = p_seller_id
        AND verification_type = 'BANK'
        AND status = 'APPROVED'
    ) INTO bank_approved;

    RETURN kyc_approved AND bank_approved;
END;
$$ LANGUAGE plpgsql;

-- Comment
COMMENT ON TABLE seller_verifications IS 'Tracks KYC, Bank, and FSSAI verifications for sellers';
COMMENT ON COLUMN seller_verifications.document_ids IS 'Array of file_uploads IDs containing verification documents';
