#!/bin/bash
# ==============================================================================
# Local Development Secrets Setup Script
# ==============================================================================
# This script generates secure secrets for local development and creates
# the necessary .env file and Kubernetes secrets.
#
# Usage:
#   ./setup-local-secrets.sh              # Interactive mode
#   ./setup-local-secrets.sh --generate   # Auto-generate all secrets
#   ./setup-local-secrets.sh --k8s        # Generate and apply to local K8s
#   ./setup-local-secrets.sh --help       # Show help
# ==============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"
INFRA_DIR="$PROJECT_ROOT/infra"

# Output files
ENV_FILE="$PROJECT_ROOT/.env"
SECRETS_LOCAL_FILE="$INFRA_DIR/k8s/secrets.local.yaml"

# ==============================================================================
# Helper Functions
# ==============================================================================

print_header() {
    echo -e "\n${GREEN}========================================${NC}"
    echo -e "${GREEN}$1${NC}"
    echo -e "${GREEN}========================================${NC}"
}

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[OK]${NC} $1"
}

# Generate a secure random string
generate_secret() {
    local length=${1:-32}
    openssl rand -base64 $length | tr -d '\n'
}

# Generate a secure password
generate_password() {
    local length=${1:-24}
    openssl rand -base64 $length | tr -d '\n' | head -c $length
}

# Generate a 256-bit key for encryption
generate_256bit_key() {
    openssl rand -base64 32 | tr -d '\n'
}

# Check if command exists
command_exists() {
    command -v "$1" &> /dev/null
}

# ==============================================================================
# Show Help
# ==============================================================================

show_help() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS]

Generate local development secrets for the Poultry Platform.

Options:
  --generate    Auto-generate all secrets (non-interactive)
  --k8s         Generate secrets and apply to local Kubernetes cluster
  --docker      Generate secrets for Docker Compose
  --env-only    Only generate .env file
  --help        Show this help message

Examples:
  $(basename "$0")                    # Interactive mode
  $(basename "$0") --generate         # Auto-generate all secrets
  $(basename "$0") --k8s              # Generate and apply to Kubernetes
  $(basename "$0") --docker           # Generate for Docker Compose

Notes:
  - This script is for LOCAL DEVELOPMENT only
  - DO NOT use generated secrets in production
  - Production secrets should be managed by External Secrets Operator
EOF
}

# ==============================================================================
# Check Prerequisites
# ==============================================================================

check_prerequisites() {
    print_info "Checking prerequisites..."

    if ! command_exists openssl; then
        print_error "openssl is required but not installed"
        exit 1
    fi

    print_success "All prerequisites met"
}

# ==============================================================================
# Generate Secrets
# ==============================================================================

generate_all_secrets() {
    print_header "Generating Local Development Secrets"

    # Database credentials
    export DB_USERNAME="poultry_dev"
    export DB_PASSWORD=$(generate_password 24)
    export POSTGRES_ADMIN_USER="postgres"
    export POSTGRES_ADMIN_PASSWORD=$(generate_password 24)
    export REDIS_PASSWORD=$(generate_password 16)

    # JWT and encryption keys
    export JWT_SECRET=$(generate_256bit_key)
    export ENCRYPTION_KEY=$(generate_256bit_key)

    # MinIO credentials
    export MINIO_ACCESS_KEY="poultry-dev-access"
    export MINIO_SECRET_KEY=$(generate_password 32)

    # Razorpay test credentials (placeholders)
    export RAZORPAY_ENABLED="false"
    export RAZORPAY_KEY_ID="rzp_test_PLACEHOLDER"
    export RAZORPAY_KEY_SECRET="PLACEHOLDER_SECRET"
    export RAZORPAY_WEBHOOK_SECRET="PLACEHOLDER_WEBHOOK_SECRET"
    export RAZORPAY_ACCOUNT_NUMBER="PLACEHOLDER_ACCOUNT"

    # SMS Provider (placeholders)
    export MSG91_AUTH_KEY="PLACEHOLDER_AUTH_KEY"
    export MSG91_SENDER_ID="PLTRY"
    export MSG91_TEMPLATE_ID="PLACEHOLDER_TEMPLATE_ID"

    # Twilio (optional, empty by default)
    export TWILIO_ACCOUNT_SID=""
    export TWILIO_AUTH_TOKEN=""
    export TWILIO_PHONE_NUMBER=""

    # Firebase (optional, empty by default)
    export FCM_PROJECT_ID=""
    export FCM_PRIVATE_KEY=""
    export FCM_CLIENT_EMAIL=""

    print_success "Secrets generated"
}

