# Secret Rotation Guide

This document outlines procedures for rotating secrets in the Poultry Platform with zero downtime.

## Table of Contents

1. [Overview](#overview)
2. [Secret Types and Rotation Schedules](#secret-types-and-rotation-schedules)
3. [Pre-Rotation Checklist](#pre-rotation-checklist)
4. [Rotation Procedures](#rotation-procedures)
5. [Zero-Downtime Rotation](#zero-downtime-rotation)
6. [Automated Rotation](#automated-rotation)
7. [Emergency Rotation](#emergency-rotation)
8. [Rollback Procedures](#rollback-procedures)
9. [Audit and Compliance](#audit-and-compliance)

---

## Overview

Secret rotation is a critical security practice that involves periodically changing sensitive credentials to:
- Limit the impact of potential credential leaks
- Comply with security policies and regulations
- Reduce the attack surface over time

### Incident Response Rotation Checklist (Immediate)

Use this checklist after a suspected exposure or accidental commit.

- [ ] Revoke and rotate database user/passwords (PostgreSQL)
- [ ] Rotate Redis password and update dependent services
- [ ] Rotate MinIO access/secret keys and update storage clients
- [ ] Rotate JWT secret and force token invalidation
- [ ] Rotate encryption key and plan re-encryption if required
- [ ] Rotate Razorpay key/secret and webhook secret
- [ ] Rotate SMS provider credentials (MSG91/Twilio as used)
- [ ] Rotate SMTP credentials (if used)
- [ ] Rotate Firebase service account key (FCM)
- [ ] Validate External Secrets sync and restart workloads
- [ ] Verify auth, payments, and notifications in staging
- [ ] Remove any leaked artifacts from git history and remotes

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Secret Rotation Flow                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    ┌─────────────────┐    ┌───────────────┐  │
│  │   Secrets    │───▶│  External       │───▶│  Kubernetes   │  │
│  │   Manager    │    │  Secrets        │    │  Secrets      │  │
│  │  (AWS/Vault) │    │  Operator       │    │               │  │
│  └──────────────┘    └─────────────────┘    └───────────────┘  │
│         │                    │                      │           │
│         ▼                    ▼                      ▼           │
│  ┌──────────────┐    ┌─────────────────┐    ┌───────────────┐  │
│  │   New        │    │  Sync           │    │  Pod          │  │
│  │   Secret     │───▶│  (1h interval)  │───▶│  Restart      │  │
│  │   Version    │    │                 │    │  (Reloader)   │  │
│  └──────────────┘    └─────────────────┘    └───────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Secret Types and Rotation Schedules

| Secret Type | Rotation Frequency | Rotation Window | Priority |
|-------------|-------------------|-----------------|----------|
| JWT Secret | 90 days | Off-peak hours | High |
| Encryption Keys | 365 days | Maintenance window | Critical |
| Database Passwords | 90 days | Off-peak hours | High |
| Redis Password | 90 days | Off-peak hours | Medium |
| Razorpay API Keys | 180 days | Business hours | High |
| Razorpay Webhook Secret | 90 days | Off-peak hours | High |
| MSG91 Auth Key | 365 days | Any time | Low |
| MinIO Access Keys | 90 days | Off-peak hours | Medium |

### Rotation Windows

- **Maintenance Window**: Scheduled maintenance periods with potential brief downtime
- **Off-Peak Hours**: 2:00 AM - 5:00 AM IST (weekdays)
- **Business Hours**: 10:00 AM - 6:00 PM IST (for vendor coordination)

---

## Pre-Rotation Checklist

Before rotating any secret:

- [ ] Verify current secret is working correctly
- [ ] Ensure External Secrets Operator is healthy
- [ ] Check pod readiness and health
- [ ] Verify backup of current secret exists
- [ ] Notify relevant team members
- [ ] Prepare rollback procedure
- [ ] Test in staging environment first

### Health Checks

```bash
# Check External Secrets Operator status
kubectl get pods -n external-secrets

# Verify ExternalSecret sync status
kubectl get externalsecret -n poultry-platform

# Check current secret sync time
kubectl get externalsecret poultry-app-secrets -n poultry-platform -o jsonpath='{.status.conditions}'
```

---

## Rotation Procedures

### 1. JWT Secret Rotation

**Impact**: All active sessions will be invalidated after rotation.

**Procedure**:

1. **Generate new JWT secret**:
   ```bash
   # Generate 256-bit secret
   openssl rand -base64 32
   ```

2. **Update in AWS Secrets Manager**:
   ```bash
   aws secretsmanager put-secret-value \
     --secret-id poultry-platform/production/app \
     --secret-string "$(jq -n --arg jwt "$(openssl rand -base64 32)" \
       --slurpfile current <(aws secretsmanager get-secret-value \
         --secret-id poultry-platform/production/app \
         --query SecretString --output text) \
       '$current[0] | .jwt_secret = $jwt')"
   ```

3. **Or update in HashiCorp Vault**:
   ```bash
   vault kv patch secret/poultry-platform/app \
     jwt_secret="$(openssl rand -base64 32)"
   ```

4. **Trigger External Secrets sync**:
   ```bash
   # Force immediate sync
   kubectl annotate externalsecret poultry-app-secrets -n poultry-platform \
     force-sync=$(date +%s) --overwrite
   ```

5. **Verify rotation**:
   ```bash
   # Check secret was updated
   kubectl get secret poultry-secrets -n poultry-platform \
     -o jsonpath='{.metadata.annotations.external-secrets\.io/last-refresh}'
   ```

6. **Rolling restart pods**:
   ```bash
   # If using Stakater Reloader, pods restart automatically
   # Otherwise, manually trigger rolling restart
   kubectl rollout restart deployment/poultry-backend -n poultry-platform
   ```

---

### 2. Database Password Rotation (Zero-Downtime)

**Important**: Database password rotation requires coordination with the database.

**Procedure**:

1. **Create new database user (dual-credential approach)**:
   ```sql
   -- Connect to PostgreSQL
   CREATE USER poultry_prod_new WITH PASSWORD 'new_secure_password';
   GRANT ALL PRIVILEGES ON DATABASE poultry TO poultry_prod_new;
   GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO poultry_prod_new;
   GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO poultry_prod_new;
   ```

2. **Update secret with new credentials**:
   ```bash
   aws secretsmanager put-secret-value \
     --secret-id poultry-platform/production/database/postgres \
     --secret-string '{"username":"poultry_prod_new","password":"new_secure_password"}'
   ```

3. **Wait for External Secrets sync** (or force sync):
   ```bash
   kubectl annotate externalsecret poultry-database-secrets -n poultry-platform \
     force-sync=$(date +%s) --overwrite
   ```

4. **Rolling restart application**:
   ```bash
   kubectl rollout restart deployment/poultry-backend -n poultry-platform
   kubectl rollout status deployment/poultry-backend -n poultry-platform
   ```

5. **Verify connections with new credentials**:
   ```bash
   # Check logs for successful database connections
   kubectl logs -l app.kubernetes.io/name=poultry-backend -n poultry-platform | grep -i "database"
   ```

6. **Remove old database user** (after verification period):
   ```sql
   -- After 24-48 hours of successful operation
   REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM poultry_prod;
   REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM poultry_prod;
   REVOKE ALL PRIVILEGES ON DATABASE poultry FROM poultry_prod;
   DROP USER poultry_prod;
   ```

---

### 3. Razorpay API Key Rotation

**Important**: Razorpay key rotation requires coordination with Razorpay dashboard.

**Procedure**:

1. **Generate new API key in Razorpay Dashboard**:
   - Log in to https://dashboard.razorpay.com
   - Navigate to Settings > API Keys
   - Generate new key pair (keep old key active)

2. **Update secret with new credentials**:
   ```bash
   aws secretsmanager put-secret-value \
     --secret-id poultry-platform/production/payment/razorpay \
     --secret-string '{
       "enabled": "true",
       "key_id": "rzp_live_NEW_KEY_ID",
       "key_secret": "NEW_KEY_SECRET",
       "webhook_secret": "EXISTING_WEBHOOK_SECRET",
       "account_number": "EXISTING_ACCOUNT_NUMBER"
     }'
   ```

3. **Trigger sync and restart**:
   ```bash
   kubectl annotate externalsecret poultry-payment-secrets -n poultry-platform \
     force-sync=$(date +%s) --overwrite
   kubectl rollout restart deployment/poultry-backend -n poultry-platform
   ```

4. **Verify payment processing**:
   ```bash
   # Test payment endpoint
   curl -X POST https://api.poultry-platform.com/api/health/payment-gateway
   ```

5. **Deactivate old API key** in Razorpay Dashboard (after 24 hours).

---

### 4. Encryption Key Rotation

**Critical**: Encryption key rotation requires re-encryption of existing data.

**Procedure**:

1. **Generate new encryption key**:
   ```bash
   openssl rand -base64 32
   ```

2. **Update secret with both old and new keys**:
   ```bash
   # Store previous key for decryption of existing data
   aws secretsmanager put-secret-value \
     --secret-id poultry-platform/production/encryption/bank \
     --secret-string '{
       "encryption_key": "NEW_256_BIT_KEY",
       "kek": "EXISTING_KEK",
       "encryption_key_previous": "OLD_256_BIT_KEY"
     }'
   ```

3. **Deploy application update** that supports dual-key decryption:
   ```bash
   kubectl rollout restart deployment/poultry-backend -n poultry-platform
   ```

4. **Run re-encryption job**:
   ```bash
   kubectl apply -f - <<EOF
   apiVersion: batch/v1
   kind: Job
   metadata:
     name: reencrypt-bank-accounts
     namespace: poultry-platform
   spec:
     template:
       spec:
         containers:
         - name: reencrypt
           image: poultry-platform/backend:latest
           command: ["java", "-jar", "app.jar", "--reencrypt-bank-accounts"]
           envFrom:
           - secretRef:
               name: poultry-secrets
           - secretRef:
               name: poultry-database-secrets
         restartPolicy: Never
     backoffLimit: 0
   EOF
   ```

5. **Verify re-encryption complete**:
   ```bash
   kubectl logs job/reencrypt-bank-accounts -n poultry-platform
   ```

6. **Remove previous key** (after verification):
   ```bash
   aws secretsmanager put-secret-value \
     --secret-id poultry-platform/production/encryption/bank \
     --secret-string '{
       "encryption_key": "NEW_256_BIT_KEY",
       "kek": "EXISTING_KEK",
       "encryption_key_previous": ""
     }'
   ```

---

## Zero-Downtime Rotation

### Using Stakater Reloader

The deployment is configured to automatically restart when secrets change:

```yaml
metadata:
  annotations:
    secret.reloader.stakater.com/reload: "poultry-secrets,poultry-database-secrets,poultry-payment-secrets"
```

### Rolling Update Strategy

Ensure deployment has proper rolling update configuration:

```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
```

### Dual-Credential Support

For critical secrets, implement dual-credential support in the application:

```java
// Example: JWT validation with dual-secret support
public class JwtService {
    @Value("${JWT_SECRET}")
    private String currentSecret;

    @Value("${JWT_SECRET_PREVIOUS:}")
    private String previousSecret;

    public boolean validateToken(String token) {
        // Try current secret first
        if (validateWithSecret(token, currentSecret)) {
            return true;
        }
        // Fall back to previous secret during rotation
        if (!previousSecret.isEmpty()) {
            return validateWithSecret(token, previousSecret);
        }
        return false;
    }
}
```

---

## Automated Rotation

### AWS Secrets Manager Automatic Rotation

Enable automatic rotation for supported secrets:

```bash
# Create rotation Lambda function
aws lambda create-function \
  --function-name poultry-secret-rotation \
  --runtime python3.9 \
  --handler rotation.handler \
  --role arn:aws:iam::123456789012:role/rotation-lambda-role \
  --code S3Bucket=lambda-functions,S3Key=rotation.zip

# Enable rotation
aws secretsmanager rotate-secret \
  --secret-id poultry-platform/production/database/postgres \
  --rotation-lambda-arn arn:aws:lambda:ap-south-1:123456789012:function:poultry-secret-rotation \
  --rotation-rules AutomaticallyAfterDays=90
```

### HashiCorp Vault Dynamic Secrets

For database credentials, use Vault's database secrets engine:

```bash
# Enable database secrets engine
vault secrets enable database

# Configure PostgreSQL connection
vault write database/config/poultry-postgres \
    plugin_name=postgresql-database-plugin \
    allowed_roles="poultry-app" \
    connection_url="postgresql://{{username}}:{{password}}@postgres-service:5432/poultry" \
    username="vault_admin" \
    password="vault_admin_password"

# Create role with dynamic credentials
vault write database/roles/poultry-app \
    db_name=poultry-postgres \
    creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; \
        GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="24h"
```

---

## Emergency Rotation

In case of suspected credential compromise:

### Immediate Actions

1. **Identify compromised secret(s)**
2. **Generate new credentials immediately**
3. **Update secret store**:
   ```bash
   # AWS Secrets Manager
   aws secretsmanager put-secret-value \
     --secret-id poultry-platform/production/app \
     --secret-string '{"jwt_secret":"NEW_EMERGENCY_SECRET"}'
   ```

4. **Force immediate sync**:
   ```bash
   # Delete and recreate ExternalSecret to force immediate sync
   kubectl delete externalsecret poultry-app-secrets -n poultry-platform
   kubectl apply -f k8s/external-secrets/poultry-secrets.yaml
   ```

5. **Force restart all pods**:
   ```bash
   kubectl rollout restart deployment/poultry-backend -n poultry-platform
   kubectl rollout status deployment/poultry-backend -n poultry-platform --timeout=300s
   ```

6. **Invalidate all sessions** (for JWT compromise):
   ```bash
   # If using Redis for session storage
   kubectl exec -it $(kubectl get pod -l app=redis -n poultry-platform -o name | head -1) \
     -n poultry-platform -- redis-cli FLUSHDB
   ```

7. **Audit and investigate**:
   - Check access logs
   - Review CloudTrail/audit logs
   - Document incident

---

## Rollback Procedures

### Rollback Secret Change

1. **Restore previous secret version** (AWS Secrets Manager):
   ```bash
   # List versions
   aws secretsmanager list-secret-version-ids \
     --secret-id poultry-platform/production/app

   # Restore previous version
   aws secretsmanager update-secret-version-stage \
     --secret-id poultry-platform/production/app \
     --version-stage AWSCURRENT \
     --move-to-version-id <previous-version-id>
   ```

2. **Restore from Vault** (if using Vault):
   ```bash
   # List versions
   vault kv metadata get secret/poultry-platform/app

   # Restore specific version
   vault kv rollback -version=<version-number> secret/poultry-platform/app
   ```

3. **Force sync and restart**:
   ```bash
   kubectl annotate externalsecret poultry-app-secrets -n poultry-platform \
     force-sync=$(date +%s) --overwrite
   kubectl rollout restart deployment/poultry-backend -n poultry-platform
   ```

---

## Audit and Compliance

### Rotation Tracking

Maintain a rotation log:

| Date | Secret | Rotated By | Reason | Verified |
|------|--------|------------|--------|----------|
| 2024-01-15 | JWT_SECRET | ops-team | Scheduled | Yes |
| 2024-01-15 | DB_PASSWORD | ops-team | Scheduled | Yes |

### Compliance Requirements

- **PCI-DSS**: Rotate encryption keys annually
- **SOC 2**: Document all rotation procedures
- **ISO 27001**: Maintain rotation schedules

### Monitoring and Alerts

Set up alerts for:
- Secrets approaching rotation deadline
- Failed rotation attempts
- ExternalSecret sync failures

```yaml
# PrometheusRule for secret rotation monitoring
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: secret-rotation-alerts
spec:
  groups:
    - name: secrets
      rules:
        - alert: ExternalSecretSyncFailed
          expr: external_secrets_sync_calls_total{status="error"} > 0
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: External Secret sync failed
            description: "ExternalSecret {{ $labels.name }} failed to sync"
```

---

## Contact Information

- **On-Call Team**: oncall@poultry-platform.com
- **Security Team**: security@poultry-platform.com
- **Slack Channel**: #platform-secrets

---

*Last Updated: January 2024*
*Document Owner: Platform Team*
