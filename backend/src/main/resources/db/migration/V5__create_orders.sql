-- V5: Create orders and order_items tables

-- Orders table
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_number VARCHAR(20) NOT NULL UNIQUE, -- Human readable: ORD-YYYYMMDD-XXXXX
    buyer_id UUID NOT NULL REFERENCES buyers(id),
    seller_id UUID NOT NULL REFERENCES sellers(id),
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    type order_type NOT NULL DEFAULT 'INSTANT',
    status order_status NOT NULL DEFAULT 'DRAFT',

    -- Amounts
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    gst_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    delivery_charge DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    platform_fee DECIMAL(12, 2) NOT NULL DEFAULT 0,

    -- Delivery info
    delivery_date DATE,
    delivery_slot VARCHAR(50), -- e.g., "6AM-9AM", "9AM-12PM"
    delivery_address JSONB NOT NULL,
    delivery_instructions TEXT,

    -- Price snapshot at order time (immutable)
    price_snapshot JSONB NOT NULL DEFAULT '{}'::JSONB,

    -- Optimistic locking
    version INT NOT NULL DEFAULT 1,

    -- Expiry for payment timeout
    expires_at TIMESTAMPTZ,

    -- Cancellation/rejection details
    cancelled_at TIMESTAMPTZ,
    cancelled_by VARCHAR(50), -- 'BUYER', 'SELLER', 'SYSTEM'
    cancellation_reason TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Order items table
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),

    -- Snapshot of product at order time
    product_snapshot JSONB NOT NULL,

    quantity DECIMAL(10, 3) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    discount_percent DECIMAL(5, 2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    gst_percent DECIMAL(5, 2) NOT NULL DEFAULT 0,
    gst_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    line_total DECIMAL(12, 2) NOT NULL,

    -- For partial delivery
    delivered_quantity DECIMAL(10, 3) DEFAULT 0,
    status order_item_status NOT NULL DEFAULT 'PENDING',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Order status history (audit trail)
CREATE TABLE order_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    from_status order_status,
    to_status order_status NOT NULL,
    changed_by_type VARCHAR(20) NOT NULL, -- 'BUYER', 'SELLER', 'ADMIN', 'SYSTEM'
    changed_by_id UUID,
    reason TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_orders_buyer_id ON orders(buyer_id);
CREATE INDEX idx_orders_seller_id ON orders(seller_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_buyer_status ON orders(buyer_id, status);
CREATE INDEX idx_orders_seller_status ON orders(seller_id, status, created_at DESC);
CREATE INDEX idx_orders_idempotency ON orders(idempotency_key);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX idx_orders_delivery_date ON orders(delivery_date) WHERE delivery_date IS NOT NULL;

-- Partial index for active orders (payment pending expiry check)
CREATE INDEX idx_orders_expires_at ON orders(expires_at)
    WHERE status = 'PAYMENT_PENDING' AND expires_at IS NOT NULL;

-- Partial index for orders needing settlement
CREATE INDEX idx_orders_pending_settlement ON orders(seller_id, created_at)
    WHERE status = 'DELIVERED';

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

CREATE INDEX idx_order_history_order_id ON order_history(order_id);
CREATE INDEX idx_order_history_created_at ON order_history(created_at DESC);

-- Triggers
CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_order_items_updated_at
    BEFORE UPDATE ON order_items
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Function to generate order number
CREATE OR REPLACE FUNCTION generate_order_number()
RETURNS TRIGGER AS $$
DECLARE
    seq_num INT;
BEGIN
    -- Get next sequence number for today
    SELECT COALESCE(MAX(CAST(SUBSTRING(order_number FROM 14) AS INT)), 0) + 1
    INTO seq_num
    FROM orders
    WHERE order_number LIKE 'ORD-' || TO_CHAR(NOW(), 'YYYYMMDD') || '-%';

    NEW.order_number := 'ORD-' || TO_CHAR(NOW(), 'YYYYMMDD') || '-' || LPAD(seq_num::TEXT, 5, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER generate_order_number_trigger
    BEFORE INSERT ON orders
    FOR EACH ROW
    WHEN (NEW.order_number IS NULL)
    EXECUTE FUNCTION generate_order_number();

-- Function to validate order state transitions
CREATE OR REPLACE FUNCTION validate_order_status_transition()
RETURNS TRIGGER AS $$
DECLARE
    valid_transition BOOLEAN := FALSE;
BEGIN
    -- Define valid transitions
    CASE OLD.status
        WHEN 'DRAFT' THEN
            valid_transition := NEW.status IN ('PLACED');
        WHEN 'PLACED' THEN
            valid_transition := NEW.status IN ('SELLER_CONFIRMED', 'SELLER_REJECTED');
        WHEN 'SELLER_CONFIRMED' THEN
            valid_transition := NEW.status IN ('PAYMENT_PENDING');
        WHEN 'PAYMENT_PENDING' THEN
            valid_transition := NEW.status IN ('PAID', 'PAYMENT_FAILED');
        WHEN 'PAYMENT_FAILED' THEN
            valid_transition := NEW.status IN ('PAYMENT_PENDING'); -- Retry
        WHEN 'PAID' THEN
            valid_transition := NEW.status IN ('DISPATCHED', 'CANCELLED_BY_BUYER');
        WHEN 'DISPATCHED' THEN
            valid_transition := NEW.status IN ('DELIVERED');
        WHEN 'DELIVERED' THEN
            valid_transition := NEW.status IN ('SETTLED');
        WHEN 'CANCELLED_BY_BUYER' THEN
            valid_transition := NEW.status IN ('REFUND_INITIATED');
        WHEN 'REFUND_INITIATED' THEN
            valid_transition := NEW.status IN ('REFUNDED');
        ELSE
            -- Terminal states: SELLER_REJECTED, SETTLED, REFUNDED
            valid_transition := FALSE;
    END CASE;

    IF NOT valid_transition THEN
        RAISE EXCEPTION 'Invalid order status transition from % to %', OLD.status, NEW.status;
    END IF;

    -- Increment version for optimistic locking
    NEW.version := OLD.version + 1;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER validate_order_status_trigger
    BEFORE UPDATE OF status ON orders
    FOR EACH ROW
    WHEN (OLD.status IS DISTINCT FROM NEW.status)
    EXECUTE FUNCTION validate_order_status_transition();
