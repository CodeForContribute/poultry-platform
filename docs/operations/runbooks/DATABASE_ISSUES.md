# Database Issues Runbook

## Overview

This runbook covers troubleshooting procedures for PostgreSQL database issues in the Poultry Platform.

**Database**: PostgreSQL 16
**Deployment**: Kubernetes StatefulSet / Docker Compose
**Connection Pool**: HikariCP

---

## Table of Contents

1. [Quick Diagnostics](#quick-diagnostics)
2. [Common Issues](#common-issues)
3. [Connection Issues](#connection-issues)
4. [Performance Issues](#performance-issues)
5. [Replication Issues](#replication-issues)
6. [Storage Issues](#storage-issues)
7. [Recovery Procedures](#recovery-procedures)

---

## Quick Diagnostics

### Health Check Commands

```bash
# Kubernetes: Check pod status
kubectl get pods -n poultry-platform -l app=postgres

# Kubernetes: Get PostgreSQL logs
kubectl logs -f postgres-0 -n poultry-platform --tail=100

# Docker: Check container status
docker compose ps postgres
docker compose logs -f postgres --tail=100

# Application health endpoint
curl -s https://api.poultry-platform.com/api/actuator/health | jq '.components.db'
```

### PostgreSQL Status Queries

```sql
-- Check active connections
SELECT count(*), state
FROM pg_stat_activity
GROUP BY state;

-- Check long-running queries
SELECT pid, now() - pg_stat_activity.query_start AS duration, query, state
FROM pg_stat_activity
WHERE (now() - pg_stat_activity.query_start) > interval '5 minutes'
AND state != 'idle';

-- Check blocked queries
SELECT blocked_locks.pid AS blocked_pid,
       blocked_activity.usename AS blocked_user,
       blocking_locks.pid AS blocking_pid,
       blocking_activity.usename AS blocking_user,
       blocked_activity.query AS blocked_statement
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks
    ON blocking_locks.locktype = blocked_locks.locktype
    AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
    AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
    AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
    AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
    AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
    AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
    AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
    AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
    AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
    AND blocking_locks.pid != blocked_locks.pid
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;

-- Check database size
SELECT pg_database.datname, pg_size_pretty(pg_database_size(pg_database.datname))
FROM pg_database
ORDER BY pg_database_size(pg_database.datname) DESC;

-- Check table sizes
SELECT relname AS table_name,
       pg_size_pretty(pg_total_relation_size(relid)) AS total_size,
       pg_size_pretty(pg_relation_size(relid)) AS data_size,
       pg_size_pretty(pg_indexes_size(relid)) AS index_size
FROM pg_catalog.pg_statio_user_tables
ORDER BY pg_total_relation_size(relid) DESC
LIMIT 20;
```

---

## Common Issues

### Issue: Database Connection Refused

**Symptoms**:
- Application logs show "Connection refused"
- Health check returns unhealthy for DB component

**Diagnosis**:
```bash
# Check if PostgreSQL is running
kubectl get pods -n poultry-platform -l app=postgres
kubectl describe pod postgres-0 -n poultry-platform

# Check PostgreSQL logs
kubectl logs postgres-0 -n poultry-platform --tail=50

# Test connectivity
kubectl exec -it postgres-0 -n poultry-platform -- pg_isready -U postgres
```

**Resolution**:
1. If pod is not running, check events:
   ```bash
   kubectl get events -n poultry-platform --sort-by='.lastTimestamp' | grep postgres
   ```

2. If pod is CrashLoopBackOff, check logs for startup errors

3. If pod is running but connections refused:
   ```bash
   # Check PostgreSQL is listening
   kubectl exec -it postgres-0 -n poultry-platform -- netstat -tlnp | grep 5432

   # Check pg_hba.conf configuration
   kubectl exec -it postgres-0 -n poultry-platform -- cat /var/lib/postgresql/data/pg_hba.conf
   ```

4. Restart PostgreSQL if necessary:
   ```bash
   kubectl rollout restart statefulset/postgres -n poultry-platform
   ```

### Issue: Connection Pool Exhausted

**Symptoms**:
- "Connection pool exhausted" errors in application logs
- `hikaricp_connections_active` metric at max
- Slow response times or timeouts

**Diagnosis**:
```bash
# Check HikariCP metrics
curl -s https://api.poultry-platform.com/api/actuator/prometheus | grep hikaricp

# Check active connections in PostgreSQL
kubectl exec -it postgres-0 -n poultry-platform -- psql -U postgres -d poultry -c \
  "SELECT count(*), application_name FROM pg_stat_activity GROUP BY application_name;"
```

**Resolution**:
1. Identify connection leaks:
   ```sql
   SELECT pid, usename, application_name, client_addr,
          state, query_start, query
   FROM pg_stat_activity
   WHERE state != 'idle'
   ORDER BY query_start;
   ```

2. Kill idle connections if necessary:
   ```sql
   SELECT pg_terminate_backend(pid)
   FROM pg_stat_activity
   WHERE state = 'idle'
   AND query_start < NOW() - INTERVAL '10 minutes';
   ```

3. Increase connection pool size (temporary):
   ```yaml
   # Update configmap
   hikari:
     maximum-pool-size: 30  # Increase from 20
   ```

4. Scale application to distribute connections

### Issue: Slow Queries

**Symptoms**:
- High response times
- CPU spikes on database server
- Timeout errors

**Diagnosis**:
```sql
-- Find slow queries
SELECT pid, now() - pg_stat_activity.query_start AS duration,
       query, state
FROM pg_stat_activity
WHERE (now() - pg_stat_activity.query_start) > interval '30 seconds'
AND state = 'active';

-- Check for missing indexes
SELECT schemaname, relname, seq_scan, seq_tup_read,
       idx_scan, idx_tup_fetch,
       seq_tup_read / NULLIF(seq_scan, 0) AS avg_seq_tup
FROM pg_stat_user_tables
WHERE seq_scan > 100
ORDER BY seq_tup_read DESC
LIMIT 20;

-- Check query statistics (requires pg_stat_statements)
SELECT query, calls, total_exec_time, mean_exec_time, rows
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;
```

**Resolution**:
1. Kill problematic query if causing issues:
   ```sql
   SELECT pg_terminate_backend(<pid>);
   ```

2. Analyze and optimize query:
   ```sql
   EXPLAIN ANALYZE <query>;
   ```

3. Add missing indexes if identified

4. Update table statistics:
   ```sql
   ANALYZE <table_name>;
   ```

---

## Connection Issues

### Troubleshooting Connection Failures

```bash
# 1. Verify network connectivity
kubectl exec -it poultry-backend-xxx -n poultry-platform -- nc -zv postgres 5432

# 2. Verify credentials
kubectl get secret postgres-credentials -n poultry-platform -o jsonpath='{.data.password}' | base64 -d

# 3. Test connection from application pod
kubectl exec -it poultry-backend-xxx -n poultry-platform -- \
  psql -h postgres -U postgres -d poultry -c "SELECT 1;"

# 4. Check DNS resolution
kubectl exec -it poultry-backend-xxx -n poultry-platform -- nslookup postgres
```

### Connection String Format

```
jdbc:postgresql://postgres:5432/poultry?sslmode=prefer&connectTimeout=10&socketTimeout=30
```

### HikariCP Configuration Tuning

```yaml
# application.yml recommended settings
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000      # 5 minutes
      connection-timeout: 20000  # 20 seconds
      max-lifetime: 1200000     # 20 minutes
      leak-detection-threshold: 60000  # 1 minute
```

---

## Performance Issues

### Identifying Performance Bottlenecks

```sql
-- Table bloat estimation
SELECT schemaname, relname,
       n_dead_tup, n_live_tup,
       round(n_dead_tup * 100.0 / NULLIF(n_live_tup + n_dead_tup, 0), 2) AS dead_pct
FROM pg_stat_user_tables
WHERE n_dead_tup > 10000
ORDER BY n_dead_tup DESC;

-- Index usage statistics
SELECT schemaname, relname, indexrelname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC
LIMIT 20;

-- Cache hit ratio
SELECT
    sum(heap_blks_read) as heap_read,
    sum(heap_blks_hit)  as heap_hit,
    sum(heap_blks_hit) / NULLIF(sum(heap_blks_hit) + sum(heap_blks_read), 0) as ratio
FROM pg_statio_user_tables;

-- Most written tables
SELECT schemaname, relname, n_tup_ins, n_tup_upd, n_tup_del
FROM pg_stat_user_tables
ORDER BY n_tup_ins + n_tup_upd + n_tup_del DESC
LIMIT 20;
```

### Maintenance Operations

```sql
-- Manual VACUUM (for urgent bloat)
VACUUM (VERBOSE, ANALYZE) table_name;

-- Full VACUUM (requires downtime or low traffic)
VACUUM FULL table_name;

-- Reindex
REINDEX TABLE table_name;

-- Update statistics
ANALYZE table_name;
```

### Performance Tuning Parameters

```sql
-- Check current settings
SHOW work_mem;
SHOW shared_buffers;
SHOW effective_cache_size;
SHOW maintenance_work_mem;

-- Recommended settings (adjust based on available RAM)
-- For 8GB RAM server:
ALTER SYSTEM SET shared_buffers = '2GB';
ALTER SYSTEM SET effective_cache_size = '6GB';
ALTER SYSTEM SET work_mem = '64MB';
ALTER SYSTEM SET maintenance_work_mem = '512MB';
-- Requires restart
```

---

## Replication Issues

### Check Replication Status

```sql
-- On primary
SELECT client_addr, state, sent_lsn, write_lsn, flush_lsn, replay_lsn,
       pg_wal_lsn_diff(sent_lsn, replay_lsn) AS replication_lag
FROM pg_stat_replication;

-- On replica
SELECT pg_last_wal_receive_lsn(), pg_last_wal_replay_lsn(),
       pg_last_xact_replay_timestamp();
```

### Replication Lag Resolution

1. Check network connectivity between primary and replica
2. Check disk I/O on replica
3. Check for long-running transactions on replica
4. Consider increasing `max_wal_senders` if needed

---

## Storage Issues

### Disk Space Management

```bash
# Check disk usage
kubectl exec -it postgres-0 -n poultry-platform -- df -h /var/lib/postgresql/data

# Check data directory size
kubectl exec -it postgres-0 -n poultry-platform -- du -sh /var/lib/postgresql/data/*
```

```sql
-- Find largest tables
SELECT relname, pg_size_pretty(pg_total_relation_size(relid))
FROM pg_catalog.pg_statio_user_tables
ORDER BY pg_total_relation_size(relid) DESC
LIMIT 10;

-- Find tables with most dead tuples
SELECT relname, n_dead_tup, pg_size_pretty(pg_total_relation_size(relid))
FROM pg_stat_user_tables
WHERE n_dead_tup > 10000
ORDER BY n_dead_tup DESC;
```

### Emergency Disk Space Recovery

```sql
-- 1. Truncate/delete old audit logs if applicable
DELETE FROM audit_log WHERE created_at < NOW() - INTERVAL '90 days';

-- 2. Vacuum to reclaim space
VACUUM FULL;

-- 3. Clear old WAL files (if safe)
SELECT pg_switch_wal();

-- 4. Drop unused indexes
DROP INDEX IF EXISTS idx_unused_index;
```

---

## Recovery Procedures

### Point-in-Time Recovery (PITR)

```bash
# 1. Stop the application
kubectl scale deployment poultry-backend -n poultry-platform --replicas=0

# 2. Restore from backup
pg_restore -h localhost -U postgres -d poultry_restore backup.dump

# 3. Apply WAL files up to target time
# (Configured in recovery.conf or postgresql.conf)
recovery_target_time = '2024-01-15 10:00:00 UTC'
recovery_target_action = 'pause'

# 4. Verify data and promote
SELECT pg_wal_replay_resume();

# 5. Restart application
kubectl scale deployment poultry-backend -n poultry-platform --replicas=3
```

### Emergency Database Restart

```bash
# Kubernetes
kubectl rollout restart statefulset/postgres -n poultry-platform

# Docker Compose
docker compose restart postgres

# Force restart if unresponsive
kubectl delete pod postgres-0 -n poultry-platform --force --grace-period=0
```

### Corruption Recovery

If database corruption is suspected:

1. **Stop writes immediately**
2. **Take a filesystem backup**
3. **Run integrity checks**:
   ```sql
   -- Check for corruption
   SELECT count(*) FROM each_table;  -- Will error on corruption
   ```
4. **Contact DBA team for recovery options**

---

## Monitoring Queries

### Connection Monitoring

```sql
-- Connection by state
SELECT state, count(*)
FROM pg_stat_activity
GROUP BY state;

-- Connections by application
SELECT application_name, count(*)
FROM pg_stat_activity
GROUP BY application_name;

-- Max connections vs current
SELECT max_conn, used, res_for_super, max_conn - used - res_for_super AS available
FROM (SELECT count(*) used FROM pg_stat_activity) t1,
     (SELECT setting::int res_for_super FROM pg_settings WHERE name='superuser_reserved_connections') t2,
     (SELECT setting::int max_conn FROM pg_settings WHERE name='max_connections') t3;
```

### Transaction Monitoring

```sql
-- Long-running transactions
SELECT pid, now() - xact_start AS duration, query, state
FROM pg_stat_activity
WHERE xact_start IS NOT NULL
AND state != 'idle'
ORDER BY xact_start;

-- Idle in transaction
SELECT pid, now() - state_change AS idle_duration, query
FROM pg_stat_activity
WHERE state = 'idle in transaction'
AND state_change < NOW() - INTERVAL '5 minutes';
```

---

## Escalation

If unable to resolve:
1. Contact Database Administrator
2. Escalate to P2 if affecting production
3. Consider engaging PostgreSQL support (if contracted)

## Related Runbooks

- [API Issues](./API_ISSUES.md)
- [Deployment Rollback](./DEPLOYMENT_ROLLBACK.md)
- [Disaster Recovery](../DISASTER_RECOVERY.md)
