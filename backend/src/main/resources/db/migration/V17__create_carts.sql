-- V17: Create carts table for buyer shopping carts

CREATE TABLE IF NOT EXISTS carts
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    buyer_id UUID NOT NULL REFERENCES buyers
(
    id
),
    seller_id UUID NOT NULL REFERENCES sellers
(
    id
),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
),
    UNIQUE
(
    buyer_id,
    seller_id
)
    );

CREATE TABLE IF NOT EXISTS cart_items
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    cart_id UUID NOT NULL REFERENCES carts
(
    id
) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products
(
    id
),
    quantity DECIMAL
(
    12,
    3
) NOT NULL,
    unit_price DECIMAL
(
    12,
    2
),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
),
    UNIQUE
(
    cart_id,
    product_id
)
    );

CREATE INDEX idx_carts_buyer ON carts (buyer_id);
CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);

CREATE TRIGGER update_carts_updated_at
    BEFORE UPDATE
    ON carts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_cart_items_updated_at
    BEFORE UPDATE
    ON cart_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
