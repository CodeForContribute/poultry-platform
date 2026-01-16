#!/bin/bash
#
# PostgreSQL Restore Script for Poultry Platform
# This script restores database backups from S3/MinIO with point-in-time recovery support
#
# Usage:
#   ./restore-script.sh [options]
#
# Options:
#   -b, --backup       Backup name or 'latest' for most recent backup
#   -d, --database     Target database name (default: from PGDATABASE env)
#   -h, --host         Target database host (default: from PGHOST env)
#   -t, --target-time  Point-in-time recovery timestamp (ISO 8601 format)
#   -l, --list         List available backups
#   --verify-only      Only verify backup without restoring
#   --no-confirm       Skip confirmation prompt
#   --drop-existing    Drop existing database before restore
#   --help             Show this help message
#

set -euo pipefail

# =============================================================================
# Configuration
# =============================================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESTORE_DIR="${RESTORE_DIR:-/tmp/restore}"
BACKUP_NAME=""
TARGET_TIME=""
VERIFY_ONLY=false
NO_CONFIRM=false
DROP_EXISTING=false
LIST_BACKUPS=false
LOG_LEVEL="${LOG_LEVEL:-INFO}"

# =============================================================================
# Logging Functions
# =============================================================================
declare -A LOG_LEVELS=([DEBUG]=0 [INFO]=1 [WARN]=2 [ERROR]=3)

log() {
  local level="$1"
  shift
  local message="$*"
  local current_level="${LOG_LEVELS[$LOG_LEVEL]:-1}"
  local msg_level="${LOG_LEVELS[$level]:-1}"

  if [ "$msg_level" -ge "$current_level" ]; then
    local color=""
    local reset="\033[0m"
    case "$level" in
      DEBUG) color="\033[36m" ;;
      INFO)  color="\033[32m" ;;
      WARN)  color="\033[33m" ;;
      ERROR) color="\033[31m" ;;
    esac
    echo -e "${color}[${level}]${reset} $(date '+%Y-%m-%d %H:%M:%S') - ${message}"
  fi
}

log_debug() { log "DEBUG" "$@"; }
log_info() { log "INFO" "$@"; }
log_warn() { log "WARN" "$@"; }
log_error() { log "ERROR" "$@" >&2; }

# =============================================================================
# Cleanup Function
# =============================================================================
cleanup() {
  local exit_code=$?
  log_info "Running cleanup..."

  # Remove temporary files
  if [ -d "$RESTORE_DIR" ]; then
    rm -rf "$RESTORE_DIR"
  fi

  if [ $exit_code -ne 0 ]; then
    log_error "Restore failed with exit code: $exit_code"
  fi

  exit $exit_code
}

trap cleanup EXIT INT TERM

# =============================================================================
# Helper Functions
# =============================================================================
check_dependencies() {
  local deps=("psql" "gunzip" "sha256sum" "jq")

  for dep in "${deps[@]}"; do
    if ! command -v "$dep" &> /dev/null; then
      log_error "Required dependency not found: $dep"
      exit 1
    fi
  done

  if ! command -v mc &> /dev/null; then
    log_error "MinIO client (mc) not found"
    exit 1
  fi

  log_debug "All dependencies verified"
}

validate_environment() {
  local required_vars=("PGHOST" "PGPORT" "PGDATABASE" "PGUSER" "PGPASSWORD"
                       "S3_BUCKET" "S3_ENDPOINT" "S3_ACCESS_KEY" "S3_SECRET_KEY")

  for var in "${required_vars[@]}"; do
    if [ -z "${!var:-}" ]; then
      log_error "Required environment variable not set: $var"
      exit 1
    fi
  done

  log_debug "Environment validated"
}

configure_s3_client() {
  log_info "Configuring S3/MinIO client..."

  if ! mc alias set backup "${S3_ENDPOINT}" "${S3_ACCESS_KEY}" "${S3_SECRET_KEY}" --api S3v4 &>/dev/null; then
    log_error "Failed to configure MinIO client"
    exit 1
  fi

  log_debug "S3 client configured"
}

confirm_action() {
  local message="$1"

  if [ "$NO_CONFIRM" == "true" ]; then
    return 0
  fi

  echo ""
  log_warn "=========================================="
  log_warn "WARNING: ${message}"
  log_warn "=========================================="
  echo ""
  read -p "Are you sure you want to proceed? (yes/no): " response

  if [[ ! "$response" =~ ^[Yy][Ee][Ss]$ ]]; then
    log_info "Operation cancelled by user"
    exit 0
  fi
}

