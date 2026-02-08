# Poultry Platform

A B2B poultry marketplace connecting sellers and buyers with end-to-end order management, payments, settlements, and delivery tracking.

## Architecture

| Component | Tech Stack | Port |
|-----------|-----------|------|
| **Backend** | Spring Boot 3.4.1, Java 21, PostgreSQL, Redis, Kafka | 8082 |
| **Seller Dashboard** | Next.js 16, React 19, TypeScript, Tailwind CSS | 3000 |
| **Buyer Android** | Kotlin, Jetpack Compose, Hilt, Retrofit | - |
| **Buyer iOS** | Swift 5.9+, SwiftUI, Alamofire, Kingfisher | - |
| **Infrastructure** | Docker Compose, Kubernetes, MinIO | - |

## Quick Start

### Prerequisites

- Java 21
- Node.js 20+
- Docker & Docker Compose

### Run with Docker Compose

```bash
docker-compose up -d
```

This starts PostgreSQL, Redis, Kafka, MinIO, the backend, and the seller dashboard.

### Run Locally

**Backend:**

```bash
cd backend
./gradlew bootRun
```

**Seller Dashboard:**

```bash
cd seller-web
npm ci
npm run dev
```

Open http://localhost:3000 for the seller dashboard.

## Services

| Service | URL |
|---------|-----|
| Seller Dashboard | http://localhost:3000 |
| Backend API | http://localhost:8082/api/v1 |
| Swagger UI | http://localhost:8082/api/swagger-ui.html |
| Health Check | http://localhost:8082/api/actuator/health |
| MinIO Console | http://localhost:9001 |

## Backend Modules

The backend is organized into domain modules under `com.poultry.*`:

- **auth** — JWT authentication, token refresh, password reset
- **seller** — Seller registration, profiles, bank accounts
- **product** — Product catalog, pricing, inventory
- **order** — Order lifecycle with state machine
- **payment** — Razorpay payment integration
- **settlement** — Seller payouts and settlement cycles
- **delivery** — Delivery tracking with OTP verification
- **cart** — Shopping cart management
- **dispute** — Dispute management and resolution
- **analytics** — Business analytics and reporting
- **notification** — Push notifications, SMS (MSG91/Twilio)
- **ledger** — Double-entry ledger for financial tracking
- **reconciliation** — Financial reconciliation
- **review** — Reviews and ratings
- **verification** — Seller KYC verification
- **storage** — File uploads via MinIO (S3-compatible)

## Seller Dashboard Features

- Order management and tracking
- Product catalog management
- Financial settlements and wallet
- Business analytics with charts
- Dispute management
- Settings and profile management

## Infrastructure

- **Database:** PostgreSQL 16 with Flyway migrations (20 migrations)
- **Cache:** Redis 7
- **Message Queue:** Apache Kafka for event-driven processing
- **Object Storage:** MinIO for images, receipts, and documents
- **Orchestration:** Kubernetes manifests with HPA, network policies, and monitoring
- **Security:** JWT auth, rate limiting (Bucket4j), circuit breakers (Resilience4j)

## Testing

```bash
# Backend
cd backend && ./gradlew test

# Seller Dashboard
cd seller-web && npm test

# Lint
cd seller-web && npm run lint
```

## Documentation

- [Setup Guide](SETUP.md) — Detailed setup instructions for all components
- [ER Diagram](docs/er-diagram.md) — Database entity relationships
- [Production Deployment](docs/PRODUCTION-DEPLOYMENT.md) — Production deployment guide
- [Running Guide](docs/RUNNING.md) — Running the application
- [Operations Runbooks](docs/operations/runbooks/) — Troubleshooting guides

## Project Structure

```
poultry-platform/
  backend/          Spring Boot API server
  seller-web/       Next.js seller dashboard
  buyer-android/    Kotlin/Jetpack Compose buyer app
  buyer-ios/        SwiftUI buyer app
  infra/            Kubernetes manifests & deployment configs
  docs/             Documentation, runbooks, legal
  scripts/          Utility scripts
  docker-compose.yml
  SETUP.md
```
