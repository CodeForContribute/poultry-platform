-- V16: Add approval workflow columns to settlements table

-- Add APPROVED and CANCELLED status values to the settlement_status enum
ALTER TYPE settlement_status ADD VALUE IF NOT EXISTS 'APPROVED';
ALTER TYPE settlement_status ADD VALUE IF NOT EXISTS 'CANCELLED';

-- Add approval columns
ALTER TABLE settlements
    ADD COLUMN IF NOT EXISTS approved_by UUID REFERENCES admin_users(id),
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS approval_remarks TEXT;

-- Add index for pending settlements needing approval
CREATE INDEX IF NOT EXISTS idx_settlements_pending_approval
    ON settlements(created_at DESC)
    WHERE status = 'PENDING';

-- Note: Index for 'APPROVED' status moved to V16_1 because PostgreSQL requires
-- new enum values to be committed before they can be used in the same transaction