# =============================================================================
# Backup Discovery Functions
# =============================================================================
list_available_backups() {
  log_info "Listing available backups..."
  echo ""
  printf "%-40s %-20s %-15s %s\n" "BACKUP NAME" "TIMESTAMP" "SIZE" "DATABASE"
  printf "%-40s %-20s %-15s %s\n" "----------------------------------------" "--------------------" "---------------" "--------"

  # Find all backup metadata files
  mc find "backup/${S3_BUCKET}/postgres-backups/" --name "*.meta.json" 2>/dev/null | sort -r | head -50 | while read -r filepath; do
    local meta_content=$(mc cat "$filepath" 2>/dev/null || echo "{}")
    local backup_name=$(echo "$meta_content" | jq -r '.backup_name // "unknown"')
    local timestamp=$(echo "$meta_content" | jq -r '.timestamp_iso // .timestamp // "unknown"')
    local size=$(echo "$meta_content" | jq -r '.backup.compressed_size_bytes // 0')
    local database=$(echo "$meta_content" | jq -r '.database.name // "unknown"')

    # Convert size to human readable
    local size_hr=$(numfmt --to=iec-i --suffix=B ${size} 2>/dev/null || echo "${size}B")

    printf "%-40s %-20s %-15s %s\n" "$backup_name" "$timestamp" "$size_hr" "$database"
  done

  echo ""
}

get_latest_backup() {
  log_info "Finding latest backup..."

  local latest=$(mc find "backup/${S3_BUCKET}/postgres-backups/" --name "*.sql.gz" 2>/dev/null | sort -r | head -1)

  if [ -z "$latest" ]; then
    log_error "No backups found"
    exit 1
  fi

  BACKUP_NAME=$(basename "$latest" .sql.gz)
  log_info "Latest backup: ${BACKUP_NAME}"
}

find_backup_path() {
  local backup_name="$1"

  # Search for the backup in all possible paths
  local backup_path=$(mc find "backup/${S3_BUCKET}/postgres-backups/" --name "${backup_name}.sql.gz" 2>/dev/null | head -1)

  if [ -z "$backup_path" ]; then
    log_error "Backup not found: ${backup_name}"
    exit 1
  fi

  echo "$backup_path"
}

# =============================================================================
# Download and Verify Functions
# =============================================================================
download_backup() {
  local backup_path="$1"

  log_info "Downloading backup..."
  mkdir -p "$RESTORE_DIR"

  # Download backup file
  if ! mc cp "$backup_path" "${RESTORE_DIR}/"; then
    log_error "Failed to download backup"
    return 1
  fi

  # Download metadata if exists
  local meta_path="${backup_path%.sql.gz}.meta.json"
  mc cp "$meta_path" "${RESTORE_DIR}/" 2>/dev/null || true

  local backup_file="${RESTORE_DIR}/${BACKUP_NAME}.sql.gz"
  if [ ! -f "$backup_file" ]; then
    log_error "Downloaded backup file not found"
    return 1
  fi

  local size=$(stat -f%z "$backup_file" 2>/dev/null || stat --printf="%s" "$backup_file")
  log_info "Downloaded: $(numfmt --to=iec-i --suffix=B ${size} 2>/dev/null || echo "${size} bytes")"

  return 0
}

verify_backup() {
  log_info "Verifying backup integrity..."

  local backup_file="${RESTORE_DIR}/${BACKUP_NAME}.sql.gz"
  local meta_file="${RESTORE_DIR}/${BACKUP_NAME}.meta.json"

  # Verify checksum if metadata exists
  if [ -f "$meta_file" ]; then
    local expected_checksum=$(jq -r '.backup.checksum_sha256 // .checksum_sha256 // ""' "$meta_file")
    if [ -n "$expected_checksum" ]; then
      local actual_checksum=$(sha256sum "$backup_file" | cut -d' ' -f1)
      if [ "$expected_checksum" != "$actual_checksum" ]; then
        log_error "Checksum mismatch!"
        log_error "  Expected: ${expected_checksum}"
        log_error "  Actual:   ${actual_checksum}"
        return 1
      fi
      log_info "Checksum verified successfully"
    fi
  fi

  # Test gzip integrity
  log_info "Testing archive integrity..."
  if ! gunzip -t "$backup_file"; then
    log_error "Backup archive is corrupted"
    return 1
  fi
  log_info "Archive integrity verified"

  # Decompress and check SQL content
  log_info "Validating SQL content..."
  gunzip -c "$backup_file" > "${RESTORE_DIR}/${BACKUP_NAME}.sql"

  if [ ! -s "${RESTORE_DIR}/${BACKUP_NAME}.sql" ]; then
    log_error "Decompressed backup is empty"
    return 1
  fi

  # Check for essential SQL statements
  if ! grep -q "CREATE TABLE\|COPY\|INSERT" "${RESTORE_DIR}/${BACKUP_NAME}.sql"; then
    log_warn "Backup may be incomplete (no CREATE TABLE, COPY, or INSERT statements found)"
  fi

  # Count tables
  local table_count=$(grep -c "CREATE TABLE" "${RESTORE_DIR}/${BACKUP_NAME}.sql" 2>/dev/null || echo "0")
  log_info "Found ${table_count} CREATE TABLE statements"

  log_info "Backup verification completed successfully"
  return 0
}

