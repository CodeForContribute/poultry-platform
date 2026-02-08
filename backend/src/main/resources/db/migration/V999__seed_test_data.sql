-- V999: Seed test data for development and testing
-- This file creates comprehensive test data for all entities
-- IMPORTANT: Use only in development environments

-- Note: Password 'Test@1234' - BCrypt hash generated with strength 12
-- You may need to regenerate this hash using BCrypt encoder

-- ============ SELLERS ============

-- Seller 1: Active seller with complete profile
INSERT INTO sellers (id, business_name, gstin, pan, phone_encrypted, email, address, bank_account_number_encrypted, bank_ifsc, bank_name, bank_account_holder, settlement_cycle, platform_fee_percent, fssai_number, status)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Sunrise Poultry Farm',
    '29ABCDE1234F1Z5',
    'ABCDE1234F',
    -- Encrypted phone: 9876543210 (placeholder - will be encrypted by application)
    decode('000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f', 'hex'),
    'admin@seller.com',
    '{"line1": "123 Farm Road", "line2": "Near Highway", "city": "Hyderabad", "state": "Telangana", "pincode": "500001", "landmark": "Opposite Bus Stand"}',
    -- Encrypted bank account (placeholder)
    decode('000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f', 'hex'),
    'SBIN0001234',
    'State Bank of India',
    'Sunrise Poultry Farm',
    'T_PLUS_2',
    2.00,
    '12345678901234',
    'ACTIVE'
);

-- Seller 2: Pending verification seller
INSERT INTO sellers (id, business_name, gstin, pan, phone_encrypted, email, address, status)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    'Green Valley Farms',
    '36FGHIJ5678K2Z9',
    'FGHIJ5678K',
    decode('000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f', 'hex'),
    'contact@greenvalley.com',
    '{"line1": "45 Green Lane", "city": "Bangalore", "state": "Karnataka", "pincode": "560001"}',
    'PENDING_VERIFICATION'
);

-- ============ SELLER USERS ============

-- Password: Test@1234 (BCrypt hash with strength 12)
-- Hash: $2a$12$QlDTBqBkNjrVKRjMPjVt.uSMPT9NFaCPQyGEd6dsAQKFDOCSyfMmC

-- Seller Admin for Seller 1
INSERT INTO seller_users (id, seller_id, email, password_hash, name, phone, role, status)
VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    '11111111-1111-1111-1111-111111111111',
    'admin@seller.com',
    '$2a$12$QlDTBqBkNjrVKRjMPjVt.uSMPT9NFaCPQyGEd6dsAQKFDOCSyfMmC',
    'Ramesh Kumar',
    '9876543210',
    'SELLER_ADMIN',
    'ACTIVE'
);

-- Seller Staff for Seller 1
INSERT INTO seller_users (id, seller_id, email, password_hash, name, phone, role, status)
VALUES (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    '11111111-1111-1111-1111-111111111111',
    'staff@seller.com',
    '$2a$12$QlDTBqBkNjrVKRjMPjVt.uSMPT9NFaCPQyGEd6dsAQKFDOCSyfMmC',
    'Suresh Reddy',
    '9876543211',
    'SELLER_STAFF',
    'ACTIVE'
);

-- Seller Admin for Seller 2
INSERT INTO seller_users (id, seller_id, email, password_hash, name, phone, role, status)
VALUES (
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    '22222222-2222-2222-2222-222222222222',
    'admin@greenvalley.com',
    '$2a$12$QlDTBqBkNjrVKRjMPjVt.uSMPT9NFaCPQyGEd6dsAQKFDOCSyfMmC',
    'Prakash Rao',
    '9876543212',
    'SELLER_ADMIN',
    'ACTIVE'
);

-- ============ PRODUCTS ============

-- Get category IDs
DO $$
DECLARE
    cat_broilers UUID;
    cat_layers UUID;
    cat_eggs UUID;
    cat_feed UUID;
    cat_chicks UUID;
    cat_vaccines UUID;
    cat_equipment UUID;
