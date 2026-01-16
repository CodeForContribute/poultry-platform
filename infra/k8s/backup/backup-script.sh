#!/bin/bash
#
# PostgreSQL Backup Script for Poultry Platform
# This script performs full database backups with compression and uploads to S3/MinIO
#
# Usage:
#   ./backup-script.sh [options]
#
# Options:
#   -d, --database    Database name (default: from PGDATABASE env)
#   -h, --host        Database host (default: from PGHOST env)
#   -o, --output      Output directory (default: /backup)
#   -r, --retention   Retention days (default: 30)
#   --dry-run         Perform backup without uploading
#   --help            Show this help message
#

set -euo pipefail

# =============================================================================
# Configuration
# =============================================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${BACKUP_DIR:-/backup}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
DRY_RUN=false
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
      DEBUG) color="\033[36m" ;;  # Cyan
      INFO)  color="\033[32m" ;;  # Green
      WARN)  color="\033[33m" ;;  # Yellow
      ERROR) color="\033[31m" ;;  # Red
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
  if [ -n "${TEMP_DIR:-}" ] && [ -d "$TEMP_DIR" ]; then
    rm -rf "$TEMP_DIR"
  fi

  # Remove local backup files
  rm -f "${BACKUP_DIR}/${BACKUP_NAME}.sql" 2>/dev/null || true
  rm -f "${BACKUP_DIR}/${BACKUP_NAME}.sql.gz" 2>/dev/null || true
  rm -f "${BACKUP_DIR}/${BACKUP_NAME}.meta.json" 2>/dev/null || true

  if [ $exit_code -ne 0 ]; then
    log_error "Backup failed with exit code: $exit_code"
    # Send alert on failure
    send_alert "FAILURE" "PostgreSQL backup failed with exit code: $exit_code"
  fi

  exit $exit_code
}

trap cleanup EXIT INT TERM

# =============================================================================
# Helper Functions
# =============================================================================
send_alert() {
  local status="$1"
  local message="$2"

  # Send to Prometheus Pushgateway if configured
  if [ -n "${PUSHGATEWAY_URL:-}" ]; then
    local job_status=0
    [ "$status" == "SUCCESS" ] && job_status=1

    cat <<EOF | curl --data-binary @- "${PUSHGATEWAY_URL}/metrics/job/postgres_backup" 2>/dev/null || true
# TYPE postgres_backup_success gauge
postgres_backup_success ${job_status}
# TYPE postgres_backup_timestamp gauge
postgres_backup_timestamp $(date +%s)
# TYPE postgres_backup_duration_seconds gauge
postgres_backup_duration_seconds ${BACKUP_DURATION:-0}
# TYPE postgres_backup_size_bytes gauge
postgres_backup_size_bytes ${BACKUP_SIZE_BYTES:-0}
EOF
    log_debug "Pushed metrics to Prometheus Pushgateway"
  fi

  # Send to Slack if webhook is configured
  if [ -n "${SLACK_WEBHOOK_URL:-}" ]; then
    local emoji=":white_check_mark:"
    [ "$status" != "SUCCESS" ] && emoji=":x:"

    curl -X POST "${SLACK_WEBHOOK_URL}" \
      -H "Content-Type: application/json" \
      -d "{
        \"text\": \"${emoji} PostgreSQL Backup ${status}\",
        \"attachments\": [{
          \"color\": \"$([ "$status" == "SUCCESS" ] && echo "good" || echo "danger")\",
          \"fields\": [
            {\"title\": \"Database\", \"value\": \"${PGDATABASE:-unknown}\", \"short\": true},
            {\"title\": \"Host\", \"value\": \"${PGHOST:-unknown}\", \"short\": true},
            {\"title\": \"Message\", \"value\": \"${message}\", \"short\": false}
          ]
        }]
      }" 2>/dev/null || true
    log_debug "Sent Slack notification"
  fi
}

check_dependencies() {
  local deps=("pg_dump" "gzip" "sha256sum")

  for dep in "${deps[@]}"; do
    if ! command -v "$dep" &> /dev/null; then
      log_error "Required dependency not found: $dep"
      exit 1
    fi
  done

  # Check for MinIO client
  if [ "$DRY_RUN" == "false" ]; then
    if ! command -v mc &> /dev/null; then
      log_error "MinIO client (mc) not found"
      exit 1
    fi
  fi

  log_debug "All dependencies verified"
}

validate_environment() {
  local required_vars=("PGHOST" "PGPORT" "PGDATABASE" "PGUSER" "PGPASSWORD")

  if [ "$DRY_RUN" == "false" ]; then
    required_vars+=("S3_BUCKET" "S3_ENDPOINT" "S3_ACCESS_KEY" "S3_SECRET_KEY")
  fi

  for var in "${required_vars[@]}"; do
    if [ -z "${!var:-}" ]; then
      log_error "Required environment variable not set: $var"
      exit 1
    fi
  done

  log_debug "Environment validated"
}

test_database_connection() {
  log_info "Testing database connection..."

  if ! pg_isready -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -t 10 &>/dev/null; then
    log_error "Cannot connect to database at ${PGHOST}:${PGPORT}"
    exit 1
  fi

  log_info "Database connection successful"
}