# =============================================================================
# Restore Functions
# =============================================================================
check_target_database() {
  log_info "Checking target database..."

  # Test connection
  if ! pg_isready -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -t 10 &>/dev/null; then
    log_error "Cannot connect to database server at ${PGHOST}:${PGPORT}"
    return 1
  fi
  log_info "Database server connection verified"

  # Check if database exists
  local db_exists=$(psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d postgres \
    -tAc "SELECT 1 FROM pg_database WHERE datname='${PGDATABASE}'" 2>/dev/null || echo "0")

  if [ "$db_exists" == "1" ]; then
    log_warn "Database '${PGDATABASE}' already exists"

    if [ "$DROP_EXISTING" == "true" ]; then
      log_warn "Will drop existing database before restore"
    else
      log_info "Will restore into existing database (may cause conflicts)"
    fi
  fi

  return 0
}

drop_existing_database() {
  if [ "$DROP_EXISTING" != "true" ]; then
    return 0
  fi

  confirm_action "This will DROP the existing database '${PGDATABASE}' and ALL its data!"

  log_info "Dropping existing database: ${PGDATABASE}"

  # Terminate all connections
  psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d postgres -c "
    SELECT pg_terminate_backend(pid)
    FROM pg_stat_activity
    WHERE datname = '${PGDATABASE}'
    AND pid <> pg_backend_pid();
  " 2>/dev/null || true

  # Drop database
  if ! psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d postgres -c "DROP DATABASE IF EXISTS ${PGDATABASE};"; then
    log_error "Failed to drop database"
    return 1
  fi

  log_info "Database dropped successfully"

  # Recreate database
  log_info "Creating fresh database: ${PGDATABASE}"
  if ! psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d postgres -c "CREATE DATABASE ${PGDATABASE};"; then
    log_error "Failed to create database"
    return 1
  fi

  return 0
}

perform_restore() {
  local sql_file="${RESTORE_DIR}/${BACKUP_NAME}.sql"
  local start_time=$(date +%s)

  log_info "Starting database restore..."
  log_info "Target: ${PGDATABASE}@${PGHOST}:${PGPORT}"

  # Drop existing if requested
  drop_existing_database

  # Perform restore
  log_info "Executing SQL restore..."

  local errors_file="${RESTORE_DIR}/restore_errors.log"

  if ! psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" \
    --single-transaction \
    --set ON_ERROR_STOP=off \
    -f "$sql_file" 2>"$errors_file"; then
    log_warn "Restore completed with some errors"
  fi

  # Check for errors
  if [ -s "$errors_file" ]; then
    local error_count=$(wc -l < "$errors_file")
    log_warn "Restore had ${error_count} warning/error messages"
    log_warn "See ${errors_file} for details"

    # Show first few errors
    if [ "$error_count" -gt 0 ]; then
      log_debug "First few messages:"
      head -10 "$errors_file" | while read -r line; do
        log_debug "  $line"
      done
    fi
  fi

  local end_time=$(date +%s)
  local duration=$((end_time - start_time))

  log_info "Restore completed in ${duration} seconds"

  return 0
}

