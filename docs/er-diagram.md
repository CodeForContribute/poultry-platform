# Poultry B2B Platform - Entity Relationship Diagram

## Visual ER Diagram (ASCII)

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                          CORE ENTITIES                                                       │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

    ┌──────────────────┐         ┌──────────────────┐         ┌──────────────────┐
    │     SELLERS      │         │      BUYERS      │         │   ADMIN_USERS    │
    ├──────────────────┤         ├──────────────────┤         ├──────────────────┤
    │ PK id            │         │ PK id            │         │ PK id            │
    │    business_name │         │    phone (enc)   │         │    email         │
    │    gstin         │         │    name          │         │    password_hash │
    │    phone (enc)   │         │    addresses[]   │         │    role          │
    │    email         │         │    device_id     │         │    status        │
    │    password_hash │         │    status        │         │    last_login    │
    │    bank_account  │         │    created_at    │         │    created_at    │
    │    ifsc_code     │         │    updated_at    │         └──────────────────┘
    │    settlement_cyc│         └────────┬─────────┘
    │    fssai_number  │                  │
    │    status        │                  │
    │    created_at    │                  │
    └────────┬─────────┘                  │
             │                            │
             │ 1:N                        │
             ▼                            │
    ┌──────────────────┐                  │
    │   SELLER_USERS   │                  │
    ├──────────────────┤                  │
    │ PK id            │                  │
    │ FK seller_id     │                  │
    │    email         │                  │
    │    password_hash │                  │
    │    role (ADMIN/  │                  │
    │         STAFF)   │                  │
    │    pwd_changed_at│                  │
    │    status        │                  │
    │    created_at    │                  │
    └──────────────────┘                  │
                                          │
┌─────────────────────────────────────────┼───────────────────────────────────────────────────────────────────┐
│                                    PRODUCT & PRICING                                                         │
└─────────────────────────────────────────┼───────────────────────────────────────────────────────────────────┘
                                          │
    ┌──────────────────┐                  │
    │    CATEGORIES    │                  │
    ├──────────────────┤                  │
    │ PK id            │                  │
    │    name          │                  │
    │    hsn_code      │◄─────────┐       │
    │    description   │          │       │
    │    status        │          │       │
    └──────────────────┘          │       │
                                  │       │
    ┌──────────────────┐          │       │         ┌──────────────────┐
    │     PRODUCTS     │          │       │         │  BUYER_FAVORITES │
    ├──────────────────┤          │       │         ├──────────────────┤
    │ PK id            │          │       │         │ PK id            │
    │ FK seller_id     │──────────┼───────┼────────►│ FK buyer_id      │
    │ FK category_id   │──────────┘       │         │ FK seller_id     │
    │    name          │                  │         │ FK product_id    │
    │    sku           │                  │         │    created_at    │
    │    description   │                  │         └──────────────────┘
    │    unit (kg/pc/  │                  │
    │         tray)    │                  │
    │    min_order_qty │                  │
    │    max_order_qty │                  │
    │    status        │                  │
    │    deleted_at    │ (soft delete)    │
    │    created_at    │                  │
    │    updated_at    │                  │
    └────────┬─────────┘                  │
             │                            │
             │ 1:N                        │
             ▼                            │
    ┌──────────────────┐                  │
    │  PRICE_HISTORY   │                  │
    ├──────────────────┤                  │
    │ PK id            │                  │
    │ FK product_id    │                  │
    │    base_price    │                  │
    │    bulk_slabs[]  │ (JSONB)          │
    │    effective_from│                  │
    │    effective_to  │                  │
    │    created_by    │                  │
    │    created_at    │                  │
    └──────────────────┘                  │
                                          │