# ==============================================================================
# Create .env File
# ==============================================================================

create_env_file() {
    print_info "Creating .env file..."

    cat > "$ENV_FILE" << EOF
# ==============================================================================
# Poultry Platform - Local Development Environment
# ==============================================================================
# Generated by setup-local-secrets.sh on $(date)
# DO NOT COMMIT THIS FILE TO VERSION CONTROL
# ==============================================================================

# ===========================================
# Application Configuration
# ===========================================
SPRING_PROFILES_ACTIVE=dev,docker
SERVER_PORT=8080

# ===========================================
# Database Configuration
# ===========================================
DB_HOST=localhost
DB_PORT=5432
DB_NAME=poultry
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}

# PostgreSQL Admin (for initial setup)
POSTGRES_USER=${POSTGRES_ADMIN_USER}
POSTGRES_PASSWORD=${POSTGRES_ADMIN_PASSWORD}
POSTGRES_DB=poultry

# ===========================================
# Redis Configuration
# ===========================================
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=${REDIS_PASSWORD}

# ===========================================
# Kafka Configuration
# ===========================================
KAFKA_SERVERS=localhost:9092

# ===========================================
# MinIO/S3 Configuration
# ===========================================
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=${MINIO_ACCESS_KEY}
MINIO_SECRET_KEY=${MINIO_SECRET_KEY}
MINIO_ROOT_USER=${MINIO_ACCESS_KEY}
MINIO_ROOT_PASSWORD=${MINIO_SECRET_KEY}

# ===========================================
# JWT Configuration
# ===========================================
JWT_SECRET=${JWT_SECRET}
JWT_ACCESS_TOKEN_EXPIRY=15m
JWT_REFRESH_TOKEN_EXPIRY=7d

# ===========================================
# Encryption Configuration
# ===========================================
ENCRYPTION_KEY=${ENCRYPTION_KEY}

# ===========================================
# Razorpay Configuration (Test Mode)
# ===========================================
RAZORPAY_ENABLED=${RAZORPAY_ENABLED}
RAZORPAY_KEY_ID=${RAZORPAY_KEY_ID}
RAZORPAY_KEY_SECRET=${RAZORPAY_KEY_SECRET}
RAZORPAY_WEBHOOK_SECRET=${RAZORPAY_WEBHOOK_SECRET}
RAZORPAY_ACCOUNT_NUMBER=${RAZORPAY_ACCOUNT_NUMBER}

# ===========================================
# SMS Provider - MSG91
# ===========================================
MSG91_AUTH_KEY=${MSG91_AUTH_KEY}
MSG91_SENDER_ID=${MSG91_SENDER_ID}
MSG91_TEMPLATE_ID=${MSG91_TEMPLATE_ID}

# ===========================================
# SMS Provider - Twilio (Optional)
# ===========================================
TWILIO_ACCOUNT_SID=${TWILIO_ACCOUNT_SID}
TWILIO_AUTH_TOKEN=${TWILIO_AUTH_TOKEN}
TWILIO_PHONE_NUMBER=${TWILIO_PHONE_NUMBER}

# ===========================================
# Firebase Cloud Messaging (Optional)
# ===========================================
FCM_PROJECT_ID=${FCM_PROJECT_ID}
FCM_PRIVATE_KEY=${FCM_PRIVATE_KEY}
FCM_CLIENT_EMAIL=${FCM_CLIENT_EMAIL}

# ===========================================
# Platform Settings
# ===========================================
PLATFORM_FEE_PERCENT=2.0
PAYMENT_TIMEOUT_MINUTES=30
MAX_BUYER_SESSIONS=2

# ===========================================
# Rate Limiting
# ===========================================
RATE_LIMIT_ENABLED=true
RATE_LIMIT_DEFAULT_RPM=100

# ===========================================
# CORS Configuration (Development)
# ===========================================
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001

