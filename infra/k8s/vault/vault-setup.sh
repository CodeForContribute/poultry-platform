#!/bin/bash
# ==============================================================================
# HashiCorp Vault Setup Script for Poultry Platform
# ==============================================================================
# This script configures Vault for use with the Poultry Platform.
# Prerequisites:
# - Vault CLI installed and configured
# - VAULT_ADDR environment variable set
# - VAULT_TOKEN environment variable set (with admin access)
# ==============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Vault Setup for Poultry Platform${NC}"
echo -e "${GREEN}========================================${NC}"

# Check prerequisites
check_prerequisites() {
    echo -e "\n${YELLOW}Checking prerequisites...${NC}"

    if ! command -v vault &> /dev/null; then
        echo -e "${RED}Error: vault CLI is not installed${NC}"
        exit 1
    fi

    if [ -z "$VAULT_ADDR" ]; then
        echo -e "${RED}Error: VAULT_ADDR environment variable not set${NC}"
        exit 1
    fi

    if [ -z "$VAULT_TOKEN" ]; then
        echo -e "${RED}Error: VAULT_TOKEN environment variable not set${NC}"
        exit 1
    fi

    echo -e "${GREEN}Prerequisites check passed${NC}"
}

# Enable KV secrets engine v2
enable_kv_engine() {
    echo -e "\n${YELLOW}Enabling KV secrets engine v2...${NC}"

    if vault secrets list | grep -q "^secret/"; then
        echo -e "${YELLOW}KV secrets engine already enabled at secret/${NC}"
    else
        vault secrets enable -version=2 -path=secret kv
        echo -e "${GREEN}KV secrets engine enabled at secret/${NC}"
    fi
}

# Create policies
create_policies() {
    echo -e "\n${YELLOW}Creating Vault policies...${NC}"

    # Backend read-only policy
    cat <<EOF | vault policy write poultry-backend -
# Read access to application secrets
path "secret/data/poultry-platform/app" {
  capabilities = ["read"]
}
path "secret/data/poultry-platform/app/*" {
  capabilities = ["read"]
}
path "secret/data/poultry-platform/database" {
  capabilities = ["read"]
}
path "secret/data/poultry-platform/database/*" {
  capabilities = ["read"]
}
path "secret/data/poultry-platform/payment" {
  capabilities = ["read"]
}
path "secret/data/poultry-platform/payment/*" {
  capabilities = ["read"]
}
path "secret/data/poultry-platform/sms" {
  capabilities = ["read"]
}
path "secret/data/poultry-platform/sms/*" {
  capabilities = ["read"]
}
path "secret/data/poultry-platform/encryption" {
  capabilities = ["read"]
}
path "secret/data/poultry-platform/encryption/*" {
  capabilities = ["read"]
}
path "secret/metadata/poultry-platform/*" {
  capabilities = ["read", "list"]
}
EOF
    echo -e "${GREEN}Created policy: poultry-backend${NC}"

    # External Secrets Operator policy
    cat <<EOF | vault policy write external-secrets-cluster -
path "secret/data/poultry-platform/*" {
  capabilities = ["read"]
}
path "secret/metadata/poultry-platform/*" {
  capabilities = ["read", "list"]
}
EOF
    echo -e "${GREEN}Created policy: external-secrets-cluster${NC}"

    # Admin policy
    cat <<EOF | vault policy write poultry-admin -
path "secret/data/poultry-platform/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}
path "secret/metadata/poultry-platform/*" {
  capabilities = ["read", "list", "delete"]
}
path "secret/delete/poultry-platform/*" {
  capabilities = ["update"]
}
path "secret/undelete/poultry-platform/*" {
  capabilities = ["update"]
}
path "secret/destroy/poultry-platform/*" {
  capabilities = ["update"]
}
EOF
    echo -e "${GREEN}Created policy: poultry-admin${NC}"
}

# Enable Kubernetes auth
enable_kubernetes_auth() {
    echo -e "\n${YELLOW}Enabling Kubernetes authentication...${NC}"

    if vault auth list | grep -q "^kubernetes/"; then
        echo -e "${YELLOW}Kubernetes auth already enabled${NC}"
    else
        vault auth enable kubernetes
        echo -e "${GREEN}Kubernetes auth enabled${NC}"
    fi

    # Configure Kubernetes auth
    # Note: In production, these values should come from your cluster
    echo -e "${YELLOW}Please configure Kubernetes auth manually with your cluster details:${NC}"
    echo ""
    echo "vault write auth/kubernetes/config \\"
    echo "    kubernetes_host=\"https://\$KUBERNETES_SERVICE_HOST:\$KUBERNETES_SERVICE_PORT\" \\"
    echo "    kubernetes_ca_cert=@/var/run/secrets/kubernetes.io/serviceaccount/ca.crt \\"
    echo "    token_reviewer_jwt=@/var/run/secrets/kubernetes.io/serviceaccount/token"
    echo ""
}

