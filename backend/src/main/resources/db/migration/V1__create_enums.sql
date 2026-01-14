-- V1: Create all ENUM types for the poultry platform
-- These must be created before tables that reference them

-- Seller status
CREATE TYPE seller_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION');

-- Seller user roles
CREATE TYPE seller_user_role AS ENUM ('SELLER_ADMIN', 'SELLER_STAFF');

-- Buyer status
CREATE TYPE buyer_status AS ENUM ('ACTIVE', 'INACTIVE', 'BLOCKED');

-- Admin roles
CREATE TYPE admin_role AS ENUM ('SUPER_ADMIN', 'OPERATIONS', 'FINANCE', 'SUPPORT');

-- Product categories
CREATE TYPE product_category AS ENUM (
    'DAY_OLD_CHICKS',
    'BROILERS',
    'LAYERS',
    'EGGS',
    'FEED',
    'VACCINES',
    'EQUIPMENT'
);

-- Product units
CREATE TYPE product_unit AS ENUM ('KG', 'PIECE', 'TRAY', 'BAG', 'BOTTLE', 'BOX');

-- Product status
CREATE TYPE product_status AS ENUM ('ACTIVE', 'INACTIVE', 'OUT_OF_STOCK');

-- Order types
CREATE TYPE order_type AS ENUM ('INSTANT', 'PREORDER');

-- Order status (state machine states)
CREATE TYPE order_status AS ENUM (
    'DRAFT',
    'PLACED',
    'SELLER_CONFIRMED',
    'SELLER_REJECTED',
    'PAYMENT_PENDING',
    'PAYMENT_FAILED',
    'PAID',
    'DISPATCHED',
    'DELIVERED',
    'SETTLED',
    'CANCELLED_BY_BUYER',
    'REFUND_INITIATED',
    'REFUNDED'
);

-- Order item status (for partial delivery)
CREATE TYPE order_item_status AS ENUM ('PENDING', 'DISPATCHED', 'DELIVERED', 'CANCELLED', 'RETURNED');

-- Payment gateway
CREATE TYPE payment_gateway AS ENUM ('RAZORPAY', 'CASHFREE');

-- Payment status
CREATE TYPE payment_status AS ENUM ('CREATED', 'PENDING', 'SUCCESS', 'FAILED', 'REFUNDED');

-- Ledger account types
CREATE TYPE ledger_account_type AS ENUM (
    'BUYER_WALLET',
    'PLATFORM_ESCROW',
    'PLATFORM_FEE',
    'SELLER_RECEIVABLE',
    'SELLER_SETTLED',
    'REFUND',
    'TDS'
);

-- Ledger entry direction
CREATE TYPE ledger_direction AS ENUM ('DR', 'CR');

-- Ledger reference types
CREATE TYPE ledger_reference_type AS ENUM ('ORDER', 'PAYMENT', 'SETTLEMENT', 'REFUND', 'ADJUSTMENT');

-- Settlement status
CREATE TYPE settlement_status AS ENUM ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED');

-- Settlement cycle
CREATE TYPE settlement_cycle AS ENUM ('T_PLUS_1', 'T_PLUS_2', 'WEEKLY');

-- Delivery status
CREATE TYPE delivery_status AS ENUM (
    'ASSIGNED',
    'PICKED_UP',
    'IN_TRANSIT',
    'OUT_FOR_DELIVERY',
    'DELIVERED',
    'DELIVERY_FAILED'
);

-- Delivery failure reasons
CREATE TYPE delivery_failure_reason AS ENUM (
    'BUYER_UNAVAILABLE',
    'WRONG_ADDRESS',
    'REJECTED_BY_BUYER',
    'DAMAGED_GOODS',
    'OTHER'
);

-- Delivery proof type
CREATE TYPE delivery_proof_type AS ENUM ('OTP', 'PHOTO');

-- Notification channels
CREATE TYPE notification_channel AS ENUM ('FCM', 'SMS', 'WHATSAPP', 'EMAIL');

-- Notification priority
CREATE TYPE notification_priority AS ENUM ('HIGH', 'MEDIUM', 'LOW');

-- Notification status
CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'DELIVERED', 'FAILED');

-- OTP purpose
CREATE TYPE otp_purpose AS ENUM ('LOGIN', 'DELIVERY_CONFIRM', 'PASSWORD_RESET');

-- Audit action types
CREATE TYPE audit_action AS ENUM (
    'LOGIN_SUCCESS',
    'LOGIN_FAILED',
    'LOGOUT',
    'PASSWORD_CHANGE',
    'OTP_REQUEST',
    'OTP_VERIFY',
    'ORDER_CREATE',
    'ORDER_UPDATE',
    'PAYMENT_ATTEMPT',
    'SETTLEMENT_INITIATE'
);

-- Reconciliation status
CREATE TYPE recon_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED');