# ===========================================
# OpenAPI Configuration
# ===========================================
OPENAPI_PRODUCTION_URL=http://localhost:8080
EOF

    print_success "Created $ENV_FILE"

    # Add .env to .gitignore if not already there
    if [ -f "$PROJECT_ROOT/.gitignore" ]; then
        if ! grep -q "^\.env$" "$PROJECT_ROOT/.gitignore"; then
            echo ".env" >> "$PROJECT_ROOT/.gitignore"
            print_info "Added .env to .gitignore"
        fi
    fi
}

# ==============================================================================
# Create Kubernetes Secrets File
# ==============================================================================

create_k8s_secrets_file() {
    print_info "Creating Kubernetes secrets file..."

    cat > "$SECRETS_LOCAL_FILE" << EOF
# ==============================================================================
# Kubernetes Secrets - Local Development
# ==============================================================================
# Generated by setup-local-secrets.sh on $(date)
# DO NOT COMMIT THIS FILE TO VERSION CONTROL
#
# Apply with: kubectl apply -f secrets.local.yaml
# ==============================================================================

---
apiVersion: v1
kind: Secret
metadata:
  name: poultry-secrets
  namespace: poultry-platform
  labels:
    app.kubernetes.io/name: poultry-platform
    app.kubernetes.io/managed-by: local-dev
  annotations:
    description: "Local development secrets"
type: Opaque
stringData:
  # Database credentials
  DB_USERNAME: "${DB_USERNAME}"
  DB_PASSWORD: "${DB_PASSWORD}"

  # Redis password
  REDIS_PASSWORD: "${REDIS_PASSWORD}"

  # MinIO credentials
  MINIO_ACCESS_KEY: "${MINIO_ACCESS_KEY}"
  MINIO_SECRET_KEY: "${MINIO_SECRET_KEY}"

  # JWT secret
  JWT_SECRET: "${JWT_SECRET}"

  # Encryption key
  ENCRYPTION_KEY: "${ENCRYPTION_KEY}"

  # Razorpay credentials
  RAZORPAY_ENABLED: "${RAZORPAY_ENABLED}"
  RAZORPAY_KEY_ID: "${RAZORPAY_KEY_ID}"
  RAZORPAY_KEY_SECRET: "${RAZORPAY_KEY_SECRET}"
  RAZORPAY_WEBHOOK_SECRET: "${RAZORPAY_WEBHOOK_SECRET}"
  RAZORPAY_ACCOUNT_NUMBER: "${RAZORPAY_ACCOUNT_NUMBER}"

  # MSG91
  MSG91_AUTH_KEY: "${MSG91_AUTH_KEY}"
  MSG91_SENDER_ID: "${MSG91_SENDER_ID}"
  MSG91_TEMPLATE_ID: "${MSG91_TEMPLATE_ID}"

  # Twilio (optional)
  TWILIO_ACCOUNT_SID: "${TWILIO_ACCOUNT_SID}"
  TWILIO_AUTH_TOKEN: "${TWILIO_AUTH_TOKEN}"
  TWILIO_PHONE_NUMBER: "${TWILIO_PHONE_NUMBER}"

  # Firebase (optional)
  FCM_PROJECT_ID: "${FCM_PROJECT_ID}"
  FCM_PRIVATE_KEY: "${FCM_PRIVATE_KEY}"
  FCM_CLIENT_EMAIL: "${FCM_CLIENT_EMAIL}"

---
apiVersion: v1
kind: Secret
metadata:
  name: poultry-database-secrets
  namespace: poultry-platform
  labels:
    app.kubernetes.io/name: poultry-platform
    app.kubernetes.io/component: database
    app.kubernetes.io/managed-by: local-dev
type: Opaque
stringData:
  DB_USERNAME: "${DB_USERNAME}"
  DB_PASSWORD: "${DB_PASSWORD}"
  REDIS_PASSWORD: "${REDIS_PASSWORD}"
  DATABASE_URL: "postgresql://${DB_USERNAME}:${DB_PASSWORD}@postgres-service:5432/poultry"
  SPRING_DATASOURCE_URL: "jdbc:postgresql://postgres-service:5432/poultry"
  SPRING_DATASOURCE_USERNAME: "${DB_USERNAME}"
  SPRING_DATASOURCE_PASSWORD: "${DB_PASSWORD}"
  REDIS_URL: "redis://:${REDIS_PASSWORD}@redis-service:6379"

---
apiVersion: v1
kind: Secret
metadata:
  name: poultry-payment-secrets
  namespace: poultry-platform
  labels:
    app.kubernetes.io/name: poultry-platform
    app.kubernetes.io/component: payment
    app.kubernetes.io/managed-by: local-dev
type: Opaque
stringData:
  RAZORPAY_ENABLED: "${RAZORPAY_ENABLED}"
  RAZORPAY_KEY_ID: "${RAZORPAY_KEY_ID}"
  RAZORPAY_KEY_SECRET: "${RAZORPAY_KEY_SECRET}"
  RAZORPAY_WEBHOOK_SECRET: "${RAZORPAY_WEBHOOK_SECRET}"
  RAZORPAY_ACCOUNT_NUMBER: "${RAZORPAY_ACCOUNT_NUMBER}"

---
apiVersion: v1
kind: Secret
metadata:
  name: postgres-admin-secrets
  namespace: poultry-platform
  labels:
    app.kubernetes.io/name: poultry-platform
    app.kubernetes.io/component: database-admin
    app.kubernetes.io/managed-by: local-dev
type: Opaque
stringData:
  POSTGRES_USER: "${POSTGRES_ADMIN_USER}"
  POSTGRES_PASSWORD: "${POSTGRES_ADMIN_PASSWORD}"
  POSTGRES_DB: "poultry"
EOF

    print_success "Created $SECRETS_LOCAL_FILE"

    # Add secrets.local.yaml to .gitignore
    if [ -f "$INFRA_DIR/k8s/.gitignore" ]; then
        if ! grep -q "secrets.local.yaml" "$INFRA_DIR/k8s/.gitignore"; then
            echo "secrets.local.yaml" >> "$INFRA_DIR/k8s/.gitignore"
        fi
    else
        echo "secrets.local.yaml" > "$INFRA_DIR/k8s/.gitignore"
    fi
}

