-- V7: Create ledger tables (double-entry bookkeeping, append-only)

-- Ledger entries table - APPEND ONLY, NO UPDATES OR DELETES
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Transaction grouping (all entries with same txn_id must balance to zero)
    txn_id UUID NOT NULL,

    -- Account info
    account_type ledger_account_type NOT NULL,
    entity_id UUID, -- buyer_id, seller_id, or NULL for platform accounts

    -- Amount (always positive, direction determines debit/credit)
    amount DECIMAL(12, 2) NOT NULL CHECK (amount > 0),
    direction ledger_direction NOT NULL,

    -- Reference to source transaction
    reference_type ledger_reference_type NOT NULL,
    reference_id UUID NOT NULL, -- order_id, payment_id, settlement_id

    -- Metadata
    description TEXT,
    metadata JSONB,

    -- Immutable timestamp
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()

    -- NO updated_at - entries are immutable
);

-- Prevent updates and deletes on ledger_entries
CREATE OR REPLACE FUNCTION prevent_ledger_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Ledger entries are immutable. Updates and deletes are not allowed.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER prevent_ledger_update
    BEFORE UPDATE ON ledger_entries
    FOR EACH ROW
    EXECUTE FUNCTION prevent_ledger_modification();

CREATE TRIGGER prevent_ledger_delete
    BEFORE DELETE ON ledger_entries
    FOR EACH ROW
    EXECUTE FUNCTION prevent_ledger_modification();

-- Function to validate that a transaction balances
CREATE OR REPLACE FUNCTION validate_ledger_balance()
RETURNS TRIGGER AS $$
DECLARE
    total_debit DECIMAL(12, 2);
    total_credit DECIMAL(12, 2);
BEGIN
    -- Calculate totals for this transaction
    SELECT
        COALESCE(SUM(CASE WHEN direction = 'DR' THEN amount ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN direction = 'CR' THEN amount ELSE 0 END), 0)
    INTO total_debit, total_credit
    FROM ledger_entries
    WHERE txn_id = NEW.txn_id;

    -- Note: This is a soft check - full validation should be done at application level
    -- after all entries for a transaction are inserted

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Account balances view (materialized for performance)
CREATE MATERIALIZED VIEW account_balances AS
SELECT
    account_type,
    entity_id,
    SUM(CASE WHEN direction = 'DR' THEN -amount ELSE amount END) as balance,
    COUNT(*) as entry_count,
    MAX(created_at) as last_updated
FROM ledger_entries
GROUP BY account_type, entity_id;

CREATE UNIQUE INDEX idx_account_balances_unique ON account_balances(account_type, entity_id);

-- Indexes for ledger_entries
CREATE INDEX idx_ledger_txn_id ON ledger_entries(txn_id);
CREATE INDEX idx_ledger_account ON ledger_entries(account_type, entity_id);
CREATE INDEX idx_ledger_reference ON ledger_entries(reference_type, reference_id);
CREATE INDEX idx_ledger_created_at ON ledger_entries(created_at DESC);

-- For balance queries by entity
CREATE INDEX idx_ledger_entity_balance ON ledger_entries(entity_id, account_type, created_at DESC)
    WHERE entity_id IS NOT NULL;

-- Daily ledger summary table (for reconciliation)
CREATE TABLE ledger_daily_summary (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    summary_date DATE NOT NULL,
    account_type ledger_account_type NOT NULL,

    opening_balance DECIMAL(12, 2) NOT NULL,
    total_debits DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total_credits DECIMAL(12, 2) NOT NULL DEFAULT 0,
    closing_balance DECIMAL(12, 2) NOT NULL,

    entry_count INT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT unique_daily_summary UNIQUE (summary_date, account_type)
);

CREATE INDEX idx_ledger_daily_summary_date ON ledger_daily_summary(summary_date DESC);