# Create Kubernetes auth roles
create_kubernetes_roles() {
    echo -e "\n${YELLOW}Creating Kubernetes auth roles...${NC}"

    # Role for backend application
    vault write auth/kubernetes/role/poultry-backend \
        bound_service_account_names=poultry-backend \
        bound_service_account_namespaces=poultry-platform \
        policies=poultry-backend \
        ttl=1h
    echo -e "${GREEN}Created role: poultry-backend${NC}"

    # Role for External Secrets Operator
    vault write auth/kubernetes/role/external-secrets-cluster \
        bound_service_account_names=external-secrets,external-secrets-vault \
        bound_service_account_namespaces=external-secrets \
        policies=external-secrets-cluster \
        ttl=1h
    echo -e "${GREEN}Created role: external-secrets-cluster${NC}"
}

# Create initial secrets structure
create_initial_secrets() {
    echo -e "\n${YELLOW}Creating initial secrets structure...${NC}"

    # Check if secrets already exist
    if vault kv get secret/poultry-platform/app &> /dev/null; then
        echo -e "${YELLOW}Secrets already exist. Skipping initial creation.${NC}"
        echo -e "${YELLOW}Use 'vault kv put' to update existing secrets.${NC}"
        return
    fi

    # Create placeholder secrets (replace with actual values)
    echo -e "${YELLOW}Creating placeholder secrets. Replace with actual values!${NC}"

    vault kv put secret/poultry-platform/app \
        jwt_secret="REPLACE_WITH_256_BIT_SECRET" \
        encryption_key="REPLACE_WITH_256_BIT_KEY" \
        minio_access_key="REPLACE_WITH_MINIO_ACCESS_KEY" \
        minio_secret_key="REPLACE_WITH_MINIO_SECRET_KEY"
    echo -e "${GREEN}Created: secret/poultry-platform/app${NC}"

    vault kv put secret/poultry-platform/database \
        postgres_username="poultry_prod" \
        postgres_password="REPLACE_WITH_POSTGRES_PASSWORD" \
        redis_password="REPLACE_WITH_REDIS_PASSWORD"
    echo -e "${GREEN}Created: secret/poultry-platform/database${NC}"

    vault kv put secret/poultry-platform/payment \
        razorpay_enabled="false" \
        razorpay_key_id="REPLACE_WITH_RAZORPAY_KEY_ID" \
        razorpay_key_secret="REPLACE_WITH_RAZORPAY_KEY_SECRET" \
        razorpay_webhook_secret="REPLACE_WITH_WEBHOOK_SECRET" \
        razorpay_account_number="REPLACE_WITH_ACCOUNT_NUMBER"
    echo -e "${GREEN}Created: secret/poultry-platform/payment${NC}"

    vault kv put secret/poultry-platform/sms \
        msg91_auth_key="REPLACE_WITH_MSG91_AUTH_KEY" \
        msg91_sender_id="PLTRY" \
        msg91_template_id="REPLACE_WITH_TEMPLATE_ID"
    echo -e "${GREEN}Created: secret/poultry-platform/sms${NC}"

    vault kv put secret/poultry-platform/encryption \
        bank_encryption_key="REPLACE_WITH_256_BIT_KEY" \
        bank_kek="REPLACE_WITH_256_BIT_KEK"
    echo -e "${GREEN}Created: secret/poultry-platform/encryption${NC}"
}

# Print summary
print_summary() {
    echo -e "\n${GREEN}========================================${NC}"
    echo -e "${GREEN}Vault Setup Complete!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Configure Kubernetes auth with your cluster details"
    echo "2. Replace placeholder secrets with actual values"
    echo "3. Apply External Secrets manifests to your cluster"
    echo ""
    echo "Useful commands:"
    echo "  vault kv get secret/poultry-platform/app"
    echo "  vault kv put secret/poultry-platform/app key=value"
    echo "  vault policy read poultry-backend"
    echo ""
}

# Main execution
main() {
    check_prerequisites
    enable_kv_engine
    create_policies
    enable_kubernetes_auth
    create_kubernetes_roles
    create_initial_secrets
    print_summary
}

main "$@"