# ==============================================================================
# Apply to Kubernetes
# ==============================================================================

apply_to_kubernetes() {
    print_info "Applying secrets to Kubernetes..."

    if ! command_exists kubectl; then
        print_error "kubectl is not installed"
        return 1
    fi

    # Check if namespace exists
    if ! kubectl get namespace poultry-platform &> /dev/null; then
        print_info "Creating namespace poultry-platform..."
        kubectl create namespace poultry-platform
    fi

    # Apply secrets
    kubectl apply -f "$SECRETS_LOCAL_FILE"
    print_success "Secrets applied to Kubernetes"

    # Show secret status
    echo ""
    print_info "Secrets in poultry-platform namespace:"
    kubectl get secrets -n poultry-platform -l app.kubernetes.io/managed-by=local-dev
}

# ==============================================================================
# Print Summary
# ==============================================================================

print_summary() {
    print_header "Setup Complete!"

    echo ""
    echo "Generated files:"
    echo "  - $ENV_FILE"
    echo "  - $SECRETS_LOCAL_FILE"
    echo ""
    echo "Next steps:"
    echo ""
    echo "1. For Docker Compose development:"
    echo "   cd $PROJECT_ROOT && docker compose up -d"
    echo ""
    echo "2. For local Gradle development:"
    echo "   source $ENV_FILE"
    echo "   cd $BACKEND_DIR && ./gradlew bootRun"
    echo ""
    echo "3. For local Kubernetes development:"
    echo "   kubectl apply -f $SECRETS_LOCAL_FILE"
    echo ""
    echo -e "${YELLOW}IMPORTANT:${NC}"
    echo "  - These secrets are for LOCAL DEVELOPMENT only"
    echo "  - Do NOT use these secrets in production"
    echo "  - Do NOT commit .env or secrets.local.yaml to version control"
    echo ""
}

# ==============================================================================
# Main
# ==============================================================================

main() {
    local mode="interactive"

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --generate)
                mode="generate"
                shift
                ;;
            --k8s)
                mode="kubernetes"
                shift
                ;;
            --docker)
                mode="docker"
                shift
                ;;
            --env-only)
                mode="env-only"
                shift
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done

    print_header "Poultry Platform - Local Secrets Setup"

    check_prerequisites
    generate_all_secrets
    create_env_file

    case $mode in
        "interactive"|"generate"|"docker")
            create_k8s_secrets_file
            print_summary
            ;;
        "kubernetes")
            create_k8s_secrets_file
            apply_to_kubernetes
            print_summary
            ;;
        "env-only")
            print_success "Generated .env file only"
            ;;
    esac
}

main "$@"