configure_s3_client() {
  if [ "$DRY_RUN" == "true" ]; then
    log_info "[DRY-RUN] Skipping S3 client configuration"
    return
  fi

  log_info "Configuring S3/MinIO client..."

  if ! mc alias set backup "${S3_ENDPOINT}" "${S3_ACCESS_KEY}" "${S3_SECRET_KEY}" --api S3v4 &>/dev/null; then
    log_error "Failed to configure MinIO client"
    exit 1
  fi

  # Ensure bucket exists
  if ! mc ls "backup/${S3_BUCKET}" &>/dev/null; then
    log_info "Creating backup bucket: ${S3_BUCKET}"
    mc mb "backup/${S3_BUCKET}" || true
  fi

  # Configure bucket versioning (if supported)
  mc version enable "backup/${S3_BUCKET}" 2>/dev/null || true

  log_debug "S3 client configured successfully"
}

# =============================================================================
# Backup Functions
# =============================================================================
perform_backup() {
  local START_TIME=$(date +%s)
  BACKUP_NAME="poultry_backup_${TIMESTAMP}"

  log_info "Starting PostgreSQL backup: ${BACKUP_NAME}"
  log_info "Database: ${PGDATABASE}@${PGHOST}:${PGPORT}"

  # Create backup directory
  mkdir -p "${BACKUP_DIR}"

  # Get database size estimate
  local db_size=$(psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" \
    -t -c "SELECT pg_size_pretty(pg_database_size('${PGDATABASE}'));" 2>/dev/null || echo "unknown")
  log_info "Database size: ${db_size}"

  # Perform database dump with progress
  log_info "Creating database dump..."
  local dump_start=$(date +%s)

  if ! pg_dump \
    --host="${PGHOST}" \
    --port="${PGPORT}" \
    --username="${PGUSER}" \
    --dbname="${PGDATABASE}" \
    --format=plain \
    --no-owner \
    --no-privileges \
    --clean \
    --if-exists \
    --verbose \
    --file="${BACKUP_DIR}/${BACKUP_NAME}.sql" 2>&1 | while read -r line; do
      log_debug "pg_dump: $line"
    done; then
    log_error "pg_dump failed"
    return 1
  fi

  local dump_end=$(date +%s)
  log_info "Dump completed in $((dump_end - dump_start)) seconds"

  # Verify backup file
  if [ ! -s "${BACKUP_DIR}/${BACKUP_NAME}.sql" ]; then
    log_error "Backup file is empty or does not exist"
    return 1
  fi

  local uncompressed_size=$(stat -f%z "${BACKUP_DIR}/${BACKUP_NAME}.sql" 2>/dev/null || \
                            stat --printf="%s" "${BACKUP_DIR}/${BACKUP_NAME}.sql" 2>/dev/null)
  log_info "Uncompressed backup size: $(numfmt --to=iec-i --suffix=B ${uncompressed_size} 2>/dev/null || echo "${uncompressed_size} bytes")"

  # Compress backup
  log_info "Compressing backup with gzip -9..."
  local compress_start=$(date +%s)

  if ! gzip -9 -f "${BACKUP_DIR}/${BACKUP_NAME}.sql"; then
    log_error "Compression failed"
    return 1
  fi

  local compress_end=$(date +%s)
  log_info "Compression completed in $((compress_end - compress_start)) seconds"

  BACKUP_SIZE_BYTES=$(stat -f%z "${BACKUP_DIR}/${BACKUP_NAME}.sql.gz" 2>/dev/null || \
                      stat --printf="%s" "${BACKUP_DIR}/${BACKUP_NAME}.sql.gz" 2>/dev/null)
  log_info "Compressed backup size: $(numfmt --to=iec-i --suffix=B ${BACKUP_SIZE_BYTES} 2>/dev/null || echo "${BACKUP_SIZE_BYTES} bytes")"

  # Calculate checksum
  BACKUP_CHECKSUM=$(sha256sum "${BACKUP_DIR}/${BACKUP_NAME}.sql.gz" | cut -d' ' -f1)
  log_info "Backup checksum (SHA256): ${BACKUP_CHECKSUM}"

  # Create metadata
  create_metadata

  local END_TIME=$(date +%s)
  BACKUP_DURATION=$((END_TIME - START_TIME))
  log_info "Backup created successfully in ${BACKUP_DURATION} seconds"

  return 0
}

create_metadata() {
  log_debug "Creating backup metadata..."

  cat > "${BACKUP_DIR}/${BACKUP_NAME}.meta.json" <<EOF
{
  "backup_name": "${BACKUP_NAME}",
  "timestamp": "${TIMESTAMP}",
  "timestamp_iso": "$(date -Iseconds)",
  "database": {
    "name": "${PGDATABASE}",
    "host": "${PGHOST}",
    "port": "${PGPORT}"
  },
  "backup": {
    "type": "full",
    "format": "plain",
    "compression": "gzip",
    "compressed_size_bytes": ${BACKUP_SIZE_BYTES},
    "checksum_sha256": "${BACKUP_CHECKSUM}"
  },
  "retention": {
    "days": ${RETENTION_DAYS},
    "expires_at": "$(date -d "+${RETENTION_DAYS} days" -Iseconds 2>/dev/null || date -v+${RETENTION_DAYS}d -Iseconds)"
  },
  "environment": {
    "hostname": "$(hostname)",
    "pg_version": "$(pg_dump --version | head -1)"
  }
}
EOF
}

upload_backup() {
  if [ "$DRY_RUN" == "true" ]; then
    log_info "[DRY-RUN] Would upload ${BACKUP_NAME}.sql.gz to ${S3_BUCKET}"
    return 0
  fi

  log_info "Uploading backup to S3..."

  local S3_PREFIX="postgres-backups/$(date +%Y/%m)"

  # Upload backup file
  if ! mc cp "${BACKUP_DIR}/${BACKUP_NAME}.sql.gz" "backup/${S3_BUCKET}/${S3_PREFIX}/"; then
    log_error "Failed to upload backup to S3"
    return 1
  fi

  # Upload metadata
  if ! mc cp "${BACKUP_DIR}/${BACKUP_NAME}.meta.json" "backup/${S3_BUCKET}/${S3_PREFIX}/"; then
    log_error "Failed to upload metadata to S3"
    return 1
  fi

  log_info "Backup uploaded to: s3://${S3_BUCKET}/${S3_PREFIX}/${BACKUP_NAME}.sql.gz"

  # Verify upload
  verify_upload "${S3_PREFIX}"

  return 0
}

verify_upload() {
  local s3_prefix="$1"

  log_info "Verifying uploaded backup..."

  # Check file exists in S3
  if ! mc stat "backup/${S3_BUCKET}/${s3_prefix}/${BACKUP_NAME}.sql.gz" &>/dev/null; then
    log_error "Uploaded backup not found in S3"
    return 1
  fi

  # Get remote size
  local remote_size=$(mc stat "backup/${S3_BUCKET}/${s3_prefix}/${BACKUP_NAME}.sql.gz" 2>&1 | grep "Size" | awk '{print $3}')
  log_info "Remote file size: ${remote_size} bytes"

  # Compare sizes
  if [ "${remote_size}" != "${BACKUP_SIZE_BYTES}" ]; then
    log_warn "Size mismatch: local=${BACKUP_SIZE_BYTES}, remote=${remote_size}"
  fi

  log_info "Upload verification completed"
  return 0
}

cleanup_old_backups() {
  if [ "$DRY_RUN" == "true" ]; then
    log_info "[DRY-RUN] Would cleanup backups older than ${RETENTION_DAYS} days"
    return 0
  fi

  log_info "Applying ${RETENTION_DAYS}-day retention policy..."

  local cutoff_date=$(date -d "-${RETENTION_DAYS} days" +%Y%m%d 2>/dev/null || \
                      date -v-${RETENTION_DAYS}d +%Y%m%d)
  local deleted_count=0

  # List all backups recursively
  mc find "backup/${S3_BUCKET}/postgres-backups/" --name "poultry_backup_*.sql.gz" 2>/dev/null | while read -r filepath; do
    local filename=$(basename "$filepath")
    if [[ "$filename" =~ ^poultry_backup_([0-9]{8})_ ]]; then
      local file_date="${BASH_REMATCH[1]}"
      if [ "$file_date" -lt "$cutoff_date" ]; then
        log_info "Removing old backup: ${filename}"
        mc rm "$filepath" 2>/dev/null || true
        mc rm "${filepath%.sql.gz}.meta.json" 2>/dev/null || true
        ((deleted_count++))
      fi
    fi
  done

  log_info "Retention cleanup completed. Removed ${deleted_count} old backup(s)"
}

# =============================================================================
# Main Function
# =============================================================================
parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      -d|--database)
        PGDATABASE="$2"
        shift 2
        ;;
      -h|--host)
        PGHOST="$2"
        shift 2
        ;;
      -o|--output)
        BACKUP_DIR="$2"
        shift 2
        ;;
      -r|--retention)
        RETENTION_DAYS="$2"
        shift 2
        ;;
      --dry-run)
        DRY_RUN=true
        shift
        ;;
      --help)
        head -30 "$0" | tail -25
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
  log_info "PostgreSQL Backup Script"
  log_info "=========================================="
  [ "$DRY_RUN" == "true" ] && log_warn "Running in DRY-RUN mode"

  check_dependencies
  validate_environment
  test_database_connection
  configure_s3_client

  if perform_backup; then
    if upload_backup; then
      cleanup_old_backups
      send_alert "SUCCESS" "Backup completed successfully: ${BACKUP_NAME}"
      log_info "=========================================="
      log_info "Backup completed successfully!"
      log_info "  Name: ${BACKUP_NAME}"
      log_info "  Size: ${BACKUP_SIZE_BYTES} bytes"
      log_info "  Duration: ${BACKUP_DURATION} seconds"
      log_info "=========================================="
      exit 0
    fi
  fi

  log_error "Backup failed!"
  exit 1
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  main "$@"
fi
