-- V22: Create seller bank accounts and notification preferences tables

-- Seller bank accounts table (for multiple bank accounts per seller)
CREATE TABLE seller_bank_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    seller_id UUID NOT NULL REFERENCES sellers(id) ON DELETE CASCADE,
    account_holder_name VARCHAR(100) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    account_number_encrypted BYTEA NOT NULL,
    account_number_last4 VARCHAR(4) NOT NULL,
    ifsc_code VARCHAR(11) NOT NULL,
    account_type VARCHAR(20) NOT NULL DEFAULT 'SAVINGS',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    razorpay_fund_account_id VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seller notification preferences table
CREATE TABLE seller_notification_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    seller_id UUID NOT NULL UNIQUE REFERENCES sellers(id) ON DELETE CASCADE,

    -- Email preferences
    email_new_orders BOOLEAN NOT NULL DEFAULT TRUE,
    email_order_updates BOOLEAN NOT NULL DEFAULT TRUE,
    email_payment_received BOOLEAN NOT NULL DEFAULT TRUE,
    email_settlement_completed BOOLEAN NOT NULL DEFAULT TRUE,
    email_dispute_raised BOOLEAN NOT NULL DEFAULT TRUE,
    email_promotions BOOLEAN NOT NULL DEFAULT FALSE,

    -- SMS preferences
    sms_new_orders BOOLEAN NOT NULL DEFAULT TRUE,
    sms_order_updates BOOLEAN NOT NULL DEFAULT FALSE,
    sms_payment_received BOOLEAN NOT NULL DEFAULT TRUE,
    sms_settlement_completed BOOLEAN NOT NULL DEFAULT TRUE,
    sms_dispute_raised BOOLEAN NOT NULL DEFAULT TRUE,

    -- Push notification preferences
    push_new_orders BOOLEAN NOT NULL DEFAULT TRUE,
    push_order_updates BOOLEAN NOT NULL DEFAULT TRUE,
    push_payment_received BOOLEAN NOT NULL DEFAULT TRUE,
    push_settlement_completed BOOLEAN NOT NULL DEFAULT TRUE,
    push_dispute_raised BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_seller_bank_accounts_seller_id ON seller_bank_accounts(seller_id);
CREATE INDEX idx_seller_bank_accounts_primary ON seller_bank_accounts(seller_id, is_primary) WHERE is_primary = TRUE;
CREATE INDEX idx_seller_notification_prefs_seller_id ON seller_notification_preferences(seller_id);

-- Triggers
CREATE TRIGGER update_seller_bank_accounts_updated_at
    BEFORE UPDATE ON seller_bank_accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_seller_notification_prefs_updated_at
    BEFORE UPDATE ON seller_notification_preferences
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
