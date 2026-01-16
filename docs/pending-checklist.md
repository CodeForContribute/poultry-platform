# Pending Checklist (vs Prompt)

This checklist maps the current repo to the requested execution order and highlights what’s still missing or mismatched.

## 1) DB schema + Flyway migrations

Done (core): sellers/users/admins, buyers + sessions, products/categories/pricing + favorites, orders/items/history, payments, ledger, settlements/reconciliation, delivery tracking/proof, notifications, audit logs, idempotency keys.

Pending:
- Align infra versions to prompt (PostgreSQL 15, Kafka 3.x) and verify migrations on those versions.
- Add missing operational migrations/jobs (e.g., future partitions beyond 2026 for `audit_logs`/`notifications`).

## 2) Auth service + JWT

Done (core): seller password login + refresh tokens, buyer OTP + max sessions, OTP rate limiting, audit logging.

Pending:
- Enforce buyer device fingerprint binding rules (currently stored but not enforced).
- Wire OTP delivery via `SmsProvider` (MSG91/Twilio) and templates; remove “log-only” OTP behavior for prod.

## 3) Product & pricing CRUD

Done (core): categories, products, soft delete, favorites, bulk slabs, price history.

Pending:
- Scheduled pricing activation notifications (send when a scheduled price becomes effective).

## 4) Order service with state machine

Done (core): strict state machine (app + DB), optimistic locking, immutable `price_snapshot`.

Pending:
- Payment timeout auto-cancel job (30 min after `SELLER_CONFIRMED`).
- Partial delivery workflows fully wired (DB supports item status; confirm API supports per-item delivery updates).

## 5) Payment integration + webhooks

Done (core): Razorpay order creation, webhook handler, webhook dedupe.

Pending:
- Webhook signature verification should use raw request body (current handler uses `payload.toString()`).
- Polling fallback, double-payment auto-refund, and a durable saga/manual-review queue for “paid but order update failed”.
- Receipt PDF generation + MinIO storage + WhatsApp/SMS delivery.

## 6) Ledger service + reconciliation job

Done (core): double-entry ledger + append-only enforcement, basic reconciliation (manual processing).

Pending:
- 2 AM IST scheduled reconciliation job + report generation + mismatch queue (partially scaffolded).
- Settlement scheduler (T+1/T+2/weekly), payout retries, and “arbitrage detection” alerts.

## 7) Delivery tracking

Done (core): delivery tracking tables + API scaffolding.

Pending:
- Buyer OTP/photo proof flow validation + retry rules enforcement + SLA breach flagging.

## 8) Seller web app

Mismatch: `seller-web/` is Next.js (not Vite) and lint currently fails.

## 9) Buyer mobile app

Mismatch: `buyer-mobile/` is not a React Native app; there are native `buyer-android/` and `buyer-ios/` apps.

## 10) Admin dashboard

Partially done: admin controllers/services exist; validate coverage of reconciliation review queue + metrics.

## 11) Notifications

Partially done: notification templates/providers exist; Kafka event plumbing exists for price-change.

Pending:
- Priority queueing, unsubscribe handling, and idempotent client delivery semantics.

## 12) Load testing + hardening

Pending:
- k6/Gatling scenarios, API contract tests, and raising service coverage thresholds (JaCoCo is 50% today).

