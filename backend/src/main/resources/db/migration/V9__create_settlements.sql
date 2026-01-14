-- V9: Create settlements and reconciliation tables

-- Settlements table
CREATE TABLE settlements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    seller_id UUID NOT NULL REFERENCES sellers(id),

    -- Settlement period
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,

    -- Amounts
    gross_amount DECIMAL(12, 2) NOT NULL,
    platform_fee_total DECIMAL(12, 2) NOT NULL,
    tds_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    other_deductions DECIMAL(12, 2) NOT NULL DEFAULT 0,
    net_amount DECIMAL(12, 2) NOT NULL,

    -- Order count
    order_count INT NOT NULL,

    -- Payout info
    payout_method VARCHAR(50), -- 'IMPS', 'NEFT', 'RTGS'
    payout_reference VARCHAR(100), -- Razorpay payout_id or bank reference
    bank_reference VARCHAR(100), -- UTR number

    -- Status
    status settlement_status NOT NULL DEFAULT 'PENDING',

    -- Retry handling
    failure_count INT NOT NULL DEFAULT 0,
    failure_reason TEXT,
    last_failure_at TIMESTAMPTZ,

    -- Timestamps
    scheduled_for TIMESTAMPTZ,
    initiated_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Settlement items (orders included in settlement)
CREATE TABLE settlement_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    settlement_id UUID NOT NULL REFERENCES settlements(id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES orders(id),

    order_amount DECIMAL(12, 2) NOT NULL,
    platform_fee DECIMAL(12, 2) NOT NULL,
    seller_amount DECIMAL(12, 2) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Reconciliation runs table
CREATE TABLE reconciliation_runs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    run_date DATE NOT NULL UNIQUE,

    -- Source files
    razorpay_file_url VARCHAR(500),
    bank_file_url VARCHAR(500),

    -- Results
    total_transactions INT NOT NULL DEFAULT 0,
    matched_count INT NOT NULL DEFAULT 0,
    unmatched_count INT NOT NULL DEFAULT 0,
    pending_count INT NOT NULL DEFAULT 0,

    total_expected_amount DECIMAL(14, 2) NOT NULL DEFAULT 0,
    total_actual_amount DECIMAL(14, 2) NOT NULL DEFAULT 0,
    discrepancy_amount DECIMAL(14, 2) NOT NULL DEFAULT 0,

    -- Report
    report_url VARCHAR(500),

    -- Status
    status recon_status NOT NULL DEFAULT 'PENDING',
    error_message TEXT,

    -- Timing
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Reconciliation mismatches table
CREATE TABLE recon_mismatches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recon_run_id UUID NOT NULL REFERENCES reconciliation_runs(id) ON DELETE CASCADE,

    -- Source info
    source VARCHAR(50) NOT NULL, -- 'RAZORPAY', 'BANK', 'LEDGER'
    source_reference VARCHAR(100) NOT NULL, -- payment_id, utr, txn_id

    -- Amount comparison
    expected_amount DECIMAL(12, 2),
    actual_amount DECIMAL(12, 2),
    variance DECIMAL(12, 2) NOT NULL,

    -- Mismatch type
    mismatch_type VARCHAR(50) NOT NULL, -- 'MISSING', 'AMOUNT_DIFF', 'DUPLICATE', 'TIMING'

    -- Related entities
    order_id UUID REFERENCES orders(id),
    payment_id UUID REFERENCES payments(id),
    settlement_id UUID REFERENCES settlements(id),

    -- Resolution
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMPTZ,
    resolved_by UUID,
    resolution_type VARCHAR(50), -- 'MANUAL_ADJUSTMENT', 'AUTO_MATCHED', 'WRITTEN_OFF'
    resolution_notes TEXT,

    -- Ledger adjustment entry (if created)
    adjustment_txn_id UUID,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- TDS records table (for compliance)
CREATE TABLE tds_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    seller_id UUID NOT NULL REFERENCES sellers(id),
    financial_year VARCHAR(10) NOT NULL, -- '2024-25'

    -- Seller PAN (for TDS)
    pan VARCHAR(10) NOT NULL,

    -- Totals
    total_payments DECIMAL(14, 2) NOT NULL DEFAULT 0,
    total_tds_deducted DECIMAL(12, 2) NOT NULL DEFAULT 0,

    -- TDS rate applied
    tds_rate DECIMAL(5, 2) NOT NULL,

    -- Form 16A generation
    form_16a_generated BOOLEAN NOT NULL DEFAULT FALSE,
    form_16a_url VARCHAR(500),
    form_16a_generated_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT unique_seller_fy UNIQUE (seller_id, financial_year)
);

-- Indexes
CREATE INDEX idx_settlements_seller_id ON settlements(seller_id);
CREATE INDEX idx_settlements_status ON settlements(status);
CREATE INDEX idx_settlements_scheduled ON settlements(scheduled_for) WHERE status = 'PENDING';
CREATE INDEX idx_settlements_period ON settlements(period_start, period_end);

CREATE INDEX idx_settlement_items_settlement_id ON settlement_items(settlement_id);
CREATE INDEX idx_settlement_items_order_id ON settlement_items(order_id);

CREATE INDEX idx_recon_runs_date ON reconciliation_runs(run_date DESC);
CREATE INDEX idx_recon_runs_status ON reconciliation_runs(status);

CREATE INDEX idx_recon_mismatches_run_id ON recon_mismatches(recon_run_id);
CREATE INDEX idx_recon_mismatches_unresolved ON recon_mismatches(created_at DESC)
    WHERE resolved = FALSE;

CREATE INDEX idx_tds_records_seller_fy ON tds_records(seller_id, financial_year);

-- Triggers
CREATE TRIGGER update_settlements_updated_at
    BEFORE UPDATE ON settlements
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_recon_mismatches_updated_at
    BEFORE UPDATE ON recon_mismatches
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tds_records_updated_at
    BEFORE UPDATE ON tds_records
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
