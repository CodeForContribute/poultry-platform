-- V15: Create simplified deliveries table for the new delivery service
-- This complements the existing delivery_tracking table with a simpler model

-- Extended delivery status enum (more granular than the original)
CREATE TYPE delivery_status_v2 AS ENUM (
    'PENDING_ASSIGNMENT',
    'ASSIGNED',
    'DISPATCHED',
    'IN_TRANSIT',
    'ARRIVED',
    'DELIVERED',
    'FAILED',
    'RETURNED'
);

-- Deliveries table (simplified delivery tracking)
CREATE TABLE deliveries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    agent_id UUID REFERENCES delivery_agents(id),

    -- Status
    status delivery_status_v2 NOT NULL DEFAULT 'PENDING_ASSIGNMENT',

    -- OTP verification
    delivery_otp VARCHAR(6),
    otp_generated_at TIMESTAMPTZ,
    otp_verified_at TIMESTAMPTZ,

    -- Proof of delivery
    photo_proof_url VARCHAR(500),

    -- Location tracking
    current_location JSONB,

    -- SLA
    sla_deadline TIMESTAMPTZ,

    -- Timestamps for state transitions
    dispatched_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,

    -- Failure handling
    failure_reason VARCHAR(500),
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 2,

    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_deliveries_order ON deliveries(order_id);
CREATE INDEX idx_deliveries_agent ON deliveries(agent_id);
CREATE INDEX idx_deliveries_status ON deliveries(status);
CREATE INDEX idx_deliveries_pending ON deliveries(status, created_at)
    WHERE status IN ('PENDING_ASSIGNMENT', 'ASSIGNED');
CREATE INDEX idx_deliveries_in_progress ON deliveries(agent_id, status)
    WHERE status IN ('DISPATCHED', 'IN_TRANSIT', 'ARRIVED');
CREATE INDEX idx_deliveries_sla ON deliveries(sla_deadline)
    WHERE status NOT IN ('DELIVERED', 'FAILED', 'RETURNED') AND sla_deadline IS NOT NULL;

-- Trigger to update updated_at
CREATE TRIGGER update_deliveries_updated_at
    BEFORE UPDATE ON deliveries
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Function to generate OTP when status changes to ARRIVED
CREATE OR REPLACE FUNCTION generate_delivery_otp_v2()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'ARRIVED' AND (OLD.status IS NULL OR OLD.status != 'ARRIVED') THEN
        NEW.delivery_otp := LPAD(FLOOR(RANDOM() * 1000000)::TEXT, 6, '0');
        NEW.otp_generated_at := NOW();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER generate_otp_on_arrived
    BEFORE INSERT OR UPDATE ON deliveries
    FOR EACH ROW
    EXECUTE FUNCTION generate_delivery_otp_v2();

-- Function to update agent stats on delivery completion
CREATE OR REPLACE FUNCTION update_agent_delivery_stats()
RETURNS TRIGGER AS $$
BEGIN
    -- On successful delivery
    IF NEW.status = 'DELIVERED' AND OLD.status != 'DELIVERED' THEN
        UPDATE delivery_agents
        SET total_deliveries = total_deliveries + 1,
            successful_deliveries = successful_deliveries + 1,
            updated_at = NOW()
        WHERE id = NEW.agent_id;
    -- On failed delivery
    ELSIF NEW.status = 'FAILED' AND OLD.status NOT IN ('FAILED', 'DELIVERED') THEN
        UPDATE delivery_agents
        SET total_deliveries = total_deliveries + 1,
            updated_at = NOW()
        WHERE id = NEW.agent_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_agent_stats_on_delivery
    AFTER UPDATE ON deliveries
    FOR EACH ROW
    EXECUTE FUNCTION update_agent_delivery_stats();

-- Delivery status history
CREATE TABLE delivery_status_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    delivery_id UUID NOT NULL REFERENCES deliveries(id) ON DELETE CASCADE,

    from_status delivery_status_v2,
    to_status delivery_status_v2 NOT NULL,

    location JSONB,
    notes TEXT,

    changed_by_type VARCHAR(20) NOT NULL, -- 'AGENT', 'SELLER', 'ADMIN', 'SYSTEM'
    changed_by_id UUID,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_delivery_status_history_delivery ON delivery_status_history(delivery_id);

-- Trigger to log status changes
CREATE OR REPLACE FUNCTION log_delivery_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO delivery_status_history (
            delivery_id, from_status, to_status, location, changed_by_type
        ) VALUES (
            NEW.id, OLD.status, NEW.status, NEW.current_location, 'SYSTEM'
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER log_delivery_status
    AFTER UPDATE ON deliveries
    FOR EACH ROW
    EXECUTE FUNCTION log_delivery_status_change();

-- Comments
COMMENT ON TABLE deliveries IS 'Simplified delivery tracking for orders';
COMMENT ON COLUMN deliveries.delivery_otp IS '6-digit OTP for delivery verification, auto-generated when status becomes ARRIVED';
COMMENT ON COLUMN deliveries.current_location IS 'Last known location of delivery agent as {lat, lng, updated_at}';