BEGIN
    SELECT id INTO cat_broilers FROM categories WHERE code = 'BROILERS';
    SELECT id INTO cat_layers FROM categories WHERE code = 'LAYERS';
    SELECT id INTO cat_eggs FROM categories WHERE code = 'EGGS';
    SELECT id INTO cat_feed FROM categories WHERE code = 'FEED';
    SELECT id INTO cat_chicks FROM categories WHERE code = 'DAY_OLD_CHICKS';
    SELECT id INTO cat_vaccines FROM categories WHERE code = 'VACCINES';
    SELECT id INTO cat_equipment FROM categories WHERE code = 'EQUIPMENT';

    -- Products for Seller 1
    INSERT INTO products (id, seller_id, category_id, name, name_hi, sku, description, unit, min_order_qty, max_order_qty, status, attributes)
    VALUES
    -- Broilers
    ('a1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', cat_broilers,
     'Live Broiler Chicken', 'जीवित ब्रॉयलर मुर्गा', 'BROIL-001',
     'Fresh, healthy broiler chickens. Average weight 2-2.5 kg per bird.', 'KG', 50, 5000, 'ACTIVE',
     '{"breed": "Cobb 500", "avg_weight": "2.2 kg", "age": "35-42 days"}'),

    -- Layers
    ('a2222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', cat_layers,
     'Layer Hens - Ready to Lay', 'लेयर मुर्गी - अंडे देने के लिए तैयार', 'LAYER-001',
     'Healthy layer hens at point of lay (18-20 weeks old).', 'PIECE', 100, 2000, 'ACTIVE',
     '{"breed": "Hy-Line Brown", "age": "18-20 weeks", "production": "280-300 eggs/year"}'),

    -- Eggs
    ('a3333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', cat_eggs,
     'Fresh Brown Eggs - Tray', 'ताजे भूरे अंडे - ट्रे', 'EGG-001',
     'Farm fresh brown eggs. 30 eggs per tray.', 'TRAY', 10, 500, 'ACTIVE',
     '{"eggs_per_tray": 30, "shell_color": "Brown", "size": "Large"}'),

    ('a4444444-4444-4444-4444-444444444444', '11111111-1111-1111-1111-111111111111', cat_eggs,
     'Premium White Eggs - Tray', 'प्रीमियम सफेद अंडे - ट्रे', 'EGG-002',
     'Premium quality white eggs. 30 eggs per tray.', 'TRAY', 10, 500, 'ACTIVE',
     '{"eggs_per_tray": 30, "shell_color": "White", "size": "Extra Large"}'),

    -- Feed
    ('a5555555-5555-5555-5555-555555555555', '11111111-1111-1111-1111-111111111111', cat_feed,
     'Broiler Starter Feed', 'ब्रॉयलर स्टार्टर फीड', 'FEED-001',
     'High protein starter feed for broiler chicks (0-14 days).', 'BAG', 5, 200, 'ACTIVE',
     '{"weight_per_bag": "50 kg", "protein": "22%", "for_age": "0-14 days"}'),

    ('a6666666-6666-6666-6666-666666666666', '11111111-1111-1111-1111-111111111111', cat_feed,
     'Layer Grower Feed', 'लेयर ग्रोअर फीड', 'FEED-002',
     'Balanced grower feed for layer birds (6-18 weeks).', 'BAG', 5, 200, 'ACTIVE',
     '{"weight_per_bag": "50 kg", "protein": "18%", "for_age": "6-18 weeks"}'),

    -- Day Old Chicks
    ('a7777777-7777-7777-7777-777777777777', '11111111-1111-1111-1111-111111111111', cat_chicks,
     'Day Old Broiler Chicks', 'एक दिन के ब्रॉयलर चूजे', 'CHICK-001',
     'Healthy day-old Cobb 500 broiler chicks.', 'PIECE', 100, 10000, 'ACTIVE',
     '{"breed": "Cobb 500", "vaccination": "Marek''s, ND"}'),

    -- Vaccines
    ('a8888888-8888-8888-8888-888888888888', '11111111-1111-1111-1111-111111111111', cat_vaccines,
     'Newcastle Disease Vaccine', 'न्यूकैसल रोग का टीका', 'VAC-001',
     'Live vaccine for Newcastle disease. 1000 doses per vial.', 'BOTTLE', 1, 50, 'ACTIVE',
     '{"doses_per_bottle": 1000, "storage": "2-8°C", "route": "Eye drop/Spray"}'),

    -- Equipment
    ('a9999999-9999-9999-9999-999999999999', '11111111-1111-1111-1111-111111111111', cat_equipment,
     'Automatic Chicken Feeder', 'स्वचालित चिकन फीडर', 'EQUIP-001',
     '10 kg capacity automatic poultry feeder.', 'PIECE', 1, 100, 'ACTIVE',
     '{"capacity": "10 kg", "material": "Plastic", "suitable_for": "15-20 birds"}'),

    -- Out of Stock product
    ('aa000000-0000-0000-0000-000000000000', '11111111-1111-1111-1111-111111111111', cat_broilers,
     'Desi Chicken (Kadaknath)', 'देसी मुर्गा (कड़कनाथ)', 'BROIL-002',
     'Premium Kadaknath chicken. Black meat variety.', 'KG', 5, 100, 'OUT_OF_STOCK',
     '{"breed": "Kadaknath", "avg_weight": "1.5 kg", "type": "Free range"}');

