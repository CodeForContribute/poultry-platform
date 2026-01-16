# Poultry B2B Platform - Setup & Run Guide

Complete guide to set up and run the Poultry B2B Platform for development and production environments.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start (Docker)](#quick-start-docker)
- [Environment Configuration](#environment-configuration)
- [Backend Setup](#backend-setup)
- [Seller Web Frontend Setup](#seller-web-frontend-setup)
- [Buyer iOS App Setup](#buyer-ios-app-setup)
- [Buyer Android App Setup](#buyer-android-app-setup)
- [Database Setup](#database-setup)
- [Infrastructure Services](#infrastructure-services)
- [Production Deployment](#production-deployment)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| Docker & Docker Compose | 24.0+ | Container runtime |
| Java JDK | 21 | Backend development |
| Gradle | 8.5+ | Backend build tool |
| Node.js | 20+ | Frontend development |
| npm | 10+ | Frontend package manager |
| Xcode | 15+ | iOS development |
| Android Studio | Hedgehog+ | Android development |
| Git | 2.40+ | Version control |

### System Requirements

- **RAM**: 16GB minimum (8GB for Docker services)
- **Disk**: 20GB free space
- **OS**: macOS 13+, Ubuntu 22.04+, or Windows 11 with WSL2

---

## Quick Start (Docker)

The fastest way to get the entire stack running locally:

```bash
# 1. Clone the repository
git clone <repository-url>
cd poultry-platform

# 2. Copy environment file
cp .env.example .env

# 3. Start all services
docker-compose up -d

# 4. Verify services are running
docker-compose ps

# 5. View logs
docker-compose logs -f backend
```

**Services started:**
- Backend API: http://localhost:8080
- PostgreSQL: localhost:5432
- Redis: localhost:6379
- Kafka: localhost:9092
- MinIO Console: http://localhost:9001 (minioadmin/minioadmin)
- MinIO API: http://localhost:9000

---

## Environment Configuration

### Create Environment File

```bash
cp .env.example .env
```

### Required Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=poultry
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis Cache
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Kafka
KAFKA_SERVERS=localhost:9092

# MinIO Object Storage
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

# JWT Security (generate a secure 256-bit key)
JWT_SECRET=your-256-bit-secret-key-base64-encoded-minimum-32-characters

# Encryption Key (for sensitive data)
ENCRYPTION_KEY=your-encryption-key-minimum-32-characters
```

### Optional Environment Variables

```bash
# Payment Gateway (Razorpay)
RAZORPAY_ENABLED=false
RAZORPAY_KEY_ID=rzp_test_xxxxx
RAZORPAY_KEY_SECRET=your-secret
RAZORPAY_WEBHOOK_SECRET=webhook-secret
RAZORPAY_ACCOUNT_NUMBER=your-account-number

# SMS Provider (MSG91)
MSG91_AUTH_KEY=your-auth-key
MSG91_SENDER_ID=PLTRY
MSG91_TEMPLATE_ID=template-id

# SMS Provider Alternative (Twilio)
TWILIO_ACCOUNT_SID=your-sid
TWILIO_AUTH_TOKEN=your-token
TWILIO_PHONE_NUMBER=+1234567890

# Email (SMTP)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password

# Firebase Push Notifications
FCM_PROJECT_ID=your-project-id
FCM_PRIVATE_KEY=your-private-key
FCM_CLIENT_EMAIL=your-client-email

# Platform Settings
PLATFORM_FEE_PERCENT=2.5
PAYMENT_TIMEOUT_MINUTES=30
MAX_BUYER_SESSIONS=5

# Rate Limiting
RATE_LIMIT_ENABLED=true
RATE_LIMIT_DEFAULT_RPM=100
RATE_LIMIT_AUTH_RPM=20
RATE_LIMIT_ADMIN_RPM=200
RATE_LIMIT_WEBHOOK_RPM=500

# Security Headers
SECURITY_HEADERS_ENABLED=true
HSTS_ENABLED=true
HSTS_MAX_AGE=31536000
HSTS_INCLUDE_SUBDOMAINS=true
HSTS_PRELOAD=true
```

---

## Backend Setup

### Option 1: Run with Docker (Recommended)

```bash
# Build and run backend with all dependencies
docker-compose up -d backend

# View logs
docker-compose logs -f backend
```

### Option 2: Run Locally

#### Prerequisites
- JDK 21 installed
- Gradle 8.5+ installed (or use wrapper)
- Infrastructure services running (PostgreSQL, Redis, Kafka, MinIO)

#### Steps

```bash
# Navigate to backend directory
cd backend

# Start infrastructure services first
docker-compose up -d postgres redis kafka zookeeper minio minio-init

# Build the application
./gradlew build -x test

# Run the application
./gradlew bootRun

# Or run with specific profile
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

#### Available Profiles

| Profile | Description |
|---------|-------------|
| `local` | Local development (default) |
| `docker` | Docker environment |
| `kubernetes` | Kubernetes deployment |
| `test` | Testing with TestContainers |

### API Documentation

Once running, access the API documentation:

- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api/v3/api-docs

### Health Checks

```bash
# Liveness
curl http://localhost:8080/api/actuator/health/liveness

# Readiness
curl http://localhost:8080/api/actuator/health/readiness

# Metrics
curl http://localhost:8080/api/actuator/prometheus
```

### Running Tests

```bash
cd backend

# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

---

## Seller Web Frontend Setup

### Prerequisites
- Node.js 20+
- npm 10+

### Installation

```bash
# Navigate to frontend directory
cd seller-web

# Install dependencies
npm install

# Create environment file
echo "NEXT_PUBLIC_API_URL=http://localhost:8080" > .env.local
```

### Development

```bash
# Start development server
npm run dev

# Access at http://localhost:3000
```

### Production Build

```bash
# Build for production
npm run build

# Start production server
npm start

# Run linting
npm run lint
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `NEXT_PUBLIC_API_URL` | Backend API URL | `http://localhost:8080` |

---

## Buyer iOS App Setup

### Prerequisites
- macOS 13+
- Xcode 15+
- iOS 17+ Simulator or Device
- Apple Developer Account (for device deployment)

### Installation

```bash
# Navigate to iOS project
cd buyer-ios

# Open in Xcode
open Package.swift
```

### Configuration

1. Open `PoultryBuyer/Core/Network/APIClient.swift`
2. Update the base URL:
   ```swift
   private let baseURL = "http://localhost:8080/api"
   // For simulator with local backend:
   // private let baseURL = "http://127.0.0.1:8080/api"
   ```

### Build & Run

1. Select target device/simulator in Xcode
2. Press `Cmd + R` to build and run

### Dependencies (Swift Package Manager)

| Package | Version | Purpose |
|---------|---------|---------|
| Alamofire | 5.8.0 | HTTP networking |
| Kingfisher | 7.10.0 | Image loading & caching |
| KeychainAccess | 4.2.0 | Secure credential storage |

---

## Buyer Android App Setup

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34 (Android 14)
- Android Emulator or Physical Device (API 26+)

### Installation

```bash
# Navigate to Android project
cd buyer-android

# Open in Android Studio
# File > Open > Select buyer-android folder
```

### Configuration

1. Open `app/build.gradle.kts`
2. Update the API URL in buildConfigField:
   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/api\"")
   // 10.0.2.2 is the emulator's localhost alias
   ```

3. For Razorpay integration (optional):
   ```kotlin
   buildConfigField("String", "RAZORPAY_KEY_ID", "\"rzp_test_XXXXXXXXXX\"")
   ```

### Build & Run

1. Sync Gradle files (`File > Sync Project with Gradle Files`)
2. Select target device/emulator
3. Click `Run` (green play button) or press `Shift + F10`

### Key Dependencies

| Dependency | Purpose |
|------------|---------|
| Jetpack Compose | Modern UI toolkit |
| Hilt | Dependency injection |
| Retrofit + OkHttp | HTTP networking |
| Room | Local database |
| Coil | Image loading |
| DataStore | Preferences storage |
| Razorpay SDK | Payment integration |

---

## Database Setup

### PostgreSQL Configuration

The database is automatically configured when using Docker Compose.

**Connection Details:**
- Host: `localhost`
- Port: `5432`
- Database: `poultry`
- Username: `postgres`
- Password: `postgres`

### Database Migrations

Migrations are managed by Flyway and run automatically on application startup.

**Migration Location:** `backend/src/main/resources/db/migration/`

**Migration Files (V1-V20):**
1. `V1__create_enums.sql` - Enum types
2. `V2__create_sellers_and_users.sql` - Seller accounts
3. `V3__create_buyers.sql` - Buyer accounts
4. `V4__create_products_and_pricing.sql` - Product catalog
5. `V5__create_orders.sql` - Order management
6. `V6__create_payments.sql` - Payment processing
7. `V7__create_ledger.sql` - Double-entry ledger
8. `V8__create_delivery.sql` - Delivery tracking
9. `V9__create_settlements.sql` - Seller settlements
10. `V10__create_auth_tables.sql` - Authentication
11. `V11__create_notifications.sql` - Notifications
12. `V12__create_file_uploads.sql` - File management
13. `V13__create_additional_indexes.sql` - Performance indexes
14. `V14__create_seller_verifications.sql` - KYC verification
15. `V15__create_deliveries_table.sql` - Delivery OTP
16. `V16__add_settlement_approval_columns.sql` - Settlement workflow
17. `V17__create_carts.sql` - Shopping cart
18. `V18__create_reviews.sql` - Reviews & ratings
19. `V19__create_disputes.sql` - Dispute management
20. `V20__seller_payment_fields.sql` - Payment fields

### Manual Database Access

```bash
# Connect via Docker
docker exec -it poultry-postgres psql -U postgres -d poultry

# Or use psql directly
psql -h localhost -p 5432 -U postgres -d poultry
```

### Database Backup & Restore

```bash
# Backup
docker exec poultry-postgres pg_dump -U postgres poultry > backup.sql

# Restore
cat backup.sql | docker exec -i poultry-postgres psql -U postgres -d poultry
```

---

## Infrastructure Services

### Redis (Cache & Sessions)

```bash
# Start Redis
docker-compose up -d redis

# Connect to Redis CLI
docker exec -it poultry-redis redis-cli

# Test connection
docker exec poultry-redis redis-cli ping
```

### Kafka (Message Queue)

```bash
# Start Kafka and Zookeeper
docker-compose up -d zookeeper kafka

# List topics
docker exec poultry-kafka kafka-topics --list --bootstrap-server localhost:9092

# Create topic manually
docker exec poultry-kafka kafka-topics --create --topic order-events \
  --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

### MinIO (Object Storage)

```bash
# Start MinIO
docker-compose up -d minio minio-init

# Access Console: http://localhost:9001
# Username: minioadmin
# Password: minioadmin
```

**Pre-configured Buckets:**
- `receipts` - Payment receipts
- `delivery-photos` - Delivery proof images
- `product-images` - Product catalog images (public read)
- `reconciliation-reports` - Financial reports

---

## Production Deployment

### Kubernetes Deployment

#### Prerequisites
- Kubernetes cluster (EKS, GKE, AKS, or self-managed)
- kubectl configured
- Docker registry access

#### Deploy to Kubernetes

```bash
# Navigate to Kubernetes manifests
cd infra/k8s

# Create namespace
kubectl apply -f namespace.yaml

# Apply all resources
kubectl apply -k .

# Or apply individually
kubectl apply -f configmap.yaml
kubectl apply -f secrets.yaml
kubectl apply -f postgres.yaml
kubectl apply -f redis.yaml
kubectl apply -f backend-deployment.yaml
kubectl apply -f backend-service.yaml
kubectl apply -f backend-hpa.yaml
```

#### Verify Deployment

```bash
# Check pods
kubectl get pods -n poultry-platform

# Check services
kubectl get svc -n poultry-platform

# View logs
kubectl logs -f deployment/backend -n poultry-platform

# Check HPA
kubectl get hpa -n poultry-platform
```

### CI/CD Pipeline

The project includes GitHub Actions workflows:

#### CI Pipeline (`.github/workflows/ci.yml`)
- Triggers: Push/PR to main/develop
- Steps: Build, Test, Code Quality, Security Scan

#### CD Pipeline (`.github/workflows/cd.yml`)
- Triggers: Push to main (staging), Tags v* (production)
- Steps: Build Docker image, Push to GHCR, Deploy to K8s

#### Required GitHub Secrets

```
KUBE_CONFIG_STAGING    # Base64 encoded kubeconfig for staging
KUBE_CONFIG_PRODUCTION # Base64 encoded kubeconfig for production
```

### Docker Production Build

```bash
# Build production image
docker build -t poultry-backend:latest ./backend

# Run production container
docker run -d \
  --name poultry-backend \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e DB_HOST=your-db-host \
  -e REDIS_HOST=your-redis-host \
  poultry-backend:latest
```

---

## Troubleshooting

### Backend Issues

#### Application won't start

```bash
# Check logs
docker-compose logs backend

# Common issues:
# 1. Database not ready - wait for PostgreSQL to start
# 2. Missing environment variables - check .env file
# 3. Port conflict - ensure 8080 is available
```

#### Database connection failed

```bash
# Verify PostgreSQL is running
docker-compose ps postgres

# Check PostgreSQL logs
docker-compose logs postgres

# Test connection
docker exec poultry-postgres pg_isready -U postgres
```

#### Redis connection failed

```bash
# Verify Redis is running
docker exec poultry-redis redis-cli ping
```

### Frontend Issues

#### npm install fails

```bash
# Clear npm cache
npm cache clean --force

# Delete node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
```

#### API connection issues

```bash
# Verify backend is running
curl http://localhost:8080/api/actuator/health

# Check CORS settings if accessing from different origin
```

### Mobile App Issues

#### iOS build fails

```bash
# Clean build folder
# Xcode: Product > Clean Build Folder (Cmd + Shift + K)

# Reset package cache
# File > Packages > Reset Package Caches
```

#### Android build fails

```bash
# Clean project
./gradlew clean

# Invalidate caches
# Android Studio: File > Invalidate Caches / Restart
```

#### Cannot connect to local backend from emulator

- **iOS Simulator**: Use `127.0.0.1` or `localhost`
- **Android Emulator**: Use `10.0.2.2` (emulator's localhost alias)
- **Physical Device**: Use your machine's IP address on the same network

### Docker Issues

#### Out of disk space

```bash
# Remove unused containers, images, volumes
docker system prune -a --volumes
```

#### Services not starting

```bash
# Restart all services
docker-compose down
docker-compose up -d

# Check resource usage
docker stats
```

### Database Issues

#### Migration failed

```bash
# Check migration status
docker exec poultry-postgres psql -U postgres -d poultry \
  -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# Reset migrations (CAUTION: destroys data)
docker-compose down -v
docker-compose up -d
```

---

## Useful Commands

```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View all logs
docker-compose logs -f

# Rebuild backend
docker-compose build backend

# Reset everything (including data)
docker-compose down -v
docker-compose up -d

# Check service status
docker-compose ps

# Backend shell access
docker exec -it poultry-backend sh

# Database shell access
docker exec -it poultry-postgres psql -U postgres -d poultry
```

---

## Support

For issues and feature requests, please create an issue in the repository.
