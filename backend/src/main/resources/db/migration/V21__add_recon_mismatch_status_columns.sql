-- V21: Add missing columns to reconciliation tables

-- Add missing columns to reconciliation_runs
ALTER TABLE reconciliation_runs
    ADD COLUMN IF NOT EXISTS run_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(20) NOT NULL DEFAULT 'RAZORPAY';

-- Add missing columns to recon_mismatches
ALTER TABLE recon_mismatches
    ADD COLUMN IF NOT EXISTS expected_status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS actual_status VARCHAR(50);
