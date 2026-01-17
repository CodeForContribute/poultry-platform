# Complete Setup Guide: Poultry Platform

This guide covers how to run the backend and frontend applications with and without Docker.

## Option 1: With Docker (Recommended)

### Prerequisites
- Docker Desktop or Rancher Desktop installed and running

### Steps

```bash
# 1. Navigate to project root
cd /Users/raushrak/Documents/poultry-platform

# 2. Start all services (PostgreSQL, Redis, Kafka, MinIO, Backend, Frontend)
docker-compose up --build

# Or run in detached mode
docker-compose up --build -d

# 3. Check status
docker-compose ps

# 4. View logs
docker-compose logs -f backend
docker-compose logs -f seller-web
```

### Access Points
| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api |
| MinIO Console | http://localhost:9001 |

### Stop Services
```bash
docker-compose down

# To also remove volumes (database data)
docker-compose down -v
```

---

## Option 2: Without Docker (Local Development)

### Prerequisites

**1. Install required services:**
```bash
# macOS with Homebrew
brew install postgresql@16 redis kafka

# Start services
brew services start postgresql@16
brew services start redis
brew services start zookeeper
brew services start kafka
```

**2. Java 21 required for backend:**
```bash
# Verify Java version
java -version

# If needed, set JAVA_HOME (use your actual path)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

**3. Node.js 18+ required for frontend:**
```bash
node -v  # Should be 18+
```

### Database Setup

```bash
# Create PostgreSQL role and database
psql -h localhost -d postgres -c "CREATE ROLE postgres WITH LOGIN SUPERUSER PASSWORD 'postgres';"
psql -h localhost -d postgres -c "CREATE DATABASE poultry OWNER postgres;"
```

### Start Backend

```bash
# Terminal 1: Backend
cd backend

# Set JAVA_HOME if not set globally
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Run with Gradle
./gradlew bootRun
```

The backend will:
- Start on port 8080
- Run Flyway migrations automatically
- Connect to local PostgreSQL, Redis, and Kafka

### Start Frontend

```bash
# Terminal 2: Frontend
cd seller-web

# Install dependencies (first time only)
npm install

# Development mode (with hot reload)
npm run dev

# OR Production mode
npm run build && npm start
```

### Access Points
| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api |
| Health Check (Backend) | http://localhost:8080/api/actuator/health |
| Health Check (Frontend) | http://localhost:3000/api/health |

---

## Environment Variables

### Backend (`backend/src/main/resources/application.yml` defaults)
```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=poultry
DB_USERNAME=postgres
DB_PASSWORD=postgres
REDIS_HOST=localhost
REDIS_PORT=6379
KAFKA_SERVERS=localhost:9092
JWT_SECRET=your-256-bit-secret-key
```

### Frontend (`seller-web/.env.local`)
```bash
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_APP_NAME=Poultry Platform - Seller Dashboard
```

---

## Quick Reference

| Task | Command |
|------|---------|
| **Docker: Start all** | `docker-compose up --build` |
| **Docker: Stop all** | `docker-compose down` |
| **Docker: View logs** | `docker-compose logs -f` |
| **Local: Start backend** | `cd backend && ./gradlew bootRun` |
| **Local: Start frontend (dev)** | `cd seller-web && npm run dev` |
| **Local: Start frontend (prod)** | `cd seller-web && npm run build && npm start` |
| **Local: Run backend tests** | `cd backend && ./gradlew test` |
| **Local: Run frontend tests** | `cd seller-web && npm test` |

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Docker not running | Start Docker Desktop or Rancher Desktop |
| JAVA_HOME invalid | `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` |
| PostgreSQL role missing | `psql -d postgres -c "CREATE ROLE postgres WITH LOGIN SUPERUSER PASSWORD 'postgres';"` |
| Database missing | `psql -d postgres -c "CREATE DATABASE poultry;"` |
| Port 8080 in use | `lsof -i :8080` then `kill <PID>` |
| Port 3000 in use | `lsof -i :3000` then `kill <PID>` |
| Gradle daemon slow | `./gradlew --stop` then retry |
