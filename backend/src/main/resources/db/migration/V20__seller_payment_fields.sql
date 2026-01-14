-- Add Razorpay payment fields to sellers table for storing contact and fund account IDs
-- This avoids creating duplicate contacts/fund accounts in Razorpay for each payout

ALTER TABLE sellers
    ADD COLUMN IF NOT EXISTS razorpay_contact_id VARCHAR(50),
    ADD COLUMN IF NOT EXISTS razorpay_fund_account_id VARCHAR(50);

-- Add indexes for faster lookups when checking for existing Razorpay accounts
CREATE INDEX IF NOT EXISTS idx_sellers_razorpay_contact_id ON sellers(razorpay_contact_id) WHERE razorpay_contact_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_sellers_razorpay_fund_account_id ON sellers(razorpay_fund_account_id) WHERE razorpay_fund_account_id IS NOT NULL;

COMMENT ON COLUMN sellers.razorpay_contact_id IS 'Razorpay Contact ID for the seller, used for payouts';
COMMENT ON COLUMN sellers.razorpay_fund_account_id IS 'Razorpay Fund Account ID for the seller bank account, used for payouts';
