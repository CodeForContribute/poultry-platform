-- V19: Create disputes and resolution tables

CREATE TYPE dispute_type AS ENUM ('QUALITY_ISSUE', 'QUANTITY_MISMATCH', 'DELIVERY_ISSUE', 'PAYMENT_ISSUE', 'WRONG_PRODUCT', 'DAMAGED_GOODS', 'OTHER');
CREATE TYPE dispute_status AS ENUM ('OPEN', 'UNDER_REVIEW', 'AWAITING_RESPONSE', 'RESOLVED', 'ESCALATED', 'CLOSED');
CREATE TYPE dispute_resolution AS ENUM ('REFUND_FULL', 'REFUND_PARTIAL', 'REPLACEMENT', 'CREDIT_NOTE', 'NO_ACTION', 'MUTUAL_AGREEMENT');
CREATE TYPE dispute_raised_by AS ENUM ('BUYER', 'SELLER');

CREATE TABLE IF NOT EXISTS disputes
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    dispute_number VARCHAR
(
    50
) NOT NULL UNIQUE,
    order_id UUID NOT NULL REFERENCES orders
(
    id
),
    buyer_id UUID NOT NULL REFERENCES buyers
(
    id
),
    seller_id UUID NOT NULL REFERENCES sellers
(
    id
),
    raised_by dispute_raised_by NOT NULL,
    type dispute_type NOT NULL,
    status dispute_status NOT NULL DEFAULT 'OPEN',
    title VARCHAR
(
    200
) NOT NULL,
    description TEXT NOT NULL,
    evidence_urls JSONB DEFAULT '[]',
    requested_resolution dispute_resolution,
    requested_amount DECIMAL
(
    12,
    2
),
    final_resolution dispute_resolution,
    resolution_amount DECIMAL
(
    12,
    2
),
    resolution_notes TEXT,
    assigned_to UUID REFERENCES admin_users
(
    id
),
    priority INT NOT NULL DEFAULT 2,
    escalated_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    resolved_by UUID REFERENCES admin_users
(
    id
),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
)
    );

CREATE TABLE IF NOT EXISTS dispute_messages
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    dispute_id UUID NOT NULL REFERENCES disputes
(
    id
) ON DELETE CASCADE,
    sender_type VARCHAR
(
    20
) NOT NULL,
    sender_id UUID NOT NULL,
    message TEXT NOT NULL,
    attachment_urls JSONB DEFAULT '[]',
    is_internal BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
)
    );

CREATE TABLE IF NOT EXISTS dispute_history
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    dispute_id UUID NOT NULL REFERENCES disputes
(
    id
) ON DELETE CASCADE,
    action VARCHAR
(
    100
) NOT NULL,
    old_status dispute_status,
    new_status dispute_status,
    performed_by UUID,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
)
    );

CREATE INDEX idx_disputes_order ON disputes (order_id);
CREATE INDEX idx_disputes_buyer ON disputes (buyer_id, created_at DESC);
CREATE INDEX idx_disputes_seller ON disputes (seller_id, created_at DESC);
CREATE INDEX idx_disputes_status ON disputes (status) WHERE status IN ('OPEN', 'UNDER_REVIEW', 'ESCALATED');
CREATE INDEX idx_disputes_assigned ON disputes (assigned_to) WHERE status NOT IN ('RESOLVED', 'CLOSED');
CREATE INDEX idx_dispute_messages_dispute ON dispute_messages (dispute_id, created_at);

CREATE TRIGGER update_disputes_updated_at
    BEFORE UPDATE
    ON disputes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Sequence for dispute numbers
CREATE SEQUENCE IF NOT EXISTS dispute_number_seq START 10001;