END $$;

-- ============ PRICE HISTORY ============

INSERT INTO price_history (id, product_id, base_price, bulk_discount_slabs, effective_from, created_by)
VALUES
-- Broiler prices
('ab111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', 120.00,
 '[{"min_qty": 100, "discount_percent": 2}, {"min_qty": 500, "discount_percent": 5}, {"min_qty": 1000, "discount_percent": 8}]',
 NOW() - INTERVAL '30 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),

-- Layer prices
('ab222222-2222-2222-2222-222222222222', 'a2222222-2222-2222-2222-222222222222', 350.00,
 '[{"min_qty": 200, "discount_percent": 3}, {"min_qty": 500, "discount_percent": 5}]',
 NOW() - INTERVAL '30 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),

-- Egg prices
('ab333333-3333-3333-3333-333333333333', 'a3333333-3333-3333-3333-333333333333', 150.00,
 '[{"min_qty": 50, "discount_percent": 2}, {"min_qty": 100, "discount_percent": 4}]',
 NOW() - INTERVAL '30 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),

('ab444444-4444-4444-4444-444444444444', 'a4444444-4444-4444-4444-444444444444', 180.00,
 '[{"min_qty": 50, "discount_percent": 2}]',
 NOW() - INTERVAL '30 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),

-- Feed prices
('ab555555-5555-5555-5555-555555555555', 'a5555555-5555-5555-5555-555555555555', 1800.00,
 '[{"min_qty": 20, "discount_percent": 2}, {"min_qty": 50, "discount_percent": 5}]',
 NOW() - INTERVAL '30 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),

('ab666666-6666-6666-6666-666666666666', 'a6666666-6666-6666-6666-666666666666', 1650.00,
 '[{"min_qty": 20, "discount_percent": 2}]',
 NOW() - INTERVAL '30 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),

-- Chick prices
('ab777777-7777-7777-7777-777777777777', 'a7777777-7777-7777-7777-777777777777', 42.00,
 '[{"min_qty": 500, "discount_percent": 3}, {"min_qty": 2000, "discount_percent": 5}]',
 NOW() - INTERVAL '30 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),

-- Vaccine prices
('ab888888-8888-8888-8888-888888888888', 'a8888888-8888-8888-8888-888888888888', 250.00,
 '[{"min_qty": 10, "discount_percent": 5}]',
 NOW() - INTERVAL '30 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),

-- Equipment prices
('ab999999-9999-9999-9999-999999999999', 'a9999999-9999-9999-9999-999999999999', 850.00,
 '[{"min_qty": 10, "discount_percent": 5}]',
 NOW() - INTERVAL '30 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');

-- ============ BUYERS ============

INSERT INTO buyers (id, phone_encrypted, phone_hash, name, business_name, gstin, addresses, status)
VALUES
('b1111111-1111-1111-1111-111111111111',
 decode('000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f', 'hex'),
 'hash1234567890abcdef1234567890abcdef1234567890abcdef1234567890ab',
 'Vijay Traders', 'Vijay Poultry Traders', '36AABCT1332L1ZH',
 '[{"line1": "45 Market Road", "city": "Hyderabad", "state": "Telangana", "pincode": "500003", "type": "BUSINESS", "is_default": true}]',
 'ACTIVE'),

('b2222222-2222-2222-2222-222222222222',
 decode('000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f', 'hex'),
 'hash2234567890abcdef1234567890abcdef1234567890abcdef1234567890ab',
 'Kumar Restaurant', 'Kumar Family Restaurant', NULL,
 '[{"line1": "123 Food Street", "city": "Secunderabad", "state": "Telangana", "pincode": "500025", "type": "BUSINESS", "is_default": true}]',
 'ACTIVE'),

('b3333333-3333-3333-3333-333333333333',
 decode('000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f', 'hex'),
 'hash3234567890abcdef1234567890abcdef1234567890abcdef1234567890ab',
 'Lakshmi Farm', 'Lakshmi Poultry Farm', '29BBBCD5678E2ZK',
 '[{"line1": "Village Road", "city": "Rangareddy", "state": "Telangana", "pincode": "500075", "type": "FARM", "is_default": true}]',
 'ACTIVE'),

('b4444444-4444-4444-4444-444444444444',
 decode('000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f', 'hex'),
 'hash4234567890abcdef1234567890abcdef1234567890abcdef1234567890ab',
 'Fresh Mart', 'Fresh Mart Superstore', '36CCCDE9012F3ZM',
 '[{"line1": "22 Mall Road", "city": "Hyderabad", "state": "Telangana", "pincode": "500034", "type": "RETAIL", "is_default": true}]',
 'ACTIVE'),

('b5555555-5555-5555-5555-555555555555',
 decode('000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f', 'hex'),
 'hash5234567890abcdef1234567890abcdef1234567890abcdef1234567890ab',
 'Srini Hotels', 'Srini Hotel Chain', '36DDDEG3456H4ZN',
 '[{"line1": "88 Hotel Avenue", "city": "Hyderabad", "state": "Telangana", "pincode": "500081", "type": "BUSINESS", "is_default": true}]',
 'ACTIVE');

-- ============ ORDERS ============
-- Note: Orders use generated order numbers, so we let the trigger create them

-- PLACED orders (pending seller action)
INSERT INTO orders (id, buyer_id, seller_id, idempotency_key, type, status, subtotal, gst_amount, delivery_charge, total_amount, platform_fee, delivery_date, delivery_slot, delivery_address, price_snapshot, version)
VALUES
('01111111-1111-1111-1111-111111111111', 'b1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111',
 'idem-001-placed-1', 'INSTANT', 'PLACED', 12000.00, 0.00, 200.00, 12200.00, 244.00,
 CURRENT_DATE + 1, '6AM-9AM', '{"line1": "45 Market Road", "city": "Hyderabad", "state": "Telangana", "pincode": "500003"}',
 '{"products": [{"id": "a1111111-1111-1111-1111-111111111111", "price": 120.00}]}', 1),

('01111111-1111-1111-1111-111111111112', 'b2222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111',
 'idem-001-placed-2', 'INSTANT', 'PLACED', 3000.00, 0.00, 150.00, 3150.00, 63.00,
 CURRENT_DATE + 1, '9AM-12PM', '{"line1": "123 Food Street", "city": "Secunderabad", "state": "Telangana", "pincode": "500025"}',
 '{"products": [{"id": "a3333333-3333-3333-3333-333333333333", "price": 150.00}]}', 1);

-- SELLER_CONFIRMED orders (awaiting payment)
INSERT INTO orders (id, buyer_id, seller_id, idempotency_key, type, status, subtotal, gst_amount, delivery_charge, total_amount, platform_fee, delivery_date, delivery_slot, delivery_address, price_snapshot, version)
VALUES
('02222222-2222-2222-2222-222222222221', 'b3333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111',
 'idem-002-confirmed-1', 'PREORDER', 'SELLER_CONFIRMED', 21000.00, 0.00, 500.00, 21500.00, 430.00,
 CURRENT_DATE + 3, '6AM-9AM', '{"line1": "Village Road", "city": "Rangareddy", "state": "Telangana", "pincode": "500075"}',
 '{"products": [{"id": "a7777777-7777-7777-7777-777777777777", "price": 42.00}]}', 2);

-- PAID orders (ready for dispatch)
INSERT INTO orders (id, buyer_id, seller_id, idempotency_key, type, status, subtotal, gst_amount, delivery_charge, total_amount, platform_fee, delivery_date, delivery_slot, delivery_address, price_snapshot, version)
VALUES
('03333333-3333-3333-3333-333333333331', 'b4444444-4444-4444-4444-444444444444', '11111111-1111-1111-1111-111111111111',
 'idem-003-paid-1', 'INSTANT', 'PAID', 7500.00, 0.00, 200.00, 7700.00, 154.00,
 CURRENT_DATE, '6AM-9AM', '{"line1": "22 Mall Road", "city": "Hyderabad", "state": "Telangana", "pincode": "500034"}',
 '{"products": [{"id": "a3333333-3333-3333-3333-333333333333", "price": 150.00}]}', 3),

('03333333-3333-3333-3333-333333333332', 'b5555555-5555-5555-5555-555555555555', '11111111-1111-1111-1111-111111111111',
 'idem-003-paid-2', 'INSTANT', 'PAID', 18000.00, 2160.00, 300.00, 20460.00, 409.20,
 CURRENT_DATE, '9AM-12PM', '{"line1": "88 Hotel Avenue", "city": "Hyderabad", "state": "Telangana", "pincode": "500081"}',
 '{"products": [{"id": "a5555555-5555-5555-5555-555555555555", "price": 1800.00}]}', 3);

-- DISPATCHED orders (in transit)
INSERT INTO orders (id, buyer_id, seller_id, idempotency_key, type, status, subtotal, gst_amount, delivery_charge, total_amount, platform_fee, delivery_date, delivery_slot, delivery_address, price_snapshot, version)
VALUES
('04444444-4444-4444-4444-444444444441', 'b1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111',
 'idem-004-dispatched-1', 'INSTANT', 'DISPATCHED', 6000.00, 0.00, 200.00, 6200.00, 124.00,
 CURRENT_DATE, '6AM-9AM', '{"line1": "45 Market Road", "city": "Hyderabad", "state": "Telangana", "pincode": "500003"}',
 '{"products": [{"id": "a1111111-1111-1111-1111-111111111111", "price": 120.00}]}', 4);

-- DELIVERED orders (completed)
INSERT INTO orders (id, buyer_id, seller_id, idempotency_key, type, status, subtotal, gst_amount, delivery_charge, total_amount, platform_fee, delivery_date, delivery_slot, delivery_address, price_snapshot, version, created_at)
VALUES
('05555555-5555-5555-5555-555555555551', 'b1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111',
 'idem-005-delivered-1', 'INSTANT', 'DELIVERED', 24000.00, 0.00, 500.00, 24500.00, 490.00,
 CURRENT_DATE - 5, '6AM-9AM', '{"line1": "45 Market Road", "city": "Hyderabad", "state": "Telangana", "pincode": "500003"}',
 '{"products": [{"id": "a1111111-1111-1111-1111-111111111111", "price": 120.00}]}', 5, NOW() - INTERVAL '7 days'),

('05555555-5555-5555-5555-555555555552', 'b2222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111',
 'idem-005-delivered-2', 'INSTANT', 'DELIVERED', 15000.00, 0.00, 300.00, 15300.00, 306.00,
 CURRENT_DATE - 3, '9AM-12PM', '{"line1": "123 Food Street", "city": "Secunderabad", "state": "Telangana", "pincode": "500025"}',
 '{"products": [{"id": "a3333333-3333-3333-3333-333333333333", "price": 150.00}]}', 5, NOW() - INTERVAL '5 days'),

('05555555-5555-5555-5555-555555555553', 'b3333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111',
 'idem-005-delivered-3', 'PREORDER', 'DELIVERED', 84000.00, 0.00, 1000.00, 85000.00, 1700.00,
 CURRENT_DATE - 10, '6AM-9AM', '{"line1": "Village Road", "city": "Rangareddy", "state": "Telangana", "pincode": "500075"}',
 '{"products": [{"id": "a7777777-7777-7777-7777-777777777777", "price": 42.00}]}', 5, NOW() - INTERVAL '12 days'),

('05555555-5555-5555-5555-555555555554', 'b4444444-4444-4444-4444-444444444444', '11111111-1111-1111-1111-111111111111',
 'idem-005-delivered-4', 'INSTANT', 'DELIVERED', 4500.00, 0.00, 150.00, 4650.00, 93.00,
 CURRENT_DATE - 2, '9AM-12PM', '{"line1": "22 Mall Road", "city": "Hyderabad", "state": "Telangana", "pincode": "500034"}',
 '{"products": [{"id": "a4444444-4444-4444-4444-444444444444", "price": 180.00}]}', 5, NOW() - INTERVAL '3 days'),

('05555555-5555-5555-5555-555555555555', 'b5555555-5555-5555-5555-555555555555', '11111111-1111-1111-1111-111111111111',
 'idem-005-delivered-5', 'INSTANT', 'DELIVERED', 72000.00, 8640.00, 1500.00, 82140.00, 1642.80,
 CURRENT_DATE - 1, '6AM-9AM', '{"line1": "88 Hotel Avenue", "city": "Hyderabad", "state": "Telangana", "pincode": "500081"}',
 '{"products": [{"id": "a5555555-5555-5555-5555-555555555555", "price": 1800.00}]}', 5, NOW() - INTERVAL '2 days');

-- CANCELLED order
INSERT INTO orders (id, buyer_id, seller_id, idempotency_key, type, status, subtotal, gst_amount, delivery_charge, total_amount, platform_fee, delivery_date, delivery_slot, delivery_address, price_snapshot, version, cancelled_at, cancelled_by, cancellation_reason)
VALUES
('06666666-6666-6666-6666-666666666661', 'b1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111',
 'idem-006-cancelled-1', 'INSTANT', 'CANCELLED_BY_BUYER', 5000.00, 0.00, 200.00, 5200.00, 104.00,
 CURRENT_DATE - 5, '6AM-9AM', '{"line1": "45 Market Road", "city": "Hyderabad", "state": "Telangana", "pincode": "500003"}',
 '{"products": [{"id": "a1111111-1111-1111-1111-111111111111", "price": 120.00}]}', 3,
 NOW() - INTERVAL '5 days', 'BUYER', 'Changed requirements');

-- ============ ORDER ITEMS ============

INSERT INTO order_items (id, order_id, product_id, product_snapshot, quantity, unit_price, discount_percent, discount_amount, gst_percent, gst_amount, line_total, status)
VALUES
-- Items for order 01111111-1111-1111-1111-111111111111
('0f111111-1111-1111-1111-111111111111', '01111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111',
 '{"name": "Live Broiler Chicken", "unit": "KG"}', 100, 120.00, 0, 0, 0, 0, 12000.00, 'PENDING'),

-- Items for other orders (simplified)
('0f111111-1111-1111-1111-111111111112', '01111111-1111-1111-1111-111111111112', 'a3333333-3333-3333-3333-333333333333',
 '{"name": "Fresh Brown Eggs", "unit": "TRAY"}', 20, 150.00, 0, 0, 0, 0, 3000.00, 'PENDING'),

('0f222222-2222-2222-2222-222222222221', '02222222-2222-2222-2222-222222222221', 'a7777777-7777-7777-7777-777777777777',
 '{"name": "Day Old Broiler Chicks", "unit": "PIECE"}', 500, 42.00, 0, 0, 0, 0, 21000.00, 'PENDING'),

('0f333333-3333-3333-3333-333333333331', '03333333-3333-3333-3333-333333333331', 'a3333333-3333-3333-3333-333333333333',
 '{"name": "Fresh Brown Eggs", "unit": "TRAY"}', 50, 150.00, 0, 0, 0, 0, 7500.00, 'PENDING'),

('0f333333-3333-3333-3333-333333333332', '03333333-3333-3333-3333-333333333332', 'a5555555-5555-5555-5555-555555555555',
 '{"name": "Broiler Starter Feed", "unit": "BAG"}', 10, 1800.00, 0, 0, 12, 2160.00, 20160.00, 'PENDING'),

('0f444444-4444-4444-4444-444444444441', '04444444-4444-4444-4444-444444444441', 'a1111111-1111-1111-1111-111111111111',
 '{"name": "Live Broiler Chicken", "unit": "KG"}', 50, 120.00, 0, 0, 0, 0, 6000.00, 'PENDING'),

('0f555555-5555-5555-5555-555555555551', '05555555-5555-5555-5555-555555555551', 'a1111111-1111-1111-1111-111111111111',
 '{"name": "Live Broiler Chicken", "unit": "KG"}', 200, 120.00, 0, 0, 0, 0, 24000.00, 'DELIVERED'),

('0f555555-5555-5555-5555-555555555552', '05555555-5555-5555-5555-555555555552', 'a3333333-3333-3333-3333-333333333333',
 '{"name": "Fresh Brown Eggs", "unit": "TRAY"}', 100, 150.00, 0, 0, 0, 0, 15000.00, 'DELIVERED'),

('0f555555-5555-5555-5555-555555555553', '05555555-5555-5555-5555-555555555553', 'a7777777-7777-7777-7777-777777777777',
 '{"name": "Day Old Broiler Chicks", "unit": "PIECE"}', 2000, 42.00, 0, 0, 0, 0, 84000.00, 'DELIVERED'),

('0f555555-5555-5555-5555-555555555554', '05555555-5555-5555-5555-555555555554', 'a4444444-4444-4444-4444-444444444444',
 '{"name": "Premium White Eggs", "unit": "TRAY"}', 25, 180.00, 0, 0, 0, 0, 4500.00, 'DELIVERED'),

('0f555555-5555-5555-5555-555555555555', '05555555-5555-5555-5555-555555555555', 'a5555555-5555-5555-5555-555555555555',
 '{"name": "Broiler Starter Feed", "unit": "BAG"}', 40, 1800.00, 0, 0, 12, 8640.00, 80640.00, 'DELIVERED');

-- ============ SETTLEMENTS ============

INSERT INTO settlements (id, seller_id, period_start, period_end, gross_amount, platform_fee_total, tds_amount, other_deductions, net_amount, order_count, status, payout_method, created_at)
VALUES
-- Completed settlements
('c1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111',
 CURRENT_DATE - INTERVAL '14 days', CURRENT_DATE - INTERVAL '7 days',
 45000.00, 900.00, 450.00, 0.00, 43650.00, 3, 'SUCCESS', 'NEFT', NOW() - INTERVAL '5 days'),

('c2222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111',
 CURRENT_DATE - INTERVAL '21 days', CURRENT_DATE - INTERVAL '14 days',
 85000.00, 1700.00, 850.00, 0.00, 82450.00, 5, 'SUCCESS', 'NEFT', NOW() - INTERVAL '12 days'),

-- Pending settlements
('c3333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111',
 CURRENT_DATE - INTERVAL '7 days', CURRENT_DATE,
 126100.00, 2522.00, 1261.00, 0.00, 122317.00, 5, 'PENDING', NULL, NOW()),

('c4444444-4444-4444-4444-444444444444', '11111111-1111-1111-1111-111111111111',
 CURRENT_DATE - INTERVAL '28 days', CURRENT_DATE - INTERVAL '21 days',
 32000.00, 640.00, 320.00, 0.00, 31040.00, 2, 'APPROVED', 'NEFT', NOW() - INTERVAL '18 days'),

-- Failed settlement
('c5555555-5555-5555-5555-555555555555', '11111111-1111-1111-1111-111111111111',
 CURRENT_DATE - INTERVAL '35 days', CURRENT_DATE - INTERVAL '28 days',
 28000.00, 560.00, 280.00, 0.00, 27160.00, 2, 'FAILED', 'NEFT', NOW() - INTERVAL '25 days');

-- Update failed settlement with failure info
UPDATE settlements SET failure_count = 1, failure_reason = 'Bank account verification failed', last_failure_at = NOW() - INTERVAL '24 days'
WHERE id = 'c5555555-5555-5555-5555-555555555555';

-- ============ DISPUTES ============

-- Create dispute number sequence
CREATE SEQUENCE IF NOT EXISTS dispute_number_seq START 1;

INSERT INTO disputes (id, dispute_number, order_id, buyer_id, seller_id, raised_by, type, status, title, description, evidence_urls, requested_resolution, requested_amount, priority, created_at)
VALUES
-- Open dispute waiting for seller response
('d1111111-1111-1111-1111-111111111111', 'DSP' || nextval('dispute_number_seq'),
 '05555555-5555-5555-5555-555555555551', 'b1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111',
 'BUYER', 'QUALITY_ISSUE', 'AWAITING_RESPONSE',
 'Quality below expectations',
 'Some of the broilers received were underweight. Expected 2.2kg average but received 1.8kg average.',
 '["https://storage.example.com/evidence/img1.jpg", "https://storage.example.com/evidence/img2.jpg"]',
 'REFUND_PARTIAL', 2400.00, 2, NOW() - INTERVAL '2 days'),

-- Dispute under review
('d2222222-2222-2222-2222-222222222222', 'DSP' || nextval('dispute_number_seq'),
 '05555555-5555-5555-5555-555555555552', 'b2222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111',
 'BUYER', 'QUANTITY_MISMATCH', 'UNDER_REVIEW',
 'Missing items in order',
 'Received only 90 trays instead of 100 trays ordered.',
 '["https://storage.example.com/evidence/img3.jpg"]',
 'REFUND_PARTIAL', 1500.00, 2, NOW() - INTERVAL '5 days'),

-- Resolved dispute
('d3333333-3333-3333-3333-333333333333', 'DSP' || nextval('dispute_number_seq'),
 '05555555-5555-5555-5555-555555555553', 'b3333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111',
 'BUYER', 'DELIVERY_ISSUE', 'RESOLVED',
 'Late delivery',
 'Order was delivered 3 hours after the scheduled slot.',
 '[]',
 'CREDIT_NOTE', NULL, 3, NOW() - INTERVAL '10 days');

-- Update resolved dispute with resolution info
UPDATE disputes SET
    final_resolution = 'CREDIT_NOTE',
    resolution_notes = 'Credit of Rs 1000 applied to buyer account for inconvenience caused.',
    resolved_at = NOW() - INTERVAL '8 days'
WHERE id = 'd3333333-3333-3333-3333-333333333333';

-- Add dispute messages
INSERT INTO dispute_messages (id, dispute_id, sender_type, sender_id, message, attachment_urls, is_internal, created_at)
VALUES
-- Messages for first dispute
('de111111-1111-1111-1111-111111111111', 'd1111111-1111-1111-1111-111111111111', 'BUYER', 'b1111111-1111-1111-1111-111111111111',
 'I weighed the chickens and they are significantly underweight. Please check the attached photos showing the scale readings.',
 '["https://storage.example.com/evidence/scale1.jpg"]', FALSE, NOW() - INTERVAL '2 days'),

-- Messages for second dispute
('de222222-2222-2222-2222-222222222221', 'd2222222-2222-2222-2222-222222222222', 'BUYER', 'b2222222-2222-2222-2222-222222222222',
 'I counted the trays multiple times. The delivery person also acknowledged the shortage.',
 '[]', FALSE, NOW() - INTERVAL '5 days'),

('de222222-2222-2222-2222-222222222222', 'd2222222-2222-2222-2222-222222222222', 'SELLER', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
 'We are checking with our dispatch team and will get back to you within 24 hours.',
 '[]', FALSE, NOW() - INTERVAL '4 days'),

-- Messages for third dispute (resolved)
('de333333-3333-3333-3333-333333333331', 'd3333333-3333-3333-3333-333333333333', 'BUYER', 'b3333333-3333-3333-3333-333333333333',
 'The delivery was scheduled for 6-9 AM but arrived at 12 PM.',
 '[]', FALSE, NOW() - INTERVAL '10 days'),

('de333333-3333-3333-3333-333333333332', 'd3333333-3333-3333-3333-333333333333', 'SELLER', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
 'We apologize for the delay. There was a vehicle breakdown. We would like to offer a credit note for your next order.',
 '[]', FALSE, NOW() - INTERVAL '9 days'),

('de333333-3333-3333-3333-333333333333', 'd3333333-3333-3333-3333-333333333333', 'ADMIN', '00000000-0000-0000-0000-000000000001',
 'Resolution applied: Credit note of Rs 1000 added to buyer account.',
 '[]', FALSE, NOW() - INTERVAL '8 days');

-- ============ NOTIFICATIONS PREFERENCES ============

INSERT INTO seller_notification_preferences (id, seller_id)
VALUES
('ae111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111');

-- ============ SUMMARY ============
-- Test Credentials:
-- Email: admin@seller.com
-- Password: Test@1234
--
-- Data created:
-- - 2 Sellers (1 active, 1 pending verification)
-- - 3 Seller users
-- - 10 Products across all categories
-- - 9 Price history records
-- - 5 Buyers
-- - 15+ Orders in various states
-- - 5 Settlements (2 success, 2 pending, 1 failed)
-- - 3 Disputes with messages
