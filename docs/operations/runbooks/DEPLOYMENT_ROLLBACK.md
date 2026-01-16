# Deployment Rollback Runbook

## Overview

This runbook covers procedures for rolling back deployments in the Poultry Platform when issues are detected.

**Deployment Method**: Kubernetes / Docker Compose
**CI/CD**: GitHub Actions
**Container Registry**: Docker Hub / AWS ECR

---

## Table of Contents

1. [When to Rollback](#when-to-rollback)
2. [Pre-Rollback Checklist](#pre-rollback-checklist)
3. [Kubernetes Rollback](#kubernetes-rollback)
4. [Docker Compose Rollback](#docker-compose-rollback)
5. [Database Migration Rollback](#database-migration-rollback)
6. [Frontend Rollback](#frontend-rollback)
7. [Post-Rollback Verification](#post-rollback-verification)
8. [Hotfix Procedures](#hotfix-procedures)

---

## When to Rollback

### Automatic Rollback Triggers

- Health check failures after deployment
- Error rate exceeds 10% (compared to pre-deployment baseline)
- P99 latency increases by more than 100%
- Critical functionality broken

### Manual Rollback Decision Criteria

| Severity | Criteria | Action |
|----------|----------|--------|
| **Critical** | Complete service outage | Immediate rollback |
| **High** | Major feature broken, >50% users affected | Rollback within 15 min |
| **Medium** | Minor feature broken, workaround available | Consider hotfix vs rollback |
| **Low** | Cosmetic issues, edge cases | Fix forward |

### Rollback vs Fix Forward

**Rollback when**:
- Issue is severe and affecting users
- Root cause is unclear
- Fix will take more than 30 minutes

**Fix forward when**:
- Issue is minor
- Root cause is known
- Fix is straightforward and can be deployed quickly

---

## Pre-Rollback Checklist

Before initiating a rollback:

- [ ] Confirm the issue is related to the recent deployment
- [ ] Notify Incident Commander (for P1/P2)
- [ ] Document current state (error logs, metrics)
- [ ] Identify the last known good version
- [ ] Check if database migrations need rollback
- [ ] Notify stakeholders of planned rollback

### Identify Last Known Good Version

```bash
# Kubernetes: View deployment history
kubectl rollout history deployment/poultry-backend -n poultry-platform

# Get details of specific revision
kubectl rollout history deployment/poultry-backend -n poultry-platform --revision=<N>

# Docker: List available images
docker images poultry-platform/backend --format "table {{.Tag}}\t{{.CreatedAt}}"

# Git: Find last successful deployment tag
git tag -l "v*" --sort=-creatordate | head -10
```

---

## Kubernetes Rollback

### Quick Rollback (Previous Version)

```bash
# Rollback to previous revision
kubectl rollout undo deployment/poultry-backend -n poultry-platform

# Monitor rollback progress
kubectl rollout status deployment/poultry-backend -n poultry-platform

# Verify pods are running
kubectl get pods -n poultry-platform -l app=poultry-backend
```

### Rollback to Specific Revision

```bash
# List all revisions
kubectl rollout history deployment/poultry-backend -n poultry-platform

# Rollback to specific revision
kubectl rollout undo deployment/poultry-backend -n poultry-platform --to-revision=<N>

# Verify rollback
kubectl describe deployment poultry-backend -n poultry-platform | grep Image
```

### Rollback to Specific Image

```bash
# Update to specific image version
kubectl set image deployment/poultry-backend \
  poultry-backend=poultry-platform/backend:v1.2.3 \
  -n poultry-platform

# Or edit deployment directly
kubectl edit deployment poultry-backend -n poultry-platform
# Change image tag to desired version
```

### Rollback with Zero Downtime

```bash
# Use rolling update strategy (already configured)
kubectl rollout undo deployment/poultry-backend -n poultry-platform

# Watch the rollout
watch -n 2 "kubectl get pods -n poultry-platform -l app=poultry-backend"

# If rollout is stuck, check events
kubectl get events -n poultry-platform --sort-by='.lastTimestamp' | tail -20
```

### Pause/Resume Rollout

```bash
# Pause if issues detected during rollout
kubectl rollout pause deployment/poultry-backend -n poultry-platform

# Resume when ready
kubectl rollout resume deployment/poultry-backend -n poultry-platform
```

### Emergency: Force Rollback

If standard rollback is not working:

```bash
# Scale down current deployment
kubectl scale deployment poultry-backend -n poultry-platform --replicas=0

# Wait for pods to terminate
kubectl get pods -n poultry-platform -l app=poultry-backend

# Update image and scale up
kubectl set image deployment/poultry-backend \
  poultry-backend=poultry-platform/backend:<known-good-version> \
  -n poultry-platform

kubectl scale deployment poultry-backend -n poultry-platform --replicas=3
```

---

## Docker Compose Rollback

### Quick Rollback

```bash
# Stop current containers
docker compose down

# Pull specific version
docker compose pull backend

# Or specify version in docker-compose.yml
# image: poultry-platform/backend:v1.2.3

# Start with previous version
docker compose up -d
```

### Rollback to Specific Version

```bash
# Stop current service
docker compose stop backend

# Remove current container
docker compose rm -f backend

# Update docker-compose.yml or use environment variable
export BACKEND_VERSION=v1.2.3

# Start with specific version
docker compose up -d backend
```

### Using Environment File

```bash
# Create/update .env with version
echo "BACKEND_VERSION=v1.2.3" >> .env

# Restart service
docker compose up -d --force-recreate backend
```

---

## Database Migration Rollback

### Check Current Migration Version

```bash
# Kubernetes
kubectl exec -it postgres-0 -n poultry-platform -- \
  psql -U postgres -d poultry -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# Docker Compose
docker compose exec postgres psql -U postgres -d poultry -c \
  "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
```

### Flyway Rollback Options

**Option 1: Manual Rollback Script**
```sql
-- Create rollback script for each migration
-- Example: V2__add_new_column.sql
-- Rollback: R__rollback_V2.sql

ALTER TABLE orders DROP COLUMN IF EXISTS new_column;

DELETE FROM flyway_schema_history WHERE version = '2';
```

**Option 2: Restore from Backup**
```bash
# If migration caused data loss, restore from backup
# See DISASTER_RECOVERY.md for backup restoration
```

### Important Considerations

1. **Irreversible Migrations**: Some migrations (DROP TABLE, DROP COLUMN) cannot be rolled back without data loss

2. **Data Migrations**: Reversing data migrations may require custom scripts

3. **Order of Operations**:
   - Rollback application first (if possible)
   - Then rollback database
   - Verify data integrity

### Schema Version Compatibility

```
Application v1.2.3 requires schema v1.2.x
Application v1.3.0 requires schema v1.3.x

Rollback Order:
1. Stop application v1.3.0
2. Rollback schema from v1.3.x to v1.2.x
3. Deploy application v1.2.3
```

---

## Frontend Rollback

### Seller Web Application

```bash
# If deployed via CDN/static hosting
# Revert to previous build artifacts

# Vercel rollback
vercel rollback --scope <team>

# Manual CDN rollback
aws s3 sync s3://backup-bucket/seller-web/v1.2.3/ s3://production-bucket/seller-web/

# Invalidate CDN cache
aws cloudfront create-invalidation --distribution-id <ID> --paths "/*"
```

### Mobile App Considerations

- Mobile apps cannot be rolled back once released
- Use feature flags to disable problematic features
- Prepare hotfix build for app store submission

---

## Post-Rollback Verification

### Verification Checklist

```bash
# 1. Check all pods are running
kubectl get pods -n poultry-platform

# 2. Verify health endpoints
curl -s https://api.poultry-platform.com/api/actuator/health | jq

# 3. Check error rates (should decrease)
curl -s https://api.poultry-platform.com/api/actuator/prometheus | \
  grep "http_server_requests_seconds_count.*status=\"5"

# 4. Verify critical functionality
# - User authentication
# - Order creation
# - Payment processing

# 5. Check logs for errors
kubectl logs -f deployment/poultry-backend -n poultry-platform --tail=50 | grep -i error
```

### Verification Script

```bash
#!/bin/bash
# rollback-verification.sh

echo "=== Post-Rollback Verification ==="

# Check health
echo "Checking health endpoint..."
HEALTH=$(curl -s https://api.poultry-platform.com/api/actuator/health | jq -r '.status')
if [ "$HEALTH" == "UP" ]; then
  echo "Health: OK"
else
  echo "Health: FAILED"
  exit 1
fi

# Check pods
echo "Checking pod status..."
READY=$(kubectl get pods -n poultry-platform -l app=poultry-backend -o jsonpath='{.items[*].status.containerStatuses[0].ready}')
if [[ "$READY" == *"false"* ]]; then
  echo "Pods: NOT READY"
  exit 1
else
  echo "Pods: OK"
fi

# Check version
echo "Checking deployed version..."
kubectl get deployment poultry-backend -n poultry-platform -o jsonpath='{.spec.template.spec.containers[0].image}'
echo ""

echo "=== Verification Complete ==="
```

### Communication

After successful rollback:
1. Update incident channel with status
2. Update status page (if applicable)
3. Notify stakeholders
4. Schedule post-mortem

---

## Hotfix Procedures

### When to Hotfix Instead of Rollback

- Bug is isolated and well-understood
- Fix is simple and low-risk
- Rollback would cause data loss
- Dependency on new database schema

### Hotfix Process

```bash
# 1. Create hotfix branch
git checkout -b hotfix/fix-description main

# 2. Apply minimal fix
# Edit files...

# 3. Test locally
./gradlew test

# 4. Commit and push
git add .
git commit -m "hotfix: Fix critical issue in payment processing"
git push origin hotfix/fix-description

# 5. Create PR with "hotfix" label
gh pr create --title "HOTFIX: Fix payment processing" --label hotfix

# 6. Get expedited review and merge

# 7. Tag and deploy
git tag v1.2.4-hotfix
git push origin v1.2.4-hotfix
```

### Emergency Hotfix Deployment

```bash
# Skip normal CI/CD for critical fixes
# (Use only in emergencies with proper authorization)

# Build locally
./gradlew bootJar

# Build and push Docker image
docker build -t poultry-platform/backend:hotfix-$(date +%Y%m%d%H%M) .
docker push poultry-platform/backend:hotfix-$(date +%Y%m%d%H%M)

# Deploy directly
kubectl set image deployment/poultry-backend \
  poultry-backend=poultry-platform/backend:hotfix-$(date +%Y%m%d%H%M) \
  -n poultry-platform
```

---

## Rollback Command Reference

### Kubernetes Quick Reference

| Action | Command |
|--------|---------|
| View history | `kubectl rollout history deployment/poultry-backend -n poultry-platform` |
| Undo (previous) | `kubectl rollout undo deployment/poultry-backend -n poultry-platform` |
| Undo (specific) | `kubectl rollout undo deployment/poultry-backend --to-revision=N -n poultry-platform` |
| Check status | `kubectl rollout status deployment/poultry-backend -n poultry-platform` |
| Pause | `kubectl rollout pause deployment/poultry-backend -n poultry-platform` |
| Resume | `kubectl rollout resume deployment/poultry-backend -n poultry-platform` |

### Docker Compose Quick Reference

| Action | Command |
|--------|---------|
| Stop service | `docker compose stop backend` |
| Remove container | `docker compose rm -f backend` |
| Start with version | `BACKEND_VERSION=v1.2.3 docker compose up -d backend` |
| View logs | `docker compose logs -f backend` |

---

## Escalation

If rollback fails:
1. Escalate to Incident Commander
2. Consider infrastructure-level intervention
3. Engage cloud provider support if infrastructure issue
4. Prepare for manual recovery procedures

## Related Runbooks

- [API Issues](./API_ISSUES.md)
- [Database Issues](./DATABASE_ISSUES.md)
- [Disaster Recovery](../DISASTER_RECOVERY.md)
- [Deployment Guide](../DEPLOYMENT_GUIDE.md)