┌─────────────────────────────────────────┼───────────────────────────────────────────────────────────────────┐
│                                      ORDER MANAGEMENT                                                        │
└─────────────────────────────────────────┼───────────────────────────────────────────────────────────────────┘
                                          │
    ┌─────────────────────────────────────┴───────────────────────────────────────┐
    │                                  ORDERS                                      │
    ├──────────────────────────────────────────────────────────────────────────────┤
    │ PK id (UUID)                                                                 │
    │ FK buyer_id ─────────────────────────────────────────────────────────────────┤
    │ FK seller_id                                                                 │
    │    idempotency_key (UNIQUE)                                                  │
    │    type (INSTANT/PREORDER)                                                   │
    │    status (enum - see state machine below)                                   │
    │    total_amount                                                              │
    │    platform_fee                                                              │
    │    gst_amount                                                                │
    │    delivery_date                                                             │
    │    delivery_address (JSONB)                                                  │
    │    price_snapshot (JSONB) ◄── immutable copy at order time                   │
    │    version (optimistic lock)                                                 │
    │    expires_at                                                                │
    │    created_at                                                                │
    │    updated_at                                                                │
    └────────────────────────────────────────┬─────────────────────────────────────┘
                                             │
                     ┌───────────────────────┼───────────────────────┐
                     │                       │                       │
                     ▼                       ▼                       ▼
    ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
    │   ORDER_ITEMS    │    │  ORDER_HISTORY   │    │    PAYMENTS      │
    ├──────────────────┤    ├──────────────────┤    ├──────────────────┤
    │ PK id            │    │ PK id            │    │ PK id            │
    │ FK order_id      │    │ FK order_id      │    │ FK order_id      │
    │ FK product_id    │    │    from_status   │    │    gateway       │
    │    product_snap  │    │    to_status     │    │    gw_order_id   │
    │    qty           │    │    changed_by    │    │    gw_payment_id │
    │    unit_price    │    │    reason        │    │    amount        │
    │    line_total    │    │    created_at    │    │    status        │
    │    status        │    └──────────────────┘    │    webhook_data  │
    │    created_at    │                            │    signature_ok  │
    └──────────────────┘                            │    created_at    │
                                                    │    updated_at    │
                                                    └──────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                    ORDER STATE MACHINE                                                       │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

    ┌───────┐     place      ┌────────┐    confirm    ┌─────────────────┐    pay_init   ┌─────────────────┐
    │ DRAFT │───────────────►│ PLACED │──────────────►│ SELLER_CONFIRMED│─────────────►│ PAYMENT_PENDING │
    └───────┘                └────┬───┘               └────────┬────────┘              └────────┬────────┘
                                  │                            │                                │
                                  │ reject                     │                                │ pay_success
                                  ▼                            │                                ▼
                         ┌────────────────┐                    │                       ┌───────────────┐
                         │SELLER_REJECTED │ (terminal)         │                       │     PAID      │
                         └────────────────┘                    │                       └───────┬───────┘
                                                               │                               │
                                                               │                    ┌──────────┼──────────┐
                                                               │                    │          │          │
                                  ┌────────────────────────────┘                    │ dispatch │ cancel   │
                                  │ pay_failed                                      ▼          ▼          │
                                  ▼                                        ┌────────────┐ ┌────────────┐  │
                         ┌────────────────┐   retry                        │ DISPATCHED │ │ CANCELLED  │  │
                         │ PAYMENT_FAILED │────────┐                       └─────┬──────┘ │ _BY_BUYER  │  │
                         └────────┬───────┘        │                             │        └─────┬──────┘  │
                                  │                │                             │ deliver      │         │
                                  └────────────────┘                             ▼              │ refund  │
                                                                          ┌────────────┐       ▼         │
                                                                          │ DELIVERED  │  ┌──────────┐   │
                                                                          └─────┬──────┘  │ REFUND_  │   │
                                                                                │         │ INITIATED│   │
                                                                                │ settle  └────┬─────┘   │
                                                                                ▼              │         │
                                                                          ┌────────────┐      │ complete│
                                                                          │  SETTLED   │      ▼         │
                                                                          └────────────┘ ┌──────────┐   │
                                                                                         │ REFUNDED │   │
                                                                                         └──────────┘   │

┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                    DELIVERY & TRACKING                                                       │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

    ┌──────────────────┐         ┌──────────────────┐         ┌──────────────────┐
    │ DELIVERY_AGENTS  │         │ DELIVERY_TRACKING│         │  DELIVERY_PROOF  │
    ├──────────────────┤         ├──────────────────┤         ├──────────────────┤
    │ PK id            │◄────────│ FK agent_id      │         │ PK id            │
    │ FK seller_id     │         │ PK id            │────────►│ FK tracking_id   │
    │    name          │         │ FK order_id      │         │    type (OTP/    │
    │    phone (enc)   │         │    status        │         │          PHOTO)  │
    │    vehicle_no    │         │    otp_code      │         │    otp_entered   │
    │    status        │         │    retry_count   │         │    photo_url     │
    │    created_at    │         │    failure_reason│         │    verified      │
    └──────────────────┘         │    sla_deadline  │         │    created_at    │
                                 │    created_at    │         └──────────────────┘
                                 │    updated_at    │
                                 └──────────────────┘

    Delivery Status Flow:
    ASSIGNED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED
                                                        └─→ DELIVERY_FAILED (max 2 retries)

┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                              LEDGER & SETTLEMENT (Double-Entry Bookkeeping)                                  │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────────────────────────────┐
    │                              LEDGER_ENTRIES (Append-Only)                                │
    ├─────────────────────────────────────────────────────────────────────────────────────────┤
    │ PK id (UUID)                                                                            │
    │    txn_id (UUID) ◄── groups related entries (debit + credit must balance)               │
    │    account_type:                                                                        │
    │       - BUYER_WALLET                                                                    │
    │       - PLATFORM_ESCROW                                                                 │
    │       - PLATFORM_FEE                                                                    │
    │       - SELLER_RECEIVABLE                                                               │
    │       - SELLER_SETTLED                                                                  │
    │       - REFUND                                                                          │
    │    entity_id (buyer_id or seller_id)                                                    │
    │    amount (DECIMAL 12,2)                                                                │
    │    direction (DR/CR)                                                                    │
    │    reference_type (ORDER/PAYMENT/SETTLEMENT/REFUND)                                     │
    │    reference_id                                                                         │
    │    created_at                                                                           │
    │                                                                                         │
    │    *** NO UPDATE/DELETE - APPEND ONLY ***                                               │
    └─────────────────────────────────────────────────────────────────────────────────────────┘

    Sample Transaction Flow (₹1000 order, 2% platform fee):

    1. Buyer Payment Received:
       ┌─────────────────────────────────────────────────────────────────┐
       │ txn_id: abc-123                                                 │
       │ Entry 1: DR BUYER_WALLET     buyer_001   ₹1000                  │
       │ Entry 2: CR PLATFORM_ESCROW  platform    ₹1000                  │
       │                                          ──────                 │
       │                              Balance:    ₹0 ✓                   │
       └─────────────────────────────────────────────────────────────────┘

    2. Order Delivered (release to seller):
       ┌─────────────────────────────────────────────────────────────────┐
       │ txn_id: def-456                                                 │
       │ Entry 1: DR PLATFORM_ESCROW    platform    ₹1000                │
       │ Entry 2: CR PLATFORM_FEE       platform    ₹20                  │
       │ Entry 3: CR SELLER_RECEIVABLE  seller_001  ₹980                 │
       │                                            ──────               │
       │                                Balance:    ₹0 ✓                 │
       └─────────────────────────────────────────────────────────────────┘

    3. Settlement to Seller Bank:
       ┌─────────────────────────────────────────────────────────────────┐
       │ txn_id: ghi-789                                                 │
       │ Entry 1: DR SELLER_RECEIVABLE  seller_001  ₹980                 │
       │ Entry 2: CR SELLER_SETTLED     seller_001  ₹980                 │
       │                                            ──────               │
       │                                Balance:    ₹0 ✓                 │
       └─────────────────────────────────────────────────────────────────┘

    ┌──────────────────┐         ┌──────────────────┐         ┌──────────────────────┐
    │   SETTLEMENTS    │         │ SETTLEMENT_ITEMS │         │ RECONCILIATION_RUNS  │
    ├──────────────────┤         ├──────────────────┤         ├──────────────────────┤
    │ PK id            │◄────────│ FK settlement_id │         │ PK id                │
    │ FK seller_id     │         │ PK id            │         │    run_date          │
    │    total_amount  │         │ FK order_id      │         │    razorpay_file_url │
    │    tds_amount    │         │    amount        │         │    bank_file_url     │
    │    net_amount    │         │    created_at    │         │    matched_count     │
    │    payout_ref    │         └──────────────────┘         │    unmatched_count   │
    │    bank_ref      │                                      │    pending_count     │
    │    status        │         ┌──────────────────┐         │    discrepancy_amt   │
    │    initiated_at  │         │ RECON_MISMATCHES │         │    report_url        │
    │    completed_at  │         ├──────────────────┤         │    status            │
    │    failure_count │         │ PK id            │         │    created_at        │
    │    created_at    │         │ FK recon_run_id  │         └──────────────────────┘
    └──────────────────┘         │    source        │                    │
                                 │    reference     │                    │
                                 │    expected_amt  │◄───────────────────┘
                                 │    actual_amt    │
                                 │    variance      │
                                 │    resolved      │
                                 │    resolved_by   │
                                 │    resolution    │
                                 │    created_at    │
                                 └──────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                    AUTHENTICATION & AUDIT                                                    │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

    ┌──────────────────┐         ┌──────────────────┐         ┌──────────────────┐
    │  REFRESH_TOKENS  │         │   OTP_REQUESTS   │         │   AUDIT_LOGS     │
    ├──────────────────┤         ├──────────────────┤         ├──────────────────┤
    │ PK id            │         │ PK id            │         │ PK id            │
    │    user_type     │         │    phone         │         │    user_type     │
    │    user_id       │         │    otp_hash      │         │    user_id       │
    │    token_hash    │         │    purpose       │         │    action        │
    │    device_id     │         │    attempts      │         │    ip_address    │
    │    device_info   │         │    expires_at    │         │    device_info   │
    │    expires_at    │         │    verified      │         │    outcome       │
    │    revoked       │         │    created_at    │         │    details       │
    │    created_at    │         └──────────────────┘         │    created_at    │
    └──────────────────┘                                      └──────────────────┘

    ┌──────────────────┐         ┌──────────────────┐
    │ BUYER_SESSIONS   │         │ IDEMPOTENCY_KEYS │
    ├──────────────────┤         ├──────────────────┤
    │ PK id            │         │ PK key           │
    │ FK buyer_id      │         │    endpoint      │
    │    device_id     │         │    response      │
    │    device_finger │         │    status_code   │
    │    ip_address    │         │    expires_at    │
    │    created_at    │         │    created_at    │
    │    last_active   │         └──────────────────┘
    │    revoked       │
    └──────────────────┘
    (max 2 active per buyer)

┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                       NOTIFICATIONS                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

    ┌──────────────────┐         ┌──────────────────┐         ┌──────────────────┐
    │ NOTIF_TEMPLATES  │         │  NOTIFICATIONS   │         │ NOTIF_PREFERENCES│
    ├──────────────────┤         ├──────────────────┤         ├──────────────────┤
    │ PK id            │◄────────│ FK template_id   │         │ PK id            │
    │    name          │         │ PK id            │         │    user_type     │
    │    channel       │         │    recipient_type│         │    user_id       │
    │    language      │         │    recipient_id  │         │    channel       │
    │    subject       │         │    channel       │         │    type          │
    │    body_template │         │    priority      │         │    enabled       │
    │    variables[]   │         │    payload       │         │    created_at    │
    │    status        │         │    status        │         └──────────────────┘
    │    created_at    │         │    sent_at       │
    └──────────────────┘         │    delivered_at  │
                                 │    error_msg     │
                                 │    created_at    │
                                 └──────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                       FILE STORAGE                                                           │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

    ┌──────────────────┐
    │   FILE_UPLOADS   │
    ├──────────────────┤
    │ PK id            │
    │    bucket        │  (receipts, delivery-photos, reconciliation-reports)
    │    object_key    │
    │    original_name │
    │    content_type  │
    │    size_bytes    │
    │    uploaded_by   │
    │    reference_type│
    │    reference_id  │
    │    created_at    │
    └──────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                    RELATIONSHIPS SUMMARY                                                     │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

    SELLER ─────1:N───── SELLER_USERS
    SELLER ─────1:N───── PRODUCTS
    SELLER ─────1:N───── DELIVERY_AGENTS
    SELLER ─────1:N───── SETTLEMENTS
    SELLER ─────1:N───── ORDERS (as seller)

    BUYER ──────1:N───── ORDERS (as buyer)
    BUYER ──────1:N───── BUYER_SESSIONS
    BUYER ──────N:M───── PRODUCTS (via BUYER_FAVORITES)

    PRODUCT ────1:N───── PRICE_HISTORY
    PRODUCT ────N:1───── CATEGORY

    ORDER ──────1:N───── ORDER_ITEMS
    ORDER ──────1:N───── ORDER_HISTORY
    ORDER ──────1:N───── PAYMENTS
    ORDER ──────1:1───── DELIVERY_TRACKING

    SETTLEMENT ─1:N───── SETTLEMENT_ITEMS
    SETTLEMENT_ITEM─N:1─ ORDER

    RECON_RUN ──1:N───── RECON_MISMATCHES

┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                    INDEXES STRATEGY                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

    HIGH-FREQUENCY QUERIES:
    ├── orders: (buyer_id, status) - buyer order list
    ├── orders: (seller_id, status, created_at) - seller dashboard
    ├── orders: (idempotency_key) - UNIQUE, deduplication
    ├── orders: (expires_at) WHERE status = 'PAYMENT_PENDING' - expiry job
    ├── products: (seller_id, status) - seller product list
    ├── price_history: (product_id, effective_from, effective_to) - current price lookup
    ├── ledger_entries: (txn_id) - transaction grouping
    ├── ledger_entries: (account_type, entity_id, created_at) - balance queries
    ├── payments: (gateway_payment_id) - UNIQUE, webhook dedup
    ├── settlements: (seller_id, status) - settlement queries
    ├── buyer_sessions: (buyer_id, revoked) - active session check
    ├── otp_requests: (phone, created_at) - rate limiting
    └── delivery_tracking: (order_id) - tracking lookup

    PARTIAL INDEXES:
    ├── orders WHERE status NOT IN ('SETTLED', 'REFUNDED', 'SELLER_REJECTED') - active orders
    ├── settlements WHERE status = 'PENDING' - pending payouts
    └── products WHERE deleted_at IS NULL - active products

┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                    PARTITIONING STRATEGY                                                     │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

    LEDGER_ENTRIES:
    └── Range partition by created_at (monthly)
        ├── ledger_entries_2024_01
        ├── ledger_entries_2024_02
        └── ... (7 year retention)

    AUDIT_LOGS:
    └── Range partition by created_at (monthly)

    NOTIFICATIONS:
    └── Range partition by created_at (monthly), auto-drop after 90 days

┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                    DATA TYPES & CONSTRAINTS                                                  │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

    MONEY:          DECIMAL(12, 2) - supports up to ₹9,99,99,99,999.99
    QUANTITY:       DECIMAL(10, 3) - supports kg with grams precision
    UUID:           Used for all primary keys (distributed-safe)
    TIMESTAMPS:     TIMESTAMPTZ (with timezone, stored as UTC)
    ENUMS:          PostgreSQL native ENUMs for status fields
    JSON:           JSONB for flexible data (addresses, price_snapshot, bulk_slabs)
    ENCRYPTED:      Phone numbers, addresses encrypted at rest (pgcrypto)

    KEY CONSTRAINTS:
    ├── orders.idempotency_key: UNIQUE
    ├── payments.gateway_payment_id: UNIQUE
    ├── buyers.phone: UNIQUE (after decryption check in app layer)
    ├── sellers.gstin: UNIQUE
    ├── products.sku: UNIQUE per seller (seller_id, sku)
    └── ledger_entries: NO UPDATE/DELETE triggers (append-only enforcement)
```

## Key Design Decisions

### 1. Double-Entry Ledger
- Every financial movement creates balanced debit/credit entries
- Append-only design prevents tampering
- Easy reconciliation and audit trail

### 2. Price Snapshot Strategy
- `price_snapshot` JSONB stored on order at creation time
- Immutable - original prices preserved for disputes
- Includes bulk slab calculations applied

### 3. Optimistic Locking
- `version` column on orders prevents race conditions
- UPDATE ... WHERE version = ? pattern

### 4. Soft Deletes
- Products use `deleted_at` - maintain history
- Ledger entries never deleted - append corrections instead

### 5. Session Management
- Buyer: max 2 sessions enforced at application layer
- Device fingerprint binding for fraud prevention

### 6. Encryption at Rest
- Phone numbers and addresses encrypted using pgcrypto
- Decryption only in application layer with proper access

## Approval Required

Please review this ER diagram and confirm:
1. Are the relationships correctly modeled?
2. Any missing entities or attributes?
3. Is the ledger design appropriate for your reconciliation needs?
4. Any concerns about the state machine for orders?

Once approved, I'll proceed with Flyway migration scripts.
