# Production Deployment Guide

This comprehensive guide covers all aspects of deploying the Poultry Platform to production. Follow each section carefully to ensure a successful deployment.

## Table of Contents

1. [Pre-Deployment Checklist](#pre-deployment-checklist)
2. [Environment Validation](#environment-validation)
3. [Database Migration Procedures](#database-migration-procedures)
4. [Deployment Sequence](#deployment-sequence)
5. [Smoke Test Procedures](#smoke-test-procedures)
6. [Rollback Procedures](#rollback-procedures)
7. [Post-Deployment Verification](#post-deployment-verification)

---

## Pre-Deployment Checklist

### 1. Code Readiness

- [ ] All feature branches merged to `main`
- [ ] All CI/CD pipeline checks passing
- [ ] Code review completed and approved
- [ ] Version tag created (e.g., `v1.0.0`)
- [ ] CHANGELOG updated with release notes
- [ ] No pending security vulnerabilities (run `./gradlew dependencyCheckAnalyze`)

### 2. Infrastructure Readiness

- [ ] Kubernetes cluster healthy (`kubectl get nodes`)
- [ ] Sufficient cluster resources (CPU, memory, storage)
- [ ] Load balancer configured and healthy
- [ ] SSL/TLS certificates valid and not expiring soon (> 30 days)
- [ ] DNS records configured correctly
- [ ] CDN cache rules configured (if applicable)

### 3. Secrets and Configuration

- [ ] All secrets rotated if needed
- [ ] JWT secret configured (256-bit minimum)
- [ ] Encryption key configured (256-bit)
- [ ] Database credentials verified
- [ ] Redis password configured
- [ ] MinIO/S3 credentials verified
- [ ] Third-party service credentials configured:
  - [ ] Razorpay API keys (live mode)
  - [ ] MSG91 or Twilio credentials
  - [ ] Firebase FCM credentials (if using push notifications)

### 4. External Services

- [ ] Razorpay webhooks configured with production URL
- [ ] SMS templates approved on MSG91/Twilio
- [ ] Firebase project configured for production
- [ ] SMTP server configured and tested

### 5. Monitoring and Alerting

- [ ] Prometheus scraping configured
- [ ] Grafana dashboards deployed
- [ ] AlertManager rules configured
- [ ] PagerDuty/Opsgenie integration tested
- [ ] Log aggregation configured (ELK/Loki)

### 6. Backup and Recovery

- [ ] Database backup completed before deployment
- [ ] Backup verification tested
- [ ] Point-in-time recovery tested
- [ ] Disaster recovery plan reviewed

### 7. Communication

- [ ] Deployment schedule communicated to stakeholders
- [ ] Maintenance window scheduled (if needed)
- [ ] On-call team notified
- [ ] Customer communication prepared (if user-facing changes)

---

## Environment Validation

### Validate Kubernetes Cluster

```bash
# Check cluster health
kubectl cluster-info
kubectl get nodes -o wide

# Check namespace exists
kubectl get namespace poultry-platform

# Check resource quotas
kubectl describe resourcequota -n poultry-platform

# Check persistent volumes
kubectl get pv
kubectl get pvc -n poultry-platform
```

### Validate Network Connectivity

```bash
# Check ingress controller
kubectl get ingress -n poultry-platform

# Check services
kubectl get svc -n poultry-platform

# Verify DNS resolution (from a test pod)
kubectl run -it --rm dns-test --image=busybox --restart=Never -- \
  nslookup postgres-service.poultry-platform.svc.cluster.local
```

### Validate Secrets

```bash
# List all secrets (verify existence, not values)
kubectl get secrets -n poultry-platform

# Verify required secrets exist
kubectl get secret poultry-secrets -n poultry-platform
kubectl get secret poultry-database-secrets -n poultry-platform
kubectl get secret poultry-payment-secrets -n poultry-platform
```

### Validate ConfigMaps

```bash
# Check configmaps
kubectl get configmaps -n poultry-platform

# Verify configuration values
kubectl describe configmap poultry-config -n poultry-platform
```

### Validate External Connectivity

```bash
# Test database connectivity from cluster
kubectl run -it --rm db-test --image=postgres:16-alpine --restart=Never -- \
  pg_isready -h postgres-service.poultry-platform.svc.cluster.local -p 5432

# Test Redis connectivity
kubectl run -it --rm redis-test --image=redis:7-alpine --restart=Never -- \
  redis-cli -h redis-service.poultry-platform.svc.cluster.local ping
```

---

## Database Migration Procedures

### Pre-Migration Steps

1. **Create a database backup**:
   ```bash
   # For managed PostgreSQL (AWS RDS, CloudSQL, etc.)
   # Create a snapshot via cloud console

   # For self-managed PostgreSQL
   pg_dump -h $DB_HOST -U $DB_USERNAME -d poultry > backup_$(date +%Y%m%d_%H%M%S).sql
   ```

2. **Verify current migration state**:
   ```bash
   # Connect to database and check Flyway schema history
   psql -h $DB_HOST -U $DB_USERNAME -d poultry -c \
     "SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;"
   ```

3. **Review pending migrations**:
   - Check `backend/src/main/resources/db/migration/` for new migration files
   - Review SQL changes with DBA if significant schema changes

### Migration Execution

Flyway migrations run automatically on application startup. For manual control:

```bash
# Option 1: Run migrations via Flyway CLI
./gradlew flywayMigrate -Dflyway.url=jdbc:postgresql://$DB_HOST:5432/poultry \
  -Dflyway.user=$DB_USERNAME -Dflyway.password=$DB_PASSWORD

# Option 2: Run migrations via application startup
# Migrations execute automatically when Spring Boot starts

# Option 3: Run migrations in Kubernetes Job
kubectl apply -f infra/k8s/migration-job.yaml
```

### Migration Rollback

Flyway Community does not support automatic rollbacks. For rollback:

1. **Restore from backup** (preferred for major issues):
   ```bash
   psql -h $DB_HOST -U $DB_USERNAME -d poultry < backup_YYYYMMDD_HHMMSS.sql
   ```

2. **Manual rollback** (for minor changes):
   - Create a new migration file with reverse changes
   - Example: `V21__rollback_v20_changes.sql`

### Migration Best Practices

- Always use additive changes when possible (add columns, don't remove)
- Add new columns as nullable, then add constraints in subsequent migrations
- Never modify existing migration files
- Test migrations on staging with production-like data
- Consider data volume impact on migration time

---

## Deployment Sequence

Production deployment follows a specific order to minimize downtime and ensure service continuity.

### Phase 1: Infrastructure Updates

1. **Apply Kubernetes namespace and RBAC** (if changed):
   ```bash
   kubectl apply -f infra/k8s/namespace.yaml
   kubectl apply -f infra/k8s/backend-deployment.yaml  # ServiceAccount only
   ```

2. **Update ConfigMaps**:
   ```bash
   kubectl apply -f infra/k8s/configmap.yaml
   ```

3. **Update Secrets** (if changed):
   ```bash
   # For External Secrets Operator
   kubectl apply -f infra/k8s/external-secrets/

   # For manual secrets
   kubectl apply -f infra/k8s/secrets.yaml
   ```

### Phase 2: Database Layer

1. **Verify PostgreSQL health**:
   ```bash
   kubectl get pods -l app=postgres -n poultry-platform
   kubectl exec -it postgres-0 -n poultry-platform -- pg_isready
   ```

2. **Verify Redis health**:
   ```bash
   kubectl get pods -l app=redis -n poultry-platform
   kubectl exec -it redis-0 -n poultry-platform -- redis-cli ping
   ```

### Phase 3: Backend API Deployment

1. **Deploy new backend version**:
   ```bash
   # Using kubectl
   kubectl set image deployment/poultry-backend \
     backend=ghcr.io/your-org/poultry-platform/backend:v1.0.0 \
     -n poultry-platform

   # Or using kustomize
   cd infra/k8s && kubectl apply -k .
   ```

2. **Monitor rollout**:
   ```bash
   kubectl rollout status deployment/poultry-backend -n poultry-platform --timeout=600s
   ```

3. **Verify pods are healthy**:
   ```bash
   kubectl get pods -l app.kubernetes.io/name=poultry-backend -n poultry-platform
   kubectl logs -f deployment/poultry-backend -n poultry-platform --tail=100
   ```

### Phase 4: Frontend Deployment

1. **Deploy Seller Web frontend**:
   ```bash
   # If using Kubernetes
   kubectl set image deployment/seller-web \
     seller-web=ghcr.io/your-org/poultry-platform/seller-web:v1.0.0 \
     -n poultry-platform

   # If using CDN/static hosting
   # Upload built assets to CDN
   # Invalidate CDN cache
   ```

2. **Verify frontend deployment**:
   ```bash
   curl -I https://seller.poultry-platform.com/
   ```

### Phase 5: Mobile App Updates

1. **Submit updated apps to stores** (if app changes):
   - iOS: Submit to App Store Connect
   - Android: Submit to Google Play Console

2. **Enable feature flags for new features** (if applicable)

3. **Monitor crash reporting** (Firebase Crashlytics, Sentry)

---

## Smoke Test Procedures

After deployment, run smoke tests to verify core functionality.

### Automated Smoke Tests

```bash
# Run smoke test script
./scripts/deploy/smoke-test.sh production

# Or run specific test suites
./scripts/deploy/smoke-test.sh production --suite=critical
./scripts/deploy/smoke-test.sh production --suite=payments
```

### Manual Smoke Tests

#### 1. Health Endpoints

```bash
# Liveness probe
curl https://api.poultry-platform.com/api/actuator/health/liveness
# Expected: {"status":"UP"}

# Readiness probe
curl https://api.poultry-platform.com/api/actuator/health/readiness
# Expected: {"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"}}}

# Metrics endpoint
curl https://api.poultry-platform.com/api/actuator/prometheus | head -20
```

#### 2. Authentication Tests

```bash
# Seller login
curl -X POST https://api.poultry-platform.com/api/auth/seller/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test_seller","password":"test_password"}'

# Buyer OTP request
curl -X POST https://api.poultry-platform.com/api/auth/buyer/request-otp \
  -H "Content-Type: application/json" \
  -d '{"phone":"+911234567890"}'
```

#### 3. Core API Tests

```bash
# Get products (public endpoint)
curl https://api.poultry-platform.com/api/products?page=0&size=10

# Get categories (public endpoint)
curl https://api.poultry-platform.com/api/products/categories
```

#### 4. Frontend Tests

- [ ] Seller Web loads without errors
- [ ] Login page functional
- [ ] Dashboard displays after login
- [ ] Product listing loads
- [ ] Order management accessible

#### 5. Integration Tests

- [ ] Place a test order (use test payment mode)
- [ ] Verify order appears in seller dashboard
- [ ] Verify SMS notifications received (test phone)
- [ ] Verify payment webhook processed

---

## Rollback Procedures

If issues are detected, follow rollback procedures immediately.

### Decision Criteria for Rollback

Initiate rollback if:
- Health checks failing for > 2 minutes
- Error rate exceeds 5% of requests
- Critical functionality broken (payments, auth, orders)
- Data corruption detected
- Security vulnerability discovered

### Kubernetes Rollback

```bash
# Immediate rollback to previous version
kubectl rollout undo deployment/poultry-backend -n poultry-platform

# Rollback to specific revision
kubectl rollout history deployment/poultry-backend -n poultry-platform
kubectl rollout undo deployment/poultry-backend -n poultry-platform --to-revision=3

# Verify rollback
kubectl rollout status deployment/poultry-backend -n poultry-platform
```

### Database Rollback

If migrations caused issues:

```bash
# 1. Stop application pods to prevent further damage
kubectl scale deployment/poultry-backend --replicas=0 -n poultry-platform

# 2. Restore database from backup
pg_restore -h $DB_HOST -U $DB_USERNAME -d poultry backup_YYYYMMDD_HHMMSS.dump

# 3. Deploy previous application version
kubectl rollout undo deployment/poultry-backend -n poultry-platform

# 4. Scale back up
kubectl scale deployment/poultry-backend --replicas=2 -n poultry-platform
```

### Frontend Rollback

```bash
# Revert to previous CDN assets
# Update CDN origin to previous version
# Invalidate CDN cache

# If using Kubernetes
kubectl rollout undo deployment/seller-web -n poultry-platform
```

### Post-Rollback Actions

1. **Notify stakeholders** of rollback
2. **Document the issue** in incident report
3. **Analyze logs** for root cause
4. **Create hotfix** if needed
5. **Schedule re-deployment** after fix

---

## Post-Deployment Verification

### Immediate Verification (First 15 minutes)

1. **Check pod status**:
   ```bash
   kubectl get pods -n poultry-platform -o wide
   ```

2. **Check service health**:
   ```bash
   for i in {1..10}; do
     curl -s -o /dev/null -w "%{http_code}\n" \
       https://api.poultry-platform.com/api/actuator/health
     sleep 5
   done
   ```

3. **Monitor error rates**:
   ```bash
   # Check application logs for errors
   kubectl logs -l app.kubernetes.io/name=poultry-backend -n poultry-platform \
     --tail=100 | grep -i error
   ```

4. **Verify metrics collection**:
   - Check Prometheus targets are UP
   - Check Grafana dashboards show data

### Extended Verification (First hour)

1. **Monitor key metrics**:
   - Request latency (p50, p95, p99)
   - Error rate (should be < 1%)
   - Active connections
   - Memory and CPU usage

2. **Check integration points**:
   - Payment gateway transactions
   - SMS delivery rates
   - Push notification delivery

3. **Review application logs**:
   ```bash
   kubectl logs -f deployment/poultry-backend -n poultry-platform --since=1h
   ```

### Daily Verification (First week)

1. **Monitor business metrics**:
   - Order success rate
   - Payment success rate
   - User registration rate
   - Active users

2. **Check error trends**:
   - Review Sentry/error tracking for new issues
   - Monitor customer support tickets

3. **Performance baseline**:
   - Compare response times to pre-deployment
   - Check database query performance
   - Monitor cache hit rates

---

## Emergency Contacts

| Role | Contact | Escalation |
|------|---------|------------|
| On-Call Engineer | [primary-oncall@company.com] | PagerDuty |
| Platform Lead | [platform-lead@company.com] | Phone |
| Database Admin | [dba@company.com] | PagerDuty |
| Security Team | [security@company.com] | Slack #security |

---

## Deployment Checklist Summary

### Pre-Deployment
- [ ] Code review completed
- [ ] CI/CD passing
- [ ] Database backup created
- [ ] Secrets verified
- [ ] Stakeholders notified

### During Deployment
- [ ] Infrastructure updated
- [ ] Database migrations successful
- [ ] Backend deployed and healthy
- [ ] Frontend deployed and accessible

### Post-Deployment
- [ ] Smoke tests passed
- [ ] Health checks green
- [ ] Metrics collecting
- [ ] No elevated error rates
- [ ] Stakeholders notified of completion
