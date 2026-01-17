-- V13: Create additional composite indexes for common query patterns

-- Seller dashboard queries
CREATE INDEX idx_orders_seller_dashboard ON orders(seller_id, status, created_at DESC)
    WHERE status NOT IN ('SETTLED', 'REFUNDED', 'SELLER_REJECTED');

-- Buyer order history
CREATE INDEX idx_orders_buyer_history ON orders(buyer_id, created_at DESC);

-- Active products with current pricing (for catalog)
CREATE INDEX idx_products_active_catalog ON products(seller_id, category_id, status)
    WHERE deleted_at IS NULL AND status = 'ACTIVE';

-- Settlement pending calculation
CREATE INDEX idx_orders_settlement_pending ON orders(seller_id, total_amount, platform_fee)
    WHERE status = 'DELIVERED';

-- Payment reconciliation queries
CREATE INDEX idx_payments_recon ON payments(gateway, status, created_at)
    WHERE status = 'SUCCESS';

-- Ledger balance calculation per entity
CREATE INDEX idx_ledger_balance_calc ON ledger_entries(account_type, entity_id, direction, amount);

-- Delivery SLA monitoring (using order_id instead of seller_id since it's referenced via orders table)
CREATE INDEX idx_delivery_sla_monitor ON delivery_tracking(order_id, promised_delivery_time, status)
    WHERE status NOT IN ('DELIVERED', 'DELIVERY_FAILED');

-- OTP verification (recent OTPs for a phone)
-- Note: Cannot use NOW() in index predicate as it's not IMMUTABLE
CREATE INDEX idx_otp_recent ON otp_requests(phone_hash, purpose, expires_at, created_at DESC)
    WHERE verified = FALSE;

-- Notification queue processing
CREATE INDEX idx_notification_queue ON notifications(channel, priority DESC, scheduled_for, created_at)
    WHERE status = 'PENDING';

-- Price change notification targets (buyers who favorited)
CREATE INDEX idx_favorites_price_notify ON buyer_favorites(product_id)
    WHERE notify_on_price_change = TRUE AND product_id IS NOT NULL;

-- Full-text search on products (optional, for search functionality)
ALTER TABLE products ADD COLUMN IF NOT EXISTS search_vector tsvector;

CREATE OR REPLACE FUNCTION products_search_vector_update()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.name, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.sku, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER products_search_vector_trigger
    BEFORE INSERT OR UPDATE OF name, description, sku ON products
    FOR EACH ROW
    EXECUTE FUNCTION products_search_vector_update();

CREATE INDEX idx_products_search ON products USING gin(search_vector);

-- Analytics indexes (for admin dashboard)
-- Note: Using timestamp column directly; query planner handles date range filters efficiently
CREATE INDEX idx_orders_daily_stats ON orders(created_at, status);
CREATE INDEX idx_payments_daily_stats ON payments(created_at, status, amount);

-- GIN index for JSONB columns that are frequently queried
CREATE INDEX idx_orders_delivery_address ON orders USING gin(delivery_address);
CREATE INDEX idx_products_attributes ON products USING gin(attributes);
CREATE INDEX idx_buyers_addresses ON buyers USING gin(addresses);

-- Index for password rotation reminder queries (sellers due for password change)
-- Note: Cannot use NOW() in partial index as it's not IMMUTABLE; filter at query time instead
CREATE INDEX idx_seller_users_pwd_rotation ON seller_users(password_changed_at)
    WHERE status = 'ACTIVE';

-- Index for finding duplicate payments
CREATE INDEX idx_payments_duplicate_check ON payments(order_id, amount, status)
    WHERE status = 'SUCCESS';
