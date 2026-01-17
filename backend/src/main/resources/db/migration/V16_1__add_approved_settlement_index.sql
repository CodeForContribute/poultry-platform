-- V16_1: Add index for approved settlements (requires V16 enum values to be committed first)

CREATE INDEX IF NOT EXISTS idx_settlements_approved_processing
    ON settlements(approved_at DESC)
    WHERE status = 'APPROVED';
