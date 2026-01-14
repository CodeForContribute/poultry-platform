-- V18: Create reviews and ratings tables

CREATE TYPE review_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED', 'HIDDEN');

CREATE TABLE IF NOT EXISTS seller_reviews
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    seller_id UUID NOT NULL REFERENCES sellers
(
    id
),
    buyer_id UUID NOT NULL REFERENCES buyers
(
    id
),
    order_id UUID NOT NULL REFERENCES orders
(
    id
),
    rating INT NOT NULL CHECK
(
    rating
    >=
    1
    AND
    rating
    <=
    5
),
    title VARCHAR
(
    200
),
    comment TEXT,
    quality_rating INT CHECK
(
    quality_rating
    >=
    1
    AND
    quality_rating
    <=
    5
),
    delivery_rating INT CHECK
(
    delivery_rating
    >=
    1
    AND
    delivery_rating
    <=
    5
),
    communication_rating INT CHECK
(
    communication_rating
    >=
    1
    AND
    communication_rating
    <=
    5
),
    status review_status NOT NULL DEFAULT 'APPROVED',
    is_verified_purchase BOOLEAN NOT NULL DEFAULT true,
    helpful_count INT NOT NULL DEFAULT 0,
    seller_response TEXT,
    seller_responded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
),
    UNIQUE
(
    order_id
)
    );

CREATE TABLE IF NOT EXISTS seller_rating_summary
(
    seller_id
    UUID
    PRIMARY
    KEY
    REFERENCES
    sellers
(
    id
),
    total_reviews INT NOT NULL DEFAULT 0,
    average_rating DECIMAL
(
    3,
    2
) NOT NULL DEFAULT 0,
    average_quality DECIMAL
(
    3,
    2
),
    average_delivery DECIMAL
(
    3,
    2
),
    average_communication DECIMAL
(
    3,
    2
),
    rating_1_count INT NOT NULL DEFAULT 0,
    rating_2_count INT NOT NULL DEFAULT 0,
    rating_3_count INT NOT NULL DEFAULT 0,
    rating_4_count INT NOT NULL DEFAULT 0,
    rating_5_count INT NOT NULL DEFAULT 0,
    last_calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
)
    );

CREATE INDEX idx_reviews_seller ON seller_reviews (seller_id, created_at DESC);
CREATE INDEX idx_reviews_buyer ON seller_reviews (buyer_id);
CREATE INDEX idx_reviews_status ON seller_reviews (status) WHERE status = 'APPROVED';

CREATE TRIGGER update_seller_reviews_updated_at
    BEFORE UPDATE
    ON seller_reviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
