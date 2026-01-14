-- V8: Create delivery and tracking tables

-- Delivery agents table
CREATE TABLE delivery_agents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    seller_id UUID NOT NULL REFERENCES sellers(id) ON DELETE CASCADE,

    name VARCHAR(255) NOT NULL,
    phone_encrypted BYTEA NOT NULL,
    phone_hash VARCHAR(64) NOT NULL, -- For lookups

    vehicle_type VARCHAR(50), -- 'bike', 'truck', 'tempo'
    vehicle_number VARCHAR(20),

    -- Current status
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    current_location JSONB, -- {lat, lng, updated_at}

    -- Stats
    total_deliveries INT NOT NULL DEFAULT 0,
    successful_deliveries INT NOT NULL DEFAULT 0,

    status seller_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Delivery tracking table
CREATE TABLE delivery_tracking (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL UNIQUE REFERENCES orders(id),
    agent_id UUID REFERENCES delivery_agents(id),

    -- Status
    status delivery_status NOT NULL DEFAULT 'ASSIGNED',

    -- Assignment
    assigned_at TIMESTAMPTZ,
    assigned_by UUID, -- seller_user_id or NULL for auto-assignment

    -- Milestones
    picked_up_at TIMESTAMPTZ,
    in_transit_at TIMESTAMPTZ,
    out_for_delivery_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,

    -- Delivery confirmation
    otp_code VARCHAR(6),
    otp_generated_at TIMESTAMPTZ,
    otp_verified BOOLEAN DEFAULT FALSE,

    -- For failed deliveries
    failure_reason delivery_failure_reason,
    failure_notes TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 2,
    next_retry_date DATE,

    -- SLA tracking
    promised_delivery_time TIMESTAMPTZ,
    sla_breached BOOLEAN DEFAULT FALSE,

    -- Location tracking
    last_known_location JSONB,
    location_history JSONB DEFAULT '[]'::JSONB,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Delivery proof table (OTP or photo)
CREATE TABLE delivery_proof (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tracking_id UUID NOT NULL REFERENCES delivery_tracking(id) ON DELETE CASCADE,

    proof_type delivery_proof_type NOT NULL,

    -- For OTP proof
    otp_entered VARCHAR(6),
    otp_verified BOOLEAN,

    -- For photo proof
    photo_url VARCHAR(500),
    photo_taken_at TIMESTAMPTZ,
    photo_location JSONB, -- {lat, lng}

    -- Verification
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMPTZ,
    verified_by VARCHAR(50), -- 'SYSTEM', 'BUYER', 'ADMIN'

    notes TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Delivery status history
CREATE TABLE delivery_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tracking_id UUID NOT NULL REFERENCES delivery_tracking(id) ON DELETE CASCADE,

    from_status delivery_status,
    to_status delivery_status NOT NULL,

    changed_by_type VARCHAR(20) NOT NULL, -- 'AGENT', 'SELLER', 'ADMIN', 'SYSTEM'
    changed_by_id UUID,

    location JSONB,
    notes TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_delivery_agents_seller_id ON delivery_agents(seller_id);
CREATE INDEX idx_delivery_agents_phone_hash ON delivery_agents(phone_hash);
CREATE INDEX idx_delivery_agents_available ON delivery_agents(seller_id, is_available)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_delivery_tracking_order_id ON delivery_tracking(order_id);
CREATE INDEX idx_delivery_tracking_agent_id ON delivery_tracking(agent_id);
CREATE INDEX idx_delivery_tracking_status ON delivery_tracking(status);
CREATE INDEX idx_delivery_tracking_sla ON delivery_tracking(promised_delivery_time)
    WHERE status NOT IN ('DELIVERED', 'DELIVERY_FAILED') AND sla_breached = FALSE;

CREATE INDEX idx_delivery_proof_tracking_id ON delivery_proof(tracking_id);

CREATE INDEX idx_delivery_history_tracking_id ON delivery_history(tracking_id);

-- Triggers
CREATE TRIGGER update_delivery_agents_updated_at
    BEFORE UPDATE ON delivery_agents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_delivery_tracking_updated_at
    BEFORE UPDATE ON delivery_tracking
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Function to generate OTP for delivery
CREATE OR REPLACE FUNCTION generate_delivery_otp()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'OUT_FOR_DELIVERY' AND OLD.status != 'OUT_FOR_DELIVERY' THEN
        NEW.otp_code := LPAD(FLOOR(RANDOM() * 1000000)::TEXT, 6, '0');
        NEW.otp_generated_at := NOW();
        NEW.otp_verified := FALSE;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER generate_otp_on_out_for_delivery
    BEFORE UPDATE ON delivery_tracking
    FOR EACH ROW
    EXECUTE FUNCTION generate_delivery_otp();
