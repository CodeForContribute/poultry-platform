-- V11: Create notification tables

-- Notification templates table
CREATE TABLE notif_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Template identification
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE, -- e.g., 'ORDER_PLACED', 'PAYMENT_SUCCESS'

    -- Channel
    channel notification_channel NOT NULL,

    -- Language support
    language VARCHAR(10) NOT NULL DEFAULT 'en',

    -- Content
    subject VARCHAR(255), -- For email
    body_template TEXT NOT NULL,

    -- Variables available in template
    variables JSONB DEFAULT '[]'::JSONB, -- ['order_id', 'amount', 'seller_name']

    -- For SMS/WhatsApp - template IDs from providers
    provider_template_id VARCHAR(100),

    status product_status NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Notifications table
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Template used
    template_id UUID REFERENCES notif_templates(id),
    template_code VARCHAR(50), -- Denormalized for faster queries

    -- Recipient
    recipient_type VARCHAR(20) NOT NULL, -- 'BUYER', 'SELLER_USER', 'ADMIN'
    recipient_id UUID NOT NULL,

    -- Channel
    channel notification_channel NOT NULL,

    -- Priority
    priority notification_priority NOT NULL DEFAULT 'MEDIUM',

    -- Content (rendered)
    subject VARCHAR(255),
    body TEXT NOT NULL,

    -- Payload data (for push notifications)
    data_payload JSONB,

    -- Delivery details
    recipient_address VARCHAR(255), -- Phone, email, FCM token

    -- Status tracking
    status notification_status NOT NULL DEFAULT 'PENDING',

    -- Provider response
    provider_message_id VARCHAR(100),
    provider_response JSONB,

    -- Timing
    scheduled_for TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,

    -- Error handling
    error_code VARCHAR(50),
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMPTZ,

    -- Reference to triggering entity
    reference_type VARCHAR(50), -- 'ORDER', 'PAYMENT', 'PRICE_CHANGE'
    reference_id UUID,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (created_at);

-- Create partitions for notifications (monthly, auto-drop after 90 days)
CREATE TABLE notifications_2024_01 PARTITION OF notifications
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
CREATE TABLE notifications_2024_02 PARTITION OF notifications
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
CREATE TABLE notifications_2024_03 PARTITION OF notifications
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');
CREATE TABLE notifications_2024_04 PARTITION OF notifications
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');
CREATE TABLE notifications_2024_05 PARTITION OF notifications
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');
CREATE TABLE notifications_2024_06 PARTITION OF notifications
    FOR VALUES FROM ('2024-06-01') TO ('2024-07-01');
CREATE TABLE notifications_2024_07 PARTITION OF notifications
    FOR VALUES FROM ('2024-07-01') TO ('2024-08-01');
CREATE TABLE notifications_2024_08 PARTITION OF notifications
    FOR VALUES FROM ('2024-08-01') TO ('2024-09-01');
CREATE TABLE notifications_2024_09 PARTITION OF notifications
    FOR VALUES FROM ('2024-09-01') TO ('2024-10-01');
CREATE TABLE notifications_2024_10 PARTITION OF notifications
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');
CREATE TABLE notifications_2024_11 PARTITION OF notifications
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');
CREATE TABLE notifications_2024_12 PARTITION OF notifications
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

-- 2025 partitions
CREATE TABLE notifications_2025_01 PARTITION OF notifications
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
CREATE TABLE notifications_2025_02 PARTITION OF notifications
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
CREATE TABLE notifications_2025_03 PARTITION OF notifications
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');
CREATE TABLE notifications_2025_04 PARTITION OF notifications
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');
CREATE TABLE notifications_2025_05 PARTITION OF notifications
    FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');
CREATE TABLE notifications_2025_06 PARTITION OF notifications
    FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');
CREATE TABLE notifications_2025_07 PARTITION OF notifications
    FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');
CREATE TABLE notifications_2025_08 PARTITION OF notifications
    FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');
CREATE TABLE notifications_2025_09 PARTITION OF notifications
    FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');
CREATE TABLE notifications_2025_10 PARTITION OF notifications
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');
CREATE TABLE notifications_2025_11 PARTITION OF notifications
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');
CREATE TABLE notifications_2025_12 PARTITION OF notifications
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

-- 2026 partitions
CREATE TABLE notifications_2026_01 PARTITION OF notifications
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE notifications_2026_02 PARTITION OF notifications
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');

