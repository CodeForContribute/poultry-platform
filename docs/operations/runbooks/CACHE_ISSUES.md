# Cache Issues Runbook

## Overview

This runbook covers troubleshooting procedures for Redis cache issues in the Poultry Platform.

**Cache Provider**: Redis 7.x
**Deployment**: Kubernetes StatefulSet / Docker Compose
**Client**: Spring Data Redis (Lettuce)

---

## Table of Contents

1. [Quick Diagnostics](#quick-diagnostics)
2. [Connection Issues](#connection-issues)
3. [Performance Issues](#performance-issues)
4. [Memory Issues](#memory-issues)
5. [Data Issues](#data-issues)
6. [Cluster Issues](#cluster-issues)
7. [Recovery Procedures](#recovery-procedures)

---

## Quick Diagnostics

### Health Check Commands

```bash
# Kubernetes: Check Redis pod status
kubectl get pods -n poultry-platform -l app=redis

# Kubernetes: Check Redis logs
kubectl logs -f redis-0 -n poultry-platform --tail=50

# Docker: Check Redis container
docker compose ps redis
docker compose logs -f redis --tail=50

# Application health check
curl -s https://api.poultry-platform.com/api/actuator/health | jq '.components.redis'
```

### Redis CLI Commands

```bash
# Connect to Redis
kubectl exec -it redis-0 -n poultry-platform -- redis-cli

# Basic health check
redis-cli PING
# Expected: PONG

# Server info
redis-cli INFO server

# Memory info
redis-cli INFO memory

# Connected clients
redis-cli INFO clients

# Check key count
redis-cli DBSIZE
```

### Key Redis Metrics

```bash
# Get all stats
kubectl exec -it redis-0 -n poultry-platform -- redis-cli INFO

# Key metrics to check:
# - connected_clients: Number of client connections
# - used_memory: Memory in use
# - used_memory_peak: Peak memory usage
# - evicted_keys: Keys evicted due to maxmemory
# - keyspace_hits/misses: Cache hit ratio
# - rejected_connections: Connections rejected
```

---

## Connection Issues

### Issue: Redis Connection Refused

**Symptoms**:
- Application logs show "Connection refused"
- Health check shows Redis as DOWN

**Diagnosis**:
```bash
# Check if Redis is running
kubectl get pods -n poultry-platform -l app=redis
kubectl describe pod redis-0 -n poultry-platform

# Check Redis logs
kubectl logs redis-0 -n poultry-platform --tail=100

# Test connectivity from application pod
kubectl exec -it deployment/poultry-backend -n poultry-platform -- \
  nc -zv redis 6379

# Test Redis ping
kubectl exec -it deployment/poultry-backend -n poultry-platform -- \
  redis-cli -h redis ping
```

**Resolution**:
1. If pod not running, check events and restart:
   ```bash
   kubectl get events -n poultry-platform | grep redis
   kubectl rollout restart statefulset/redis -n poultry-platform
   ```

2. Check resource limits:
   ```bash
   kubectl describe pod redis-0 -n poultry-platform | grep -A 5 "Limits:\|Requests:"
   ```

3. Verify service configuration:
   ```bash
   kubectl get svc redis -n poultry-platform
   kubectl describe svc redis -n poultry-platform
   ```

### Issue: Connection Timeout

**Symptoms**:
- Intermittent timeouts
- Slow Redis operations

**Diagnosis**:
```bash
# Check client connections
kubectl exec -it redis-0 -n poultry-platform -- redis-cli CLIENT LIST

# Check slow log
kubectl exec -it redis-0 -n poultry-platform -- redis-cli SLOWLOG GET 10

# Check network latency
kubectl exec -it deployment/poultry-backend -n poultry-platform -- \
  redis-cli -h redis --latency

# Check for connection exhaustion
kubectl exec -it redis-0 -n poultry-platform -- redis-cli INFO clients
```

**Resolution**:
1. Increase connection timeout in application:
   ```yaml
   spring:
     data:
       redis:
         timeout: 5000ms  # Increase from default
   ```

2. Tune TCP keepalive:
   ```bash
   redis-cli CONFIG SET tcp-keepalive 60
   ```

3. Increase maxclients if needed:
   ```bash
   redis-cli CONFIG SET maxclients 20000
   ```

### Issue: Authentication Failure

**Symptoms**:
- "NOAUTH Authentication required"
- "ERR invalid password"

**Diagnosis**:
```bash
# Check if password is configured
kubectl get secret redis-credentials -n poultry-platform -o jsonpath='{.data.password}' | base64 -d

# Test authentication
kubectl exec -it redis-0 -n poultry-platform -- redis-cli AUTH <password>
```

**Resolution**:
1. Verify password in application configuration
2. Update secret if password changed:
   ```bash
   kubectl create secret generic redis-credentials \
     --from-literal=password=<new-password> \
     --dry-run=client -o yaml | kubectl apply -f -
   ```
3. Restart application to pick up new credentials

---

## Performance Issues

### Issue: High Latency

**Symptoms**:
- Redis operations taking > 10ms
- Application slowdowns

**Diagnosis**:
```bash
# Check slow log
kubectl exec -it redis-0 -n poultry-platform -- redis-cli SLOWLOG GET 25

# Check latency
kubectl exec -it redis-0 -n poultry-platform -- redis-cli --latency

# Check if persistence is causing issues
kubectl exec -it redis-0 -n poultry-platform -- redis-cli INFO persistence

# Check big keys
kubectl exec -it redis-0 -n poultry-platform -- redis-cli --bigkeys
```

**Resolution**:
1. Identify and optimize slow operations from SLOWLOG

2. Disable persistence if not needed:
   ```bash
   redis-cli CONFIG SET save ""
   redis-cli CONFIG SET appendonly no
   ```

3. Enable lazy freeing:
   ```bash
   redis-cli CONFIG SET lazyfree-lazy-eviction yes
   redis-cli CONFIG SET lazyfree-lazy-expire yes
   ```

4. Consider pipelining for batch operations

### Issue: Low Cache Hit Ratio

**Symptoms**:
- Cache hit ratio < 80%
- Increased database load

**Diagnosis**:
```bash
# Check hit ratio
kubectl exec -it redis-0 -n poultry-platform -- redis-cli INFO stats | grep keyspace

# Calculate hit ratio
# hits / (hits + misses) * 100

# Check TTL distribution
kubectl exec -it redis-0 -n poultry-platform -- redis-cli DEBUG OBJECT <key>
```

**Resolution**:
1. Review cache TTL settings - may be too short
2. Ensure cache warming on application startup
3. Check if keys are being evicted prematurely
4. Review caching strategy in application

### Issue: Hot Key Problem

**Symptoms**:
- Single keys accessed very frequently
- Uneven load distribution (in cluster)

**Diagnosis**:
```bash
# Monitor key access patterns
kubectl exec -it redis-0 -n poultry-platform -- redis-cli MONITOR | head -1000

# Check client output buffer
kubectl exec -it redis-0 -n poultry-platform -- redis-cli INFO clients
```

**Resolution**:
1. Implement local caching (L1 cache) for hot keys
2. Use read replicas for hot keys
3. Consider key sharding (key:1, key:2, etc.)

---

## Memory Issues

### Issue: Memory Usage Too High

**Symptoms**:
- Redis using > 80% of allocated memory
- OOM killer risk
- Keys being evicted

**Diagnosis**:
```bash
# Check memory usage
kubectl exec -it redis-0 -n poultry-platform -- redis-cli INFO memory

# Check eviction stats
kubectl exec -it redis-0 -n poultry-platform -- redis-cli INFO stats | grep evicted

# Find big keys
kubectl exec -it redis-0 -n poultry-platform -- redis-cli --bigkeys

# Memory analysis
kubectl exec -it redis-0 -n poultry-platform -- redis-cli MEMORY DOCTOR
```

**Resolution**:
1. **Immediate**: Increase maxmemory:
   ```bash
   redis-cli CONFIG SET maxmemory 2gb
   ```

2. **Clean up**: Delete unnecessary keys:
   ```bash
   # Find and delete expired session keys
   redis-cli KEYS "session:*" | xargs redis-cli DEL
   ```

3. **Optimize data structures**:
   - Use hashes instead of strings for related data
   - Use appropriate encoding (ziplist, intset)

4. **Configure eviction policy**:
   ```bash
   redis-cli CONFIG SET maxmemory-policy allkeys-lru
   ```

### Issue: Memory Fragmentation

**Symptoms**:
- `mem_fragmentation_ratio` > 1.5
- High memory usage despite low key count

**Diagnosis**:
```bash
# Check fragmentation ratio
kubectl exec -it redis-0 -n poultry-platform -- redis-cli INFO memory | grep fragmentation

# mem_fragmentation_ratio > 1.5 indicates fragmentation
```

**Resolution**:
1. Enable active defragmentation:
   ```bash
   redis-cli CONFIG SET activedefrag yes
   redis-cli CONFIG SET active-defrag-ignore-bytes 100mb
   redis-cli CONFIG SET active-defrag-threshold-lower 10
   ```

2. If severe, restart Redis during low-traffic period

### Issue: Out of Memory

**Symptoms**:
- "OOM command not allowed"
- Redis refusing writes

**Diagnosis**:
```bash
# Check memory state
kubectl exec -it redis-0 -n poultry-platform -- redis-cli INFO memory

# Check if maxmemory is set
kubectl exec -it redis-0 -n poultry-platform -- redis-cli CONFIG GET maxmemory
```

**Resolution**:
1. **Emergency**: Flush least important data:
   ```bash
   # Be very careful with FLUSHDB/FLUSHALL
   redis-cli SELECT 1  # Non-critical database
   redis-cli FLUSHDB ASYNC
   ```

2. Delete specific key patterns:
   ```bash
   redis-cli KEYS "temp:*" | xargs redis-cli DEL
   ```

3. Increase maxmemory and pod limits

4. Review and reduce TTLs

---

## Data Issues

### Issue: Stale Cache Data

**Symptoms**:
- Outdated data being served
- Inconsistency between database and cache

**Diagnosis**:
```bash
# Check key TTL
kubectl exec -it redis-0 -n poultry-platform -- redis-cli TTL <key>

# Check key last modified
kubectl exec -it redis-0 -n poultry-platform -- redis-cli OBJECT IDLETIME <key>
```

**Resolution**:
1. **Manual invalidation**:
   ```bash
   # Delete specific key
   redis-cli DEL <key>

   # Delete pattern
   redis-cli KEYS "product:*" | xargs redis-cli DEL
   ```

2. **Application-level fix**:
   - Implement cache-aside pattern correctly
   - Use write-through caching
   - Reduce TTL for frequently updated data

### Issue: Cache Key Patterns

**Common Key Patterns in Poultry Platform**:

| Pattern | Description | TTL |
|---------|-------------|-----|
| `session:<id>` | User sessions | 24h |
| `otp:<phone>` | OTP codes | 5m |
| `product:<id>` | Product cache | 1h |
| `seller:<id>` | Seller profile | 30m |
| `rate_limit:<ip>` | Rate limiting | 1m |

### Clearing Specific Cache Types

```bash
# Clear all sessions
redis-cli KEYS "session:*" | xargs -r redis-cli DEL

# Clear all product cache
redis-cli KEYS "product:*" | xargs -r redis-cli DEL

# Clear rate limiting data
redis-cli KEYS "rate_limit:*" | xargs -r redis-cli DEL
```

---

## Cluster Issues

### For Redis Cluster Deployments

### Issue: Cluster State Unhealthy

**Diagnosis**:
```bash
# Check cluster info
redis-cli CLUSTER INFO

# Check cluster nodes
redis-cli CLUSTER NODES

# Check for failed nodes
redis-cli CLUSTER NODES | grep fail
```

**Resolution**:
1. If node is down, check and restart
2. If slots are unassigned:
   ```bash
   redis-cli CLUSTER ADDSLOTS {slot-number}
   ```
3. Reshard if needed:
   ```bash
   redis-cli --cluster reshard <host>:<port>
   ```

### Issue: Cluster Failover

```bash
# Manual failover (on replica)
redis-cli CLUSTER FAILOVER

# Force failover (if master is down)
redis-cli CLUSTER FAILOVER FORCE
```

---

## Recovery Procedures

### Redis Restart

```bash
# Kubernetes
kubectl rollout restart statefulset/redis -n poultry-platform

# Docker Compose
docker compose restart redis

# Wait and verify
kubectl wait --for=condition=ready pod/redis-0 -n poultry-platform --timeout=60s
```

### Data Recovery from Persistence

```bash
# If using RDB
# 1. Stop Redis
# 2. Replace dump.rdb with backup
# 3. Start Redis

# If using AOF
# 1. Stop Redis
# 2. Replace appendonly.aof with backup
# 3. Run redis-check-aof --fix appendonly.aof
# 4. Start Redis
```

### Emergency Cache Flush

**WARNING: This will clear all cached data**

```bash
# Flush current database only
kubectl exec -it redis-0 -n poultry-platform -- redis-cli FLUSHDB ASYNC

# Flush all databases (use with extreme caution)
kubectl exec -it redis-0 -n poultry-platform -- redis-cli FLUSHALL ASYNC
```

### Cache Warming After Restart

```bash
# The application should warm cache on startup
# Monitor cache hit ratio after restart
watch -n 5 'kubectl exec redis-0 -n poultry-platform -- redis-cli INFO stats | grep keyspace'

# If manual warming needed, trigger cache population endpoints
curl -X POST https://api.poultry-platform.com/api/admin/cache/warm
```

---

## Monitoring Commands

### Real-time Monitoring

```bash
# Monitor all commands (use briefly, very verbose)
kubectl exec -it redis-0 -n poultry-platform -- redis-cli MONITOR

# Check latency
kubectl exec -it redis-0 -n poultry-platform -- redis-cli --latency-history

# Check intrinsic latency
kubectl exec -it redis-0 -n poultry-platform -- redis-cli --intrinsic-latency 5
```

### Statistics Queries

```bash
# Get all stats
kubectl exec -it redis-0 -n poultry-platform -- redis-cli INFO

# Specific sections
redis-cli INFO server
redis-cli INFO clients
redis-cli INFO memory
redis-cli INFO persistence
redis-cli INFO stats
redis-cli INFO replication
redis-cli INFO cpu
redis-cli INFO cluster
redis-cli INFO keyspace
```

---

## Configuration Reference

### Recommended Production Settings

```conf
# Memory
maxmemory 1gb
maxmemory-policy allkeys-lru

# Persistence (if enabled)
save 900 1
save 300 10
save 60 10000

# Networking
timeout 0
tcp-keepalive 300
maxclients 10000

# Performance
lazyfree-lazy-eviction yes
lazyfree-lazy-expire yes
lazyfree-lazy-server-del yes
```

### Application Configuration

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: -1ms
```

---

## Escalation

If unable to resolve:
1. Check Redis documentation
2. Escalate to infrastructure team
3. Consider Redis support (if using managed service)

## Related Runbooks

- [API Issues](./API_ISSUES.md)
- [Database Issues](./DATABASE_ISSUES.md)
- [Performance Issues](./API_ISSUES.md#performance-issues)
