#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Poultry Platform - Local Setup${NC}"
echo -e "${GREEN}========================================${NC}"

# Check prerequisites
echo -e "\n${YELLOW}Checking prerequisites...${NC}"

command -v docker >/dev/null 2>&1 || { echo -e "${RED}Docker is required but not installed. Aborting.${NC}"; exit 1; }
command -v docker-compose >/dev/null 2>&1 || command -v docker compose >/dev/null 2>&1 || { echo -e "${RED}Docker Compose is required but not installed. Aborting.${NC}"; exit 1; }

echo -e "${GREEN}✓ Docker and Docker Compose are installed${NC}"

# Check if .env file exists
if [ ! -f .env ]; then
    echo -e "\n${YELLOW}Creating .env file from example...${NC}"
    cp .env.example .env
    echo -e "${GREEN}✓ Created .env file. Please update it with your configuration.${NC}"
fi

# Start infrastructure services
echo -e "\n${YELLOW}Starting infrastructure services...${NC}"
docker compose up -d postgres redis zookeeper kafka minio minio-init

# Wait for services to be healthy
echo -e "\n${YELLOW}Waiting for services to be healthy...${NC}"
sleep 10

# Check service health
echo -e "\n${YELLOW}Checking service health...${NC}"

check_service() {
    local service=$1
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if docker compose ps $service | grep -q "healthy\|running"; then
            echo -e "${GREEN}✓ $service is ready${NC}"
            return 0
        fi
        echo "Waiting for $service... (attempt $attempt/$max_attempts)"
        sleep 2
        attempt=$((attempt + 1))
    done

    echo -e "${RED}✗ $service failed to start${NC}"
    return 1
}

check_service "postgres"
check_service "redis"
check_service "kafka"
check_service "minio"

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}Infrastructure is ready!${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "\nServices running:"
echo -e "  • PostgreSQL: localhost:5432"
echo -e "  • Redis: localhost:6379"
echo -e "  • Kafka: localhost:9092"
echo -e "  • MinIO: localhost:9000 (console: localhost:9001)"
echo -e "\nTo start the backend:"
echo -e "  cd backend && ./gradlew bootRun"
echo -e "\nOr run everything with Docker:"
echo -e "  docker compose up -d"
echo -e "\nAPI will be available at:"
echo -e "  http://localhost:8080/api"
echo -e "  Swagger UI: http://localhost:8080/api/swagger-ui.html"