-- Notification preferences table
CREATE TABLE notif_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- User
    user_type VARCHAR(20) NOT NULL, -- 'BUYER', 'SELLER_USER'
    user_id UUID NOT NULL,

    -- Channel preference
    channel notification_channel NOT NULL,

    -- Notification type
    notification_type VARCHAR(50) NOT NULL, -- 'ORDER_UPDATES', 'PRICE_ALERTS', 'MARKETING'

    -- Setting
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT unique_user_channel_type UNIQUE (user_type, user_id, channel, notification_type)
);

-- Indexes
CREATE INDEX idx_notif_templates_code ON notif_templates(code);
CREATE INDEX idx_notif_templates_channel ON notif_templates(channel, status);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_type, recipient_id);
CREATE INDEX idx_notifications_status ON notifications(status, created_at DESC);
CREATE INDEX idx_notifications_pending ON notifications(priority DESC, created_at)
    WHERE status = 'PENDING';
CREATE INDEX idx_notifications_retry ON notifications(next_retry_at)
    WHERE status = 'FAILED' AND retry_count < max_retries;
CREATE INDEX idx_notifications_reference ON notifications(reference_type, reference_id);

CREATE INDEX idx_notif_preferences_user ON notif_preferences(user_type, user_id);

-- Triggers
CREATE TRIGGER update_notif_templates_updated_at
    BEFORE UPDATE ON notif_templates
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_notifications_updated_at
    BEFORE UPDATE ON notifications
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_notif_preferences_updated_at
    BEFORE UPDATE ON notif_preferences
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert default templates
INSERT INTO notif_templates (code, name, channel, language, subject, body_template, variables) VALUES
    -- Order templates - English
    ('ORDER_PLACED_SMS_EN', 'Order Placed SMS', 'SMS', 'en', NULL,
     'Your order {{order_number}} for Rs.{{amount}} has been placed with {{seller_name}}. Track: {{tracking_url}}',
     '["order_number", "amount", "seller_name", "tracking_url"]'),

    ('ORDER_CONFIRMED_SMS_EN', 'Order Confirmed SMS', 'SMS', 'en', NULL,
     'Good news! {{seller_name}} confirmed your order {{order_number}}. Please complete payment within 30 mins.',
     '["order_number", "seller_name"]'),

    ('PAYMENT_SUCCESS_SMS_EN', 'Payment Success SMS', 'SMS', 'en', NULL,
     'Payment of Rs.{{amount}} received for order {{order_number}}. Your order will be dispatched soon!',
     '["order_number", "amount"]'),

    ('ORDER_DISPATCHED_SMS_EN', 'Order Dispatched SMS', 'SMS', 'en', NULL,
     'Your order {{order_number}} is on the way! Delivery agent: {{agent_name}}, {{agent_phone}}',
     '["order_number", "agent_name", "agent_phone"]'),

    ('DELIVERY_OTP_SMS_EN', 'Delivery OTP SMS', 'SMS', 'en', NULL,
     'Your delivery OTP for order {{order_number}} is {{otp}}. Share with delivery agent to confirm receipt.',
     '["order_number", "otp"]'),

    -- Hindi templates
    ('ORDER_PLACED_SMS_HI', 'Order Placed SMS Hindi', 'SMS', 'hi', NULL,
     'आपका ऑर्डर {{order_number}} Rs.{{amount}} का {{seller_name}} के पास जमा हो गया है। ट्रैक करें: {{tracking_url}}',
     '["order_number", "amount", "seller_name", "tracking_url"]'),

    -- Push notification templates
    ('ORDER_PLACED_PUSH', 'Order Placed Push', 'FCM', 'en', 'Order Placed',
     'Your order #{{order_number}} has been placed successfully',
     '["order_number"]'),

    ('PRICE_ALERT_PUSH', 'Price Alert Push', 'FCM', 'en', 'Price Update',
     '{{product_name}} price changed to Rs.{{new_price}}/{{unit}} by {{seller_name}}',
     '["product_name", "new_price", "unit", "seller_name"]'),

    -- Seller notifications
    ('NEW_ORDER_SELLER_SMS', 'New Order for Seller', 'SMS', 'en', NULL,
     'New order {{order_number}} received! Amount: Rs.{{amount}}. Login to confirm.',
     '["order_number", "amount"]'),

    ('SETTLEMENT_SUCCESS_SMS', 'Settlement Success', 'SMS', 'en', NULL,
     'Rs.{{amount}} has been transferred to your bank account. Ref: {{reference}}',
     '["amount", "reference"]');
