# Payment Issues Runbook

## Overview

This runbook covers troubleshooting procedures for payment-related issues in the Poultry Platform.

**Payment Provider**: Razorpay
**Payment Types**: UPI, Cards, Net Banking, Wallets
**Settlement**: Automated via Razorpay Route

---

## Table of Contents

1. [Quick Diagnostics](#quick-diagnostics)
2. [Payment Flow Overview](#payment-flow-overview)
3. [Payment Creation Issues](#payment-creation-issues)
4. [Payment Capture Issues](#payment-capture-issues)
5. [Webhook Issues](#webhook-issues)
6. [Settlement Issues](#settlement-issues)
7. [Refund Issues](#refund-issues)
8. [Reconciliation Issues](#reconciliation-issues)

---

## Quick Diagnostics

### Health Check

```bash
# Check payment service health
curl -s https://api.poultry-platform.com/api/actuator/health | jq '.components.circuitBreakers.details.razorpay'

# Check Razorpay status
curl -s https://status.razorpay.com/api/v2/status.json | jq

# Check payment-related logs
kubectl logs deployment/poultry-backend -n poultry-platform | grep -i "payment\|razorpay" | tail -50
```

### Razorpay Dashboard Checks

1. **Payment Status**: [Dashboard > Payments](https://dashboard.razorpay.com/app/payments)
2. **Webhook Logs**: [Dashboard > Webhooks](https://dashboard.razorpay.com/app/webhooks)
3. **Settlement Status**: [Dashboard > Settlements](https://dashboard.razorpay.com/app/settlements)
4. **API Health**: [Status Page](https://status.razorpay.com)

---

## Payment Flow Overview

```
1. Order Created
   └─> Order in PENDING state

2. Payment Initiated (Buyer)
   └─> Razorpay Order created
   └─> Payment link/checkout displayed

3. Payment Authorized
   └─> Webhook: payment.authorized
   └─> Order moves to PAYMENT_PENDING

4. Payment Captured (Auto/Manual)
   └─> Webhook: payment.captured
   └─> Order moves to CONFIRMED
   └─> Inventory reserved

5. Settlement
   └─> Razorpay Route splits payment
   └─> Platform fee deducted
   └─> Seller balance updated

6. Payout
   └─> Settlement transferred to seller bank
```

---

## Payment Creation Issues

### Issue: Order Creation Fails

**Symptoms**:
- 500 error when creating order
- "Unable to create Razorpay order"

**Diagnosis**:
```bash
# Check logs for order creation
kubectl logs deployment/poultry-backend -n poultry-platform | \
  grep -i "razorpay.*order" | tail -20

# Verify Razorpay credentials
kubectl get secret razorpay-credentials -n poultry-platform -o yaml

# Test Razorpay connectivity
kubectl exec -it deployment/poultry-backend -n poultry-platform -- \
  curl -v https://api.razorpay.com/v1/orders \
  -u <key_id>: \
  -H "Content-Type: application/json" \
  -d '{"amount":100,"currency":"INR","receipt":"test"}'
```

**Resolution**:
1. Verify API credentials are correct
2. Check if Razorpay account is active
3. Verify order amount meets minimum requirements (INR 1.00)
4. Check circuit breaker status and reset if needed

### Issue: Invalid Amount Error

**Symptoms**:
- "Amount must be greater than 0"
- "Amount exceeds maximum limit"

**Resolution**:
1. Verify amount calculation logic
2. Check for currency conversion issues
3. Minimum: INR 1.00 (100 paise)
4. Maximum: Check merchant limit in Razorpay dashboard

---

## Payment Capture Issues

### Issue: Payment Not Being Captured

**Symptoms**:
- Payment shows as "Authorized" but not "Captured"
- Order stuck in PAYMENT_PENDING state

**Diagnosis**:
```bash
# Check payment status in database
kubectl exec -it postgres-0 -n poultry-platform -- psql -U postgres -d poultry -c \
  "SELECT id, razorpay_payment_id, status, created_at FROM payments WHERE status = 'AUTHORIZED' ORDER BY created_at DESC LIMIT 10;"

# Check capture logs
kubectl logs deployment/poultry-backend -n poultry-platform | \
  grep -i "capture" | tail -20

# Verify payment in Razorpay
curl -u <key_id>:<key_secret> \
  https://api.razorpay.com/v1/payments/<payment_id>
```

**Resolution**:
1. **Manual capture** if auto-capture failed:
   ```bash
   curl -X POST -u <key_id>:<key_secret> \
     https://api.razorpay.com/v1/payments/<payment_id>/capture \
     -H "Content-Type: application/json" \
     -d '{"amount":<amount_in_paise>}'
   ```

2. Check if payment window has expired (5 days default)

3. Update order status manually if capture succeeded but webhook failed:
   ```sql
   UPDATE payments SET status = 'CAPTURED', updated_at = NOW()
   WHERE razorpay_payment_id = '<payment_id>';

   UPDATE orders SET status = 'CONFIRMED', updated_at = NOW()
   WHERE id = (SELECT order_id FROM payments WHERE razorpay_payment_id = '<payment_id>');
   ```

### Issue: Auto-Capture Disabled

**Symptoms**:
- All payments require manual capture
- Customer complaints about delayed order confirmation

**Resolution**:
1. Enable auto-capture in Razorpay dashboard
2. Or implement auto-capture in application after payment.authorized webhook

---

## Webhook Issues

### Issue: Webhooks Not Being Received

**Symptoms**:
- Payment status not updating
- Missing payment events in logs

**Diagnosis**:
```bash
# Check webhook endpoint health
curl -X POST https://api.poultry-platform.com/api/webhooks/razorpay \
  -H "Content-Type: application/json" \
  -d '{"event":"test"}'

# Check webhook logs
kubectl logs deployment/poultry-backend -n poultry-platform | \
  grep -i "webhook" | tail -30

# Verify webhook secret
kubectl get secret razorpay-credentials -n poultry-platform -o jsonpath='{.data.webhook-secret}' | base64 -d
```

**Resolution**:
1. Verify webhook URL in Razorpay dashboard matches your endpoint
2. Ensure webhook secret is correctly configured
3. Check firewall rules allow Razorpay IPs
4. Verify SSL certificate is valid

### Issue: Webhook Signature Verification Failed

**Symptoms**:
- 401 responses to webhook calls
- "Invalid signature" in logs

**Diagnosis**:
```bash
# Check for signature errors
kubectl logs deployment/poultry-backend -n poultry-platform | \
  grep -i "signature" | tail -20
```

**Resolution**:
1. Regenerate webhook secret in Razorpay dashboard
2. Update secret in Kubernetes:
   ```bash
   kubectl create secret generic razorpay-credentials \
     --from-literal=webhook-secret=<new_secret> \
     --dry-run=client -o yaml | kubectl apply -f -
   ```
3. Restart application to pick up new secret

### Issue: Webhook Processing Errors

**Symptoms**:
- Webhooks received but orders not updated
- Duplicate processing

**Diagnosis**:
```bash
# Check for processing errors
kubectl logs deployment/poultry-backend -n poultry-platform | \
  grep -E "webhook.*error|payment.*exception" | tail -30

# Check idempotency
kubectl exec -it postgres-0 -n poultry-platform -- psql -U postgres -d poultry -c \
  "SELECT razorpay_payment_id, count(*) FROM payments GROUP BY razorpay_payment_id HAVING count(*) > 1;"
```

**Resolution**:
1. Implement idempotent webhook processing
2. Add retry logic with exponential backoff
3. Use database transactions for state updates

---

## Settlement Issues

### Issue: Seller Not Receiving Settlement

**Symptoms**:
- Payment captured but seller balance not updated
- Settlement missing in Razorpay dashboard

**Diagnosis**:
```bash
# Check settlement records
kubectl exec -it postgres-0 -n poultry-platform -- psql -U postgres -d poultry -c \
  "SELECT o.id, o.total_amount, o.platform_fee, o.seller_amount, s.status as settlement_status
   FROM orders o
   LEFT JOIN settlements s ON o.id = s.order_id
   WHERE o.status = 'CONFIRMED' AND s.status != 'COMPLETED'
   ORDER BY o.created_at DESC LIMIT 10;"

# Check Razorpay Route configuration
curl -u <key_id>:<key_secret> \
  https://api.razorpay.com/v1/route/transfers \
  -d 'payment_id=<payment_id>'
```

**Resolution**:
1. Verify Route is enabled for merchant account
2. Check seller's Razorpay linked account status
3. Manually trigger settlement if auto-settlement failed:
   ```bash
   curl -X POST -u <key_id>:<key_secret> \
     https://api.razorpay.com/v1/payments/<payment_id>/transfers \
     -H "Content-Type: application/json" \
     -d '{
       "transfers": [{
         "account": "<seller_razorpay_account_id>",
         "amount": <amount_in_paise>,
         "currency": "INR"
       }]
     }'
   ```

### Issue: Incorrect Platform Fee Calculation

**Symptoms**:
- Seller receiving wrong amount
- Platform fee not matching expected percentage

**Diagnosis**:
```sql
-- Check fee calculations
SELECT id, total_amount, platform_fee, seller_amount,
       ROUND(total_amount * 0.02, 2) as expected_fee,
       ABS(platform_fee - ROUND(total_amount * 0.02, 2)) as fee_diff
FROM orders
WHERE ABS(platform_fee - ROUND(total_amount * 0.02, 2)) > 1
ORDER BY created_at DESC
LIMIT 10;
```

**Resolution**:
1. Verify platform fee percentage in configuration
2. Check for rounding errors in calculations
3. Reconcile and adjust manually if needed

---

## Refund Issues

### Issue: Refund Failing

**Symptoms**:
- Refund requests returning errors
- Customer complaints about pending refunds

**Diagnosis**:
```bash
# Check refund logs
kubectl logs deployment/poultry-backend -n poultry-platform | \
  grep -i "refund" | tail -30

# Verify payment is eligible for refund
curl -u <key_id>:<key_secret> \
  https://api.razorpay.com/v1/payments/<payment_id>
```

**Resolution**:
1. **Verify payment is captured** (can't refund authorized payments)

2. **Check refund window** (180 days from payment date)

3. **Initiate manual refund**:
   ```bash
   curl -X POST -u <key_id>:<key_secret> \
     https://api.razorpay.com/v1/payments/<payment_id>/refund \
     -H "Content-Type: application/json" \
     -d '{"amount":<amount_in_paise>}'
   ```

4. **Check for partial refund limits**

### Issue: Refund Processing Slowly

**Symptoms**:
- Refund initiated but funds not returned
- Customer queries about refund status

**Resolution**:
1. Refund timelines by payment method:
   - UPI: 5-7 business days
   - Cards: 5-10 business days
   - Net Banking: 5-7 business days
   - Wallets: Instant to 3 days

2. Check refund status in Razorpay dashboard
3. If delayed beyond SLA, contact Razorpay support

---

## Reconciliation Issues

### Issue: Payment Records Mismatch

**Symptoms**:
- Database records don't match Razorpay
- Financial reports showing discrepancies

**Diagnosis**:
```bash
# Generate reconciliation report
kubectl exec -it postgres-0 -n poultry-platform -- psql -U postgres -d poultry -c \
  "SELECT DATE(created_at),
          COUNT(*) as total_payments,
          SUM(CASE WHEN status = 'CAPTURED' THEN 1 ELSE 0 END) as captured,
          SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed,
          SUM(amount) as total_amount
   FROM payments
   WHERE created_at >= NOW() - INTERVAL '7 days'
   GROUP BY DATE(created_at)
   ORDER BY DATE(created_at) DESC;"

# Export Razorpay transactions
curl -u <key_id>:<key_secret> \
  "https://api.razorpay.com/v1/payments?from=<timestamp>&to=<timestamp>&count=100"
```

**Resolution**:
1. Run daily reconciliation job
2. Identify and fix missing webhook events
3. Update database records from Razorpay API
4. Generate exception report for manual review

### Issue: Duplicate Payments

**Symptoms**:
- Same order charged multiple times
- Customer complaints about double charges

**Diagnosis**:
```sql
-- Find duplicate payments
SELECT order_id, COUNT(*) as payment_count,
       SUM(amount) as total_charged
FROM payments
WHERE status = 'CAPTURED'
GROUP BY order_id
HAVING COUNT(*) > 1;
```

**Resolution**:
1. Identify duplicate payment
2. Process refund for duplicate
3. Update order status if needed
4. Investigate root cause (frontend/backend issue)

---

## Emergency Procedures

### Payment System Down

1. **Enable maintenance mode** for payment features
2. **Notify customers** about payment issues
3. **Queue payments** for later processing if possible
4. **Monitor Razorpay status page**
5. **Prepare for manual processing** if extended outage

### Mass Refund Required

```bash
# Export affected payments
kubectl exec -it postgres-0 -n poultry-platform -- psql -U postgres -d poultry -c \
  "COPY (SELECT razorpay_payment_id, amount FROM payments WHERE <criteria>) TO '/tmp/refunds.csv' CSV HEADER;"

# Process refunds in batches
for payment_id in $(cat refund_list.txt); do
  curl -X POST -u <key_id>:<key_secret> \
    https://api.razorpay.com/v1/payments/$payment_id/refund
  sleep 1  # Rate limit
done
```

---

## Monitoring Queries

### Payment Health Dashboard

```sql
-- Payment success rate (last hour)
SELECT
    COUNT(*) FILTER (WHERE status = 'CAPTURED') * 100.0 / COUNT(*) as success_rate,
    COUNT(*) as total_attempts,
    COUNT(*) FILTER (WHERE status = 'CAPTURED') as successful,
    COUNT(*) FILTER (WHERE status = 'FAILED') as failed
FROM payments
WHERE created_at >= NOW() - INTERVAL '1 hour';

-- Payment by method
SELECT payment_method, COUNT(*),
       AVG(EXTRACT(EPOCH FROM (captured_at - created_at))) as avg_capture_time_seconds
FROM payments
WHERE created_at >= NOW() - INTERVAL '24 hours'
GROUP BY payment_method;

-- Settlement status
SELECT status, COUNT(*), SUM(amount)
FROM settlements
WHERE created_at >= NOW() - INTERVAL '7 days'
GROUP BY status;
```

---

## Escalation

1. **Internal Issues**: Escalate to Payment Team Lead
2. **Razorpay Issues**: Contact Razorpay Support
   - Email: support@razorpay.com
   - Dashboard: Support ticket
3. **Financial Discrepancies**: Escalate to Finance Team

## Related Runbooks

- [API Issues](./API_ISSUES.md)
- [Database Issues](./DATABASE_ISSUES.md)
- [Kafka Issues](./KAFKA_ISSUES.md) (for payment events)
