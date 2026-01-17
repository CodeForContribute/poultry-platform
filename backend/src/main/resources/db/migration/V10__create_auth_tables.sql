-- V10: Create authentication and audit tables

-- Refresh tokens table
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- User reference (polymorphic)
    user_type VARCHAR(20) NOT NULL, -- 'SELLER_USER', 'BUYER', 'ADMIN'
    user_id UUID NOT NULL,

    -- Token
    token_hash VARCHAR(255) NOT NULL UNIQUE,

    -- Device binding
    device_id VARCHAR(255),
    device_info JSONB,
    ip_address INET,

    -- Validity
    expires_at TIMESTAMPTZ NOT NULL,

    -- Revocation
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMPTZ,
    revoke_reason VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- OTP requests table (for rate limiting)
CREATE TABLE otp_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    phone_hash VARCHAR(64) NOT NULL, -- Hashed phone for lookups
    phone_encrypted BYTEA NOT NULL, -- Encrypted for audit

    otp_hash VARCHAR(255) NOT NULL, -- Hashed OTP
    purpose otp_purpose NOT NULL,

    -- Rate limiting
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,

    -- Validity
    expires_at TIMESTAMPTZ NOT NULL,

    -- Verification
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMPTZ,

    -- Metadata
    ip_address INET,
    device_info JSONB,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Audit logs table (comprehensive logging)
-- Note: PRIMARY KEY must include partition column (created_at) for partitioned tables
CREATE TABLE audit_logs (
    id UUID DEFAULT uuid_generate_v4(),

    -- Actor
    user_type VARCHAR(20), -- 'SELLER_USER', 'BUYER', 'ADMIN', 'SYSTEM'
    user_id UUID,

    -- Action
    action audit_action NOT NULL,

    -- Target resource
    resource_type VARCHAR(50), -- 'ORDER', 'PAYMENT', 'PRODUCT', etc.
    resource_id UUID,

    -- Request context
    ip_address INET,
    user_agent TEXT,
    device_info JSONB,

    -- Result
    outcome VARCHAR(20) NOT NULL, -- 'SUCCESS', 'FAILURE', 'BLOCKED'
    failure_reason TEXT,

    -- Details
    details JSONB,

    -- Request tracing
    correlation_id VARCHAR(64),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Create partitions for audit_logs (monthly)
CREATE TABLE audit_logs_2024_01 PARTITION OF audit_logs
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
CREATE TABLE audit_logs_2024_02 PARTITION OF audit_logs
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
CREATE TABLE audit_logs_2024_03 PARTITION OF audit_logs
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');
CREATE TABLE audit_logs_2024_04 PARTITION OF audit_logs
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');
CREATE TABLE audit_logs_2024_05 PARTITION OF audit_logs
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');
CREATE TABLE audit_logs_2024_06 PARTITION OF audit_logs
    FOR VALUES FROM ('2024-06-01') TO ('2024-07-01');
CREATE TABLE audit_logs_2024_07 PARTITION OF audit_logs
    FOR VALUES FROM ('2024-07-01') TO ('2024-08-01');
CREATE TABLE audit_logs_2024_08 PARTITION OF audit_logs
    FOR VALUES FROM ('2024-08-01') TO ('2024-09-01');
CREATE TABLE audit_logs_2024_09 PARTITION OF audit_logs
    FOR VALUES FROM ('2024-09-01') TO ('2024-10-01');
CREATE TABLE audit_logs_2024_10 PARTITION OF audit_logs
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');
CREATE TABLE audit_logs_2024_11 PARTITION OF audit_logs
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');
CREATE TABLE audit_logs_2024_12 PARTITION OF audit_logs
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

-- 2025 partitions
CREATE TABLE audit_logs_2025_01 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
CREATE TABLE audit_logs_2025_02 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
CREATE TABLE audit_logs_2025_03 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');
CREATE TABLE audit_logs_2025_04 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');
CREATE TABLE audit_logs_2025_05 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');
CREATE TABLE audit_logs_2025_06 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');
CREATE TABLE audit_logs_2025_07 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');
CREATE TABLE audit_logs_2025_08 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');
CREATE TABLE audit_logs_2025_09 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');
CREATE TABLE audit_logs_2025_10 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');
CREATE TABLE audit_logs_2025_11 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');
CREATE TABLE audit_logs_2025_12 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

-- 2026 partitions
CREATE TABLE audit_logs_2026_01 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE audit_logs_2026_02 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');

-- Idempotency keys table (for POST request deduplication)
CREATE TABLE idempotency_keys (
    key VARCHAR(64) PRIMARY KEY,

    endpoint VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL DEFAULT 'POST',

    -- Stored response
    response_status INT,
    response_body JSONB,
    response_headers JSONB,

    -- Validity (24h window)
    expires_at TIMESTAMPTZ NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_type, user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_active ON refresh_tokens(user_type, user_id, expires_at)
    WHERE revoked = FALSE;

CREATE INDEX idx_otp_requests_phone ON otp_requests(phone_hash);
CREATE INDEX idx_otp_requests_rate_limit ON otp_requests(phone_hash, created_at DESC);
CREATE INDEX idx_otp_requests_pending ON otp_requests(phone_hash, expires_at)
    WHERE verified = FALSE;

CREATE INDEX idx_audit_logs_user ON audit_logs(user_type, user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action, created_at DESC);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_logs_correlation ON audit_logs(correlation_id);

CREATE INDEX idx_idempotency_keys_expires ON idempotency_keys(expires_at);

-- Function to clean up expired idempotency keys
CREATE OR REPLACE FUNCTION cleanup_expired_idempotency_keys()
RETURNS void AS $$
BEGIN
    DELETE FROM idempotency_keys WHERE expires_at < NOW();
END;
$$ LANGUAGE plpgsql;

-- Function to check OTP rate limit (5 per hour)
CREATE OR REPLACE FUNCTION check_otp_rate_limit(p_phone_hash VARCHAR)
RETURNS BOOLEAN AS $$
DECLARE
    request_count INT;
BEGIN
    SELECT COUNT(*) INTO request_count
    FROM otp_requests
    WHERE phone_hash = p_phone_hash
      AND created_at > NOW() - INTERVAL '1 hour';

    RETURN request_count < 5;
END;
$$ LANGUAGE plpgsql;
