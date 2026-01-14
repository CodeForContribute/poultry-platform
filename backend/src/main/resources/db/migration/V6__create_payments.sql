-- V6: Create payments table

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id),

    -- Gateway info
    gateway payment_gateway NOT NULL,
    gateway_order_id VARCHAR(100), -- Razorpay order_id
    gateway_payment_id VARCHAR(100) UNIQUE, -- Razorpay payment_id (for dedup)
    gateway_signature VARCHAR(255),

    -- Amount
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',

    -- Status
    status payment_status NOT NULL DEFAULT 'CREATED',

    -- Webhook data
    webhook_payload JSONB,
    webhook_received_at TIMESTAMPTZ,
    signature_verified BOOLEAN DEFAULT FALSE,

    -- Error handling
    error_code VARCHAR(50),
    error_description TEXT,

    -- Refund tracking
    refund_id VARCHAR(100),
    refund_amount DECIMAL(12, 2),
    refund_status VARCHAR(50),
    refunded_at TIMESTAMPTZ,

    -- Timestamps
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Payment attempts table (track all attempts including failures)
CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    attempt_number INT NOT NULL,

    -- Method used
    method VARCHAR(50), -- 'upi', 'card', 'netbanking', 'wallet'
    method_details JSONB, -- Bank name, card last 4, etc.

    -- Result
    status VARCHAR(50) NOT NULL,
    error_code VARCHAR(50),
    error_description TEXT,

    -- Gateway response
    gateway_response JSONB,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_gateway_order_id ON payments(gateway_order_id);
CREATE INDEX idx_payments_gateway_payment_id ON payments(gateway_payment_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at DESC);

-- For polling fallback - find payments that need status check
CREATE INDEX idx_payments_pending_check ON payments(created_at)
    WHERE status = 'PENDING' AND gateway_payment_id IS NULL;

CREATE INDEX idx_payment_attempts_payment_id ON payment_attempts(payment_id);

-- Triggers
CREATE TRIGGER update_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
