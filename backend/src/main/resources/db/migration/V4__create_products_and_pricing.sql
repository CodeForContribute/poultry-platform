-- V4: Create products and pricing tables

-- Categories table (reference data)
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code product_category NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    name_hi VARCHAR(100), -- Hindi name
    hsn_code VARCHAR(10) NOT NULL,
    description TEXT,
    gst_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    status product_status NOT NULL DEFAULT 'ACTIVE',
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Products table
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    seller_id UUID NOT NULL REFERENCES sellers(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id),
    name VARCHAR(255) NOT NULL,
    name_hi VARCHAR(255), -- Hindi name
    sku VARCHAR(50) NOT NULL,
    description TEXT,
    unit product_unit NOT NULL,
    min_order_qty DECIMAL(10, 3) NOT NULL DEFAULT 1,
    max_order_qty DECIMAL(10, 3),
    image_urls JSONB DEFAULT '[]'::JSONB,
    attributes JSONB DEFAULT '{}'::JSONB, -- Flexible attributes (breed, age, weight range, etc.)
    status product_status NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMPTZ, -- Soft delete
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT unique_seller_sku UNIQUE (seller_id, sku)
);

-- Price history table (maintains full history, supports scheduled pricing)
CREATE TABLE price_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    base_price DECIMAL(12, 2) NOT NULL,
    bulk_discount_slabs JSONB DEFAULT '[]'::JSONB,
    -- Format: [{"min_qty": 100, "max_qty": 500, "discount_percent": 5}, ...]
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ, -- NULL means currently active or future scheduled
    created_by UUID, -- seller_user_id who created this price
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Ensure no overlapping price periods for same product
    CONSTRAINT no_overlapping_prices EXCLUDE USING gist (
        product_id WITH =,
        tstzrange(effective_from, COALESCE(effective_to, 'infinity'::timestamptz)) WITH &&
    )
);

-- Buyer favorites (for price alerts)
CREATE TABLE buyer_favorites (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    buyer_id UUID NOT NULL REFERENCES buyers(id) ON DELETE CASCADE,
    seller_id UUID REFERENCES sellers(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id) ON DELETE CASCADE,
    notify_on_price_change BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Can favorite either a seller or a specific product
    CONSTRAINT favorite_target CHECK (
        (seller_id IS NOT NULL AND product_id IS NULL) OR
        (seller_id IS NULL AND product_id IS NOT NULL)
    ),
    CONSTRAINT unique_buyer_seller_favorite UNIQUE (buyer_id, seller_id),
    CONSTRAINT unique_buyer_product_favorite UNIQUE (buyer_id, product_id)
);

-- Indexes
CREATE INDEX idx_products_seller_id ON products(seller_id);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_status ON products(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_sku ON products(sku);

CREATE INDEX idx_price_history_product_id ON price_history(product_id);
CREATE INDEX idx_price_history_effective ON price_history(product_id, effective_from, effective_to);
-- Index for finding current price
CREATE INDEX idx_price_history_current ON price_history(product_id, effective_from DESC)
    WHERE effective_to IS NULL OR effective_to > NOW();

CREATE INDEX idx_buyer_favorites_buyer_id ON buyer_favorites(buyer_id);
CREATE INDEX idx_buyer_favorites_product_id ON buyer_favorites(product_id) WHERE product_id IS NOT NULL;
CREATE INDEX idx_buyer_favorites_seller_id ON buyer_favorites(seller_id) WHERE seller_id IS NOT NULL;

-- Triggers
CREATE TRIGGER update_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert default categories
INSERT INTO categories (code, name, name_hi, hsn_code, gst_rate, display_order) VALUES
    ('DAY_OLD_CHICKS', 'Day Old Chicks', 'एक दिन के चूजे', '0105', 0.00, 1),
    ('BROILERS', 'Broilers (Live)', 'ब्रॉयलर (जीवित)', '0105', 0.00, 2),
    ('LAYERS', 'Layers (Live)', 'लेयर्स (जीवित)', '0105', 0.00, 3),
    ('EGGS', 'Eggs', 'अंडे', '0407', 0.00, 4),
    ('FEED', 'Poultry Feed', 'मुर्गी का चारा', '2309', 0.00, 5),
    ('VACCINES', 'Vaccines & Medicines', 'टीके और दवाएं', '3002', 12.00, 6),
    ('EQUIPMENT', 'Equipment & Supplies', 'उपकरण', '8436', 18.00, 7);
