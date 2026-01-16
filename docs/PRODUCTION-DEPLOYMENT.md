# Poultry Platform - Production Deployment Guide

This guide covers the steps required to deploy the Poultry Platform to production.

## Pre-Deployment Checklist

### 1. Secrets Configuration

All secrets have been pre-generated in `.env.production` and `infra/k8s/secrets.yaml`. Before deployment, you must:

#### Required Third-Party Credentials

| Service | Required | Where to Get |
|---------|----------|--------------|
| Razorpay | Yes | [Razorpay Dashboard](https://dashboard.razorpay.com/app/keys) |
| MSG91 | Yes (or Twilio) | [MSG91 Dashboard](https://msg91.com/) |
| Twilio | Alternative SMS | [Twilio Console](https://console.twilio.com/) |
| Firebase FCM | Optional | [Firebase Console](https://console.firebase.google.com/) |

#### Steps:
1. Copy `.env.production` to your secure secrets management
2. Replace all `REPLACE_*` values with actual credentials
3. Update `infra/k8s/secrets.yaml` with matching values

### 2. GitHub Actions Secrets

Configure these secrets in GitHub Repository Settings > Secrets and Variables > Actions:

```
KUBE_CONFIG_STAGING    - Base64-encoded kubeconfig for staging cluster
KUBE_CONFIG_PRODUCTION - Base64-encoded kubeconfig for production cluster
```

To generate:
```bash
cat ~/.kube/config | base64 -w 0
```

### 3. Database Setup

The platform uses PostgreSQL with Flyway migrations.

```bash
# Create production database
psql -h <host> -U postgres
CREATE DATABASE poultry;
CREATE USER poultry_prod WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE poultry TO poultry_prod;
```

Flyway will automatically run migrations on startup.

### 4. Security Checklist

- [x] JWT secret generated (256-bit)
- [x] Encryption key generated (256-bit)
- [x] Database password generated
- [x] MinIO credentials generated
- [x] Redis password generated
- [x] Rate limiting enabled
- [x] Security headers configured
- [x] HSTS enabled
- [ ] SSL/TLS certificates configured
- [ ] Firewall rules configured
- [ ] WAF enabled (recommended)

## Deployment Methods

### Option A: Kubernetes Deployment

```bash
# 1. Apply secrets first
kubectl apply -f infra/k8s/secrets.yaml

# 2. Apply all other resources
kubectl apply -k infra/k8s/

# 3. Verify deployment
kubectl get pods -n poultry-platform
kubectl logs -f deployment/poultry-backend -n poultry-platform
```

### Option B: Docker Compose (Single Server)

```bash
# 1. Copy .env.production to .env
cp .env.production .env

# 2. Update SPRING_PROFILES_ACTIVE
sed -i 's/SPRING_PROFILES_ACTIVE=kubernetes/SPRING_PROFILES_ACTIVE=docker/' .env

# 3. Start services
docker compose -f docker-compose.yml up -d
```

### Option C: GitHub Actions (Automated)

1. Push to `main` branch for staging deployment
2. Create a version tag for production deployment:
```bash
git tag v1.0.0
git push origin v1.0.0
```

## Post-Deployment Verification

### Health Checks

```bash
# Liveness probe
curl https://api.your-domain.com/api/actuator/health/liveness

# Readiness probe
curl https://api.your-domain.com/api/actuator/health/readiness

# Full health details (authenticated)
curl -H "Authorization: Bearer <token>" \
  https://api.your-domain.com/api/actuator/health
```

### Verify Services

```bash
# Check database connectivity
curl https://api.your-domain.com/api/actuator/health/readiness | jq '.components.db'

# Check Redis connectivity
curl https://api.your-domain.com/api/actuator/health/readiness | jq '.components.redis'

# Verify Swagger UI
open https://api.your-domain.com/api/swagger-ui.html
```

## Monitoring Setup

### Prometheus Metrics

Metrics are exposed at `/api/actuator/prometheus`

Recommended Prometheus scrape config:
```yaml
scrape_configs:
  - job_name: 'poultry-backend'
    metrics_path: '/api/actuator/prometheus'
    static_configs:
      - targets: ['api.your-domain.com:443']
    scheme: https
```

### Key Metrics to Monitor

| Metric | Alert Threshold |
|--------|-----------------|
| `http_server_requests_seconds` | p99 > 2s |
| `hikaricp_connections_active` | > 80% of pool |
| `jvm_memory_used_bytes` | > 80% of max |
| `resilience4j_circuitbreaker_state` | OPEN |

## Rollback Procedure

### Kubernetes
```bash
kubectl rollout undo deployment/poultry-backend -n poultry-platform
```

### Docker Compose
```bash
docker compose down
docker compose pull  # Pull previous version
docker compose up -d
```

## Troubleshooting

### Common Issues

1. **Database connection failed**
   - Verify DB_HOST and DB_PASSWORD in secrets
   - Check network connectivity to database

2. **Redis connection refused**
   - Verify REDIS_HOST and REDIS_PASSWORD
   - Check Redis service is running

3. **Payment webhook verification failed**
   - Ensure RAZORPAY_WEBHOOK_SECRET matches Razorpay dashboard
   - Verify webhook URL is accessible

4. **SMS OTP not sending**
   - Check MSG91_AUTH_KEY and MSG91_TEMPLATE_ID
   - Verify template is approved on MSG91

### Log Analysis

```bash
# Kubernetes
kubectl logs -f deployment/poultry-backend -n poultry-platform

# Docker
docker compose logs -f backend
```

## Support

For issues with:
- **Backend API**: Check `/api/actuator/health` and application logs
- **Seller Web**: Check browser console and network tab
- **Mobile Apps**: Enable debug logging in app settings
