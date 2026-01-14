-- V3: Create buyers table

CREATE TABLE buyers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone_encrypted BYTEA NOT NULL,
    phone_hash VARCHAR(64) NOT NULL UNIQUE, -- For lookups without decryption
    name VARCHAR(255),
    business_name VARCHAR(255),
    gstin VARCHAR(15),
    addresses JSONB DEFAULT '[]'::JSONB,
    default_address_index INT DEFAULT 0,
    device_id VARCHAR(255),
    fcm_token VARCHAR(500),
    preferred_language VARCHAR(10) DEFAULT 'en',
    status buyer_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Buyer sessions table (max 2 active sessions)
CREATE TABLE buyer_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    buyer_id UUID NOT NULL REFERENCES buyers(id) ON DELETE CASCADE,
    device_id VARCHAR(255) NOT NULL,
    device_fingerprint VARCHAR(255),
    device_info JSONB,
    ip_address INET,
    refresh_token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMPTZ,
    revoke_reason VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_active_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_buyers_phone_hash ON buyers(phone_hash);
CREATE INDEX idx_buyers_status ON buyers(status);
CREATE INDEX idx_buyer_sessions_buyer_id ON buyer_sessions(buyer_id);
CREATE INDEX idx_buyer_sessions_device_id ON buyer_sessions(device_id);
CREATE INDEX idx_buyer_sessions_active ON buyer_sessions(buyer_id, revoked) WHERE revoked = FALSE;

-- Trigger
CREATE TRIGGER update_buyers_updated_at
    BEFORE UPDATE ON buyers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
