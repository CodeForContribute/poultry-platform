# ==============================================================================
# HashiCorp Vault Policy for Poultry Platform
# ==============================================================================
# This file contains Vault policies for the Poultry Platform.
# Apply these policies using: vault policy write <policy-name> <file.hcl>
# ==============================================================================

# ==============================================================================
# Policy: poultry-backend
# ==============================================================================
# This policy grants the backend application read access to its secrets.
# Apply with: vault policy write poultry-backend vault-policy.hcl
# ==============================================================================

# Read access to application secrets
path "secret/data/poultry-platform/app" {
  capabilities = ["read"]
}

path "secret/data/poultry-platform/app/*" {
  capabilities = ["read"]
}

# Read access to database secrets
path "secret/data/poultry-platform/database" {
  capabilities = ["read"]
}

path "secret/data/poultry-platform/database/*" {
  capabilities = ["read"]
}

# Read access to payment secrets
path "secret/data/poultry-platform/payment" {
  capabilities = ["read"]
}

path "secret/data/poultry-platform/payment/*" {
  capabilities = ["read"]
}

# Read access to SMS provider secrets
path "secret/data/poultry-platform/sms" {
  capabilities = ["read"]
}

path "secret/data/poultry-platform/sms/*" {
  capabilities = ["read"]
}

# Read access to encryption keys
path "secret/data/poultry-platform/encryption" {
  capabilities = ["read"]
}

path "secret/data/poultry-platform/encryption/*" {
  capabilities = ["read"]
}

# Metadata access (for secret versioning)
path "secret/metadata/poultry-platform/*" {
  capabilities = ["read", "list"]
}

# ==============================================================================
# Policy: poultry-admin
# ==============================================================================
# This policy grants administrative access to manage secrets.
# Apply with: vault policy write poultry-admin vault-admin-policy.hcl
# ==============================================================================

# Full access to poultry-platform secrets
path "secret/data/poultry-platform/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}

path "secret/metadata/poultry-platform/*" {
  capabilities = ["read", "list", "delete"]
}

# Ability to manage secret versions
path "secret/delete/poultry-platform/*" {
  capabilities = ["update"]
}

path "secret/undelete/poultry-platform/*" {
  capabilities = ["update"]
}

path "secret/destroy/poultry-platform/*" {
  capabilities = ["update"]
}

# ==============================================================================
# Policy: external-secrets-cluster
# ==============================================================================
# This policy is for the External Secrets Operator ClusterSecretStore.
# Apply with: vault policy write external-secrets-cluster external-secrets-policy.hcl
# ==============================================================================

# Read access to all poultry-platform secrets
path "secret/data/poultry-platform/*" {
  capabilities = ["read"]
}

path "secret/metadata/poultry-platform/*" {
  capabilities = ["read", "list"]
}

# ==============================================================================
# Policy: poultry-rotation
# ==============================================================================
# This policy is for automated secret rotation jobs.
# Apply with: vault policy write poultry-rotation rotation-policy.hcl
# ==============================================================================

# Update secrets during rotation
path "secret/data/poultry-platform/app" {
  capabilities = ["read", "update"]
}

path "secret/data/poultry-platform/database" {
  capabilities = ["read", "update"]
}

path "secret/data/poultry-platform/encryption" {
  capabilities = ["read", "update"]
}

# Metadata access for versioning
path "secret/metadata/poultry-platform/*" {
  capabilities = ["read", "list"]
}