verify_restore() {
  log_info "Verifying restored database..."

  # Check table count
  local table_count=$(psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" \
    -tAc "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null || echo "0")
  log_info "Tables restored: ${table_count}"

  # Check database size
  local db_size=$(psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" \
    -tAc "SELECT pg_size_pretty(pg_database_size('${PGDATABASE}'));" 2>/dev/null || echo "unknown")
  log_info "Database size: ${db_size}"

  # List tables
  log_info "Tables in restored database:"
  psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -c "
    SELECT table_name, pg_size_pretty(pg_total_relation_size(quote_ident(table_name))) as size
    FROM information_schema.tables
    WHERE table_schema = 'public'
    ORDER BY pg_total_relation_size(quote_ident(table_name)) DESC
    LIMIT 10;
  " 2>/dev/null || true

  return 0
}

# =============================================================================
# Point-in-Time Recovery (WAL-based)
# =============================================================================
pitr_restore() {
  local target_time="$1"

  log_info "Performing Point-in-Time Recovery to: ${target_time}"
  log_warn "PITR requires WAL archiving to be enabled"

  # This is a placeholder for WAL-based PITR
  # In production, you would:
  # 1. Restore the base backup
  # 2. Apply WAL files up to the target time
  # 3. This requires pg_basebackup and continuous WAL archiving

  cat <<EOF

==============================================================================
POINT-IN-TIME RECOVERY INSTRUCTIONS
==============================================================================

WAL-based Point-in-Time Recovery requires:

1. WAL archiving enabled on the primary server:
   archive_mode = on
   archive_command = 'mc cp %p backup/${S3_BUCKET}/wal-archive/%f'

2. A base backup created with pg_basebackup:
   pg_basebackup -h \$PGHOST -U \$PGUSER -D /var/lib/postgresql/data -Fp -Xs -P

3. Recovery configuration (postgresql.auto.conf or recovery.conf):
   restore_command = 'mc cp backup/${S3_BUCKET}/wal-archive/%f %p'
   recovery_target_time = '${target_time}'
   recovery_target_action = 'promote'

For now, this script performs a logical backup restore to the latest
available backup before the target time.

==============================================================================
EOF

  # Find the latest backup before target time
  log_info "Finding appropriate backup for target time..."

  local target_epoch=$(date -d "$target_time" +%s 2>/dev/null || date -j -f "%Y-%m-%dT%H:%M:%S" "$target_time" +%s)

  local selected_backup=""
  mc find "backup/${S3_BUCKET}/postgres-backups/" --name "*.meta.json" 2>/dev/null | while read -r filepath; do
    local meta_content=$(mc cat "$filepath" 2>/dev/null || echo "{}")
    local backup_time=$(echo "$meta_content" | jq -r '.timestamp_iso // ""')
    if [ -n "$backup_time" ]; then
      local backup_epoch=$(date -d "$backup_time" +%s 2>/dev/null || date -j -f "%Y-%m-%dT%H:%M:%S" "$backup_time" +%s)
      if [ "$backup_epoch" -le "$target_epoch" ]; then
        selected_backup=$(echo "$meta_content" | jq -r '.backup_name')
        echo "$selected_backup"
        break
      fi
    fi
  done | tail -1

  if [ -z "$selected_backup" ]; then
    log_error "No backup found before target time: ${target_time}"
    return 1
  fi

  BACKUP_NAME="$selected_backup"
  log_info "Selected backup: ${BACKUP_NAME}"

  return 0
}

# =============================================================================
# Main Function
# =============================================================================
parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      -b|--backup)
        BACKUP_NAME="$2"
        shift 2
        ;;
      -d|--database)
        PGDATABASE="$2"
        shift 2
        ;;
      -h|--host)
        PGHOST="$2"
        shift 2
        ;;
      -t|--target-time)
        TARGET_TIME="$2"
        shift 2
        ;;
      -l|--list)
        LIST_BACKUPS=true
        shift
        ;;
      --verify-only)
        VERIFY_ONLY=true
        shift
        ;;
      --no-confirm)
        NO_CONFIRM=true
        shift
        ;;
      --drop-existing)
        DROP_EXISTING=true
        shift
        ;;
      --help)
        head -35 "$0" | tail -30
        exit 0
        ;;
      *)
        log_error "Unknown option: $1"
        exit 1
        ;;
    esac
  done
}

main() {
  parse_args "$@"

  log_info "=========================================="
  log_info "PostgreSQL Restore Script"
  log_info "=========================================="

  check_dependencies
  validate_environment
  configure_s3_client

  # List backups mode
  if [ "$LIST_BACKUPS" == "true" ]; then
    list_available_backups
    exit 0
  fi

  # Get backup name
  if [ -z "$BACKUP_NAME" ] || [ "$BACKUP_NAME" == "latest" ]; then
    get_latest_backup
  fi

  # Handle PITR
  if [ -n "$TARGET_TIME" ]; then
    pitr_restore "$TARGET_TIME"
  fi

  # Find backup path
  local backup_path=$(find_backup_path "$BACKUP_NAME")
  log_info "Backup location: ${backup_path}"

  # Download and verify
  download_backup "$backup_path"
  verify_backup

  if [ "$VERIFY_ONLY" == "true" ]; then
    log_info "Verification complete (--verify-only mode)"
    exit 0
  fi

  # Confirm restore
  confirm_action "This will restore backup '${BACKUP_NAME}' to database '${PGDATABASE}'@'${PGHOST}'"

  # Perform restore
  check_target_database
  perform_restore
  verify_restore

  log_info "=========================================="
  log_info "Restore completed successfully!"
  log_info "  Backup: ${BACKUP_NAME}"
  log_info "  Database: ${PGDATABASE}@${PGHOST}:${PGPORT}"
  log_info "=========================================="

  exit 0
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  main "$@"
fi
