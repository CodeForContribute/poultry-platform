# API Issues Runbook

## Overview

This runbook covers troubleshooting procedures for backend API issues in the Poultry Platform.

**Framework**: Spring Boot 3.x
**Language**: Java 21
**Deployment**: Kubernetes / Docker Compose
**Base Path**: `/api`

---

## Table of Contents

1. [Quick Diagnostics](#quick-diagnostics)
2. [Health Check Issues](#health-check-issues)
3. [Performance Issues](#performance-issues)
4. [Authentication Issues](#authentication-issues)
5. [Error Response Handling](#error-response-handling)
6. [Memory Issues](#memory-issues)
7. [Rate Limiting Issues](#rate-limiting-issues)
8. [External Service Issues](#external-service-issues)

---

## Quick Diagnostics

### Basic Health Checks

```bash
# Check application health
curl -s https://api.poultry-platform.com/api/actuator/health | jq

# Check specific components
curl -s https://api.poultry-platform.com/api/actuator/health/readiness | jq
curl -s https://api.poultry-platform.com/api/actuator/health/liveness | jq

# Check if API is responding
curl -w "@curl-format.txt" -o /dev/null -s https://api.poultry-platform.com/api/actuator/health
```

### Kubernetes Status

```bash
# Check pod status
kubectl get pods -n poultry-platform -l app=poultry-backend

# Check pod logs
kubectl logs -f deployment/poultry-backend -n poultry-platform --tail=100

# Check resource usage
kubectl top pods -n poultry-platform -l app=poultry-backend

# Check pod events
kubectl describe pod -n poultry-platform -l app=poultry-backend | grep -A 20 Events
```

### Key Metrics

```bash
# Get Prometheus metrics
curl -s https://api.poultry-platform.com/api/actuator/prometheus | grep -E "^(http_server_requests|jvm_memory|hikaricp)" | head -50

# Key metrics to check:
# - http_server_requests_seconds_count (request rate)
# - http_server_requests_seconds_sum (total time)
# - jvm_memory_used_bytes (memory usage)
# - hikaricp_connections_active (DB connections)
```

---

## Health Check Issues

### Issue: Liveness Probe Failing

**Symptoms**:
- Pod in CrashLoopBackOff
- Kubernetes restarting pods frequently

**Diagnosis**:
```bash
# Check liveness endpoint directly
kubectl exec -it deployment/poultry-backend -n poultry-platform -- \
  curl -s localhost:8080/api/actuator/health/liveness

# Check pod events
kubectl describe pod <pod-name> -n poultry-platform | grep -A 10 "Liveness"
```

**Resolution**:
1. Check if application is starting correctly:
   ```bash
   kubectl logs <pod-name> -n poultry-platform --previous
   ```

2. Increase initial delay if startup is slow:
   ```yaml
   livenessProbe:
     initialDelaySeconds: 120  # Increase from 60
   ```

3. Check for deadlocks or infinite loops in application

### Issue: Readiness Probe Failing

**Symptoms**:
- Pod is Running but not Ready
- Traffic not being routed to pod

**Diagnosis**:
```bash
# Check readiness endpoint
kubectl exec -it deployment/poultry-backend -n poultry-platform -- \
  curl -s localhost:8080/api/actuator/health/readiness | jq

# Check which component is unhealthy
curl -s https://api.poultry-platform.com/api/actuator/health | jq '.components | to_entries[] | select(.value.status != "UP")'
```

**Resolution**:
1. If database is unhealthy: See [Database Issues](./DATABASE_ISSUES.md)
2. If Redis is unhealthy: See [Cache Issues](./CACHE_ISSUES.md)
3. If Kafka is unhealthy: See [Kafka Issues](./KAFKA_ISSUES.md)

---

## Performance Issues

### Issue: High Response Latency

**Symptoms**:
- p99 latency > 2 seconds
- Users experiencing slow responses
- Timeouts in client applications

**Diagnosis**:
```bash
# Check response time metrics
curl -s https://api.poultry-platform.com/api/actuator/prometheus | \
  grep "http_server_requests_seconds" | grep -v "#"

# Check for slow endpoints
curl -s https://api.poultry-platform.com/api/actuator/prometheus | \
  grep "http_server_requests_seconds_max" | sort -t'=' -k2 -n -r | head -10

# Check thread pool status
curl -s https://api.poultry-platform.com/api/actuator/prometheus | \
  grep "tomcat_threads"
```

**Resolution**:
1. **Identify slow endpoints** and optimize:
   - Check database queries
   - Add caching
   - Optimize external API calls

2. **Scale horizontally**:
   ```bash
   kubectl scale deployment poultry-backend -n poultry-platform --replicas=5
   ```

3. **Check database connection pool**:
   ```bash
   curl -s https://api.poultry-platform.com/api/actuator/prometheus | grep hikaricp
   ```

4. **Enable async processing** for heavy operations

### Issue: High Error Rate

**Symptoms**:
- 5xx errors increasing
- Alert for error rate > 5%

**Diagnosis**:
```bash
# Check error rates by endpoint
curl -s https://api.poultry-platform.com/api/actuator/prometheus | \
  grep 'http_server_requests_seconds_count.*status="5'

# Check recent error logs
kubectl logs deployment/poultry-backend -n poultry-platform --tail=500 | \
  grep -E "(ERROR|Exception|WARN)" | tail -50

# Check error breakdown
kubectl logs deployment/poultry-backend -n poultry-platform --tail=1000 | \
  grep "ERROR" | awk '{print $NF}' | sort | uniq -c | sort -rn
```

**Resolution**:
1. Identify root cause from logs
2. Check external service status
3. Check for recent deployments
4. Consider rollback if deployment-related

### Issue: Request Timeouts

**Symptoms**:
- 504 Gateway Timeout errors
- Connection timeouts in client

**Diagnosis**:
```bash
# Check for stuck requests
kubectl exec -it deployment/poultry-backend -n poultry-platform -- \
  curl -s localhost:8080/api/actuator/threaddump | grep -A 20 "BLOCKED\|WAITING"

# Check database for long-running queries
# See DATABASE_ISSUES.md
```

**Resolution**:
1. Increase timeout if legitimate:
   ```yaml
   server:
     tomcat:
       connection-timeout: 30000
   ```

2. Add circuit breaker for external calls

3. Implement async processing for long operations

---

## Authentication Issues

### Issue: JWT Token Validation Failures

**Symptoms**:
- 401 Unauthorized responses
- "Invalid token" errors

**Diagnosis**:
```bash
# Check JWT configuration
kubectl get configmap poultry-config -n poultry-platform -o yaml | grep -A 5 jwt

# Check for clock skew
kubectl exec -it deployment/poultry-backend -n poultry-platform -- date
date

# Test token validation
curl -v -H "Authorization: Bearer <token>" \
  https://api.poultry-platform.com/api/sellers/me
```

**Resolution**:
1. Verify JWT secret is consistent across all pods
2. Check token expiration settings
3. Sync system clocks if skew detected
4. Clear any cached invalid tokens

### Issue: OTP Not Being Sent

**Symptoms**:
- Users not receiving OTP
- Login/registration failing

**Diagnosis**:
```bash
# Check SMS service logs
kubectl logs deployment/poultry-backend -n poultry-platform | grep -i "sms\|otp\|msg91\|twilio"

# Check circuit breaker status
curl -s https://api.poultry-platform.com/api/actuator/health | jq '.components.circuitBreakers'

# Check rate limiting
kubectl logs deployment/poultry-backend -n poultry-platform | grep "rate.limit"
```

**Resolution**:
1. Verify SMS provider credentials
2. Check SMS provider dashboard for delivery status
3. Reset circuit breaker if open:
   ```bash
   # Restart affected pods
   kubectl rollout restart deployment/poultry-backend -n poultry-platform
   ```
4. Check OTP rate limiting hasn't been exceeded

---

## Error Response Handling

### Common HTTP Status Codes

| Status | Meaning | Typical Cause |
|--------|---------|---------------|
| 400 | Bad Request | Invalid input, validation failure |
| 401 | Unauthorized | Invalid/missing JWT token |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate resource, state conflict |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Application error |
| 502 | Bad Gateway | Upstream service error |
| 503 | Service Unavailable | Service overloaded or maintenance |
| 504 | Gateway Timeout | Upstream service timeout |

### Analyzing Error Logs

```bash
# Get error distribution
kubectl logs deployment/poultry-backend -n poultry-platform --tail=10000 | \
  grep "ERROR" | \
  sed -E 's/.*\[([^]]+)\].*/\1/' | \
  sort | uniq -c | sort -rn | head -20

# Get stack traces for specific error
kubectl logs deployment/poultry-backend -n poultry-platform --tail=10000 | \
  grep -A 30 "NullPointerException"

# Get errors with correlation ID
kubectl logs deployment/poultry-backend -n poultry-platform | \
  grep -E "correlationId.*ERROR"
```

---

## Memory Issues

### Issue: OutOfMemoryError

**Symptoms**:
- Pod restarts with OOMKilled
- Application becomes unresponsive
- Heap space errors in logs

**Diagnosis**:
```bash
# Check memory usage
kubectl top pods -n poultry-platform -l app=poultry-backend

# Check pod events for OOMKilled
kubectl describe pod <pod-name> -n poultry-platform | grep -i oom

# Check JVM memory metrics
curl -s https://api.poultry-platform.com/api/actuator/prometheus | \
  grep "jvm_memory_used_bytes"

# Get heap dump (if pod is still running)
kubectl exec -it <pod-name> -n poultry-platform -- \
  jmap -dump:format=b,file=/tmp/heap.hprof 1
```

**Resolution**:
1. **Increase memory limits** (temporary):
   ```yaml
   resources:
     limits:
       memory: "2Gi"  # Increase from 1Gi
     requests:
       memory: "1Gi"
   ```

2. **Analyze heap dump** for memory leaks

3. **Common memory leak sources**:
   - Unbounded caches
   - Connection pool leaks
   - Large object accumulation

4. **Tune JVM settings**:
   ```bash
   JAVA_OPTS="-Xms512m -Xmx1536m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
   ```

### Issue: High GC Pauses

**Symptoms**:
- Periodic latency spikes
- High CPU during GC

**Diagnosis**:
```bash
# Check GC metrics
curl -s https://api.poultry-platform.com/api/actuator/prometheus | \
  grep "jvm_gc"

# Enable GC logging
JAVA_OPTS="$JAVA_OPTS -Xlog:gc*:file=/var/log/gc.log:time,uptime:filecount=5,filesize=10M"
```

**Resolution**:
1. Tune G1GC parameters
2. Increase heap size
3. Review object allocation patterns

---

## Rate Limiting Issues

### Issue: Legitimate Requests Being Rate Limited

**Symptoms**:
- 429 Too Many Requests responses
- Users complaining about access

**Diagnosis**:
```bash
# Check rate limit configuration
kubectl get configmap poultry-config -n poultry-platform -o yaml | grep -A 10 "rate-limit"

# Check rate limit metrics
curl -s https://api.poultry-platform.com/api/actuator/prometheus | \
  grep "rate_limit"

# Check application logs
kubectl logs deployment/poultry-backend -n poultry-platform | grep -i "rate.limit"
```

**Resolution**:
1. **Whitelist IP** if legitimate:
   ```yaml
   rate-limit:
     whitelisted-ips:
       - "10.0.0.0/8"
       - "<legitimate-ip>"
   ```

2. **Increase rate limits** if appropriate:
   ```yaml
   rate-limit:
     default-requests-per-minute: 200  # Increase from 100
   ```

3. **Add burst capacity**:
   ```yaml
   rate-limit:
     burst-multiplier: 2.0  # Allow burst up to 2x limit
   ```

---

## External Service Issues

### Issue: Razorpay API Failures

**Symptoms**:
- Payment processing failures
- Circuit breaker open

**Diagnosis**:
```bash
# Check circuit breaker status
curl -s https://api.poultry-platform.com/api/actuator/health | \
  jq '.components.circuitBreakers.details.razorpay'

# Check Razorpay-related errors
kubectl logs deployment/poultry-backend -n poultry-platform | grep -i razorpay

# Check Razorpay status page
curl -s https://status.razorpay.com/api/v2/status.json
```

**Resolution**:
1. Check [Razorpay Status Page](https://status.razorpay.com)
2. Verify API credentials
3. Reset circuit breaker by restarting pods
4. Enable fallback payment processing if available

### Issue: SMS Provider Failures

**Symptoms**:
- OTP not being delivered
- MSG91/Twilio circuit breaker open

**Diagnosis**:
```bash
# Check circuit breaker
curl -s https://api.poultry-platform.com/api/actuator/health | \
  jq '.components.circuitBreakers.details.msg91'

# Check provider logs
kubectl logs deployment/poultry-backend -n poultry-platform | grep -iE "msg91|twilio|sms"
```

**Resolution**:
1. Check provider dashboard for delivery status
2. Verify API credentials
3. Switch to backup SMS provider if configured
4. Implement SMS fallback mechanism

---

## Diagnostic Commands Reference

### Log Analysis

```bash
# Tail logs with filtering
kubectl logs -f deployment/poultry-backend -n poultry-platform | grep -v "INFO"

# Get logs for specific time range
kubectl logs deployment/poultry-backend -n poultry-platform --since=1h

# Get logs with correlation ID
kubectl logs deployment/poultry-backend -n poultry-platform | \
  grep "correlationId=<id>"
```

### Performance Profiling

```bash
# Thread dump
kubectl exec -it <pod-name> -n poultry-platform -- \
  jstack 1

# CPU profiling (async-profiler)
kubectl exec -it <pod-name> -n poultry-platform -- \
  ./profiler.sh -d 30 -f /tmp/profile.html 1

# Flight recorder
kubectl exec -it <pod-name> -n poultry-platform -- \
  jcmd 1 JFR.start duration=60s filename=/tmp/recording.jfr
```

### Network Diagnostics

```bash
# Test connectivity to external services
kubectl exec -it deployment/poultry-backend -n poultry-platform -- \
  nc -zv api.razorpay.com 443

# Check DNS resolution
kubectl exec -it deployment/poultry-backend -n poultry-platform -- \
  nslookup postgres
```

---

## Escalation

If unable to resolve:
1. Check related runbooks (Database, Cache, Kafka)
2. Escalate to Tech Lead
3. Consider rollback if deployment-related
4. Engage vendor support if external service issue

## Related Runbooks

- [Database Issues](./DATABASE_ISSUES.md)
- [Cache Issues](./CACHE_ISSUES.md)
- [Kafka Issues](./KAFKA_ISSUES.md)
- [Payment Issues](./PAYMENT_ISSUES.md)
- [Deployment Rollback](./DEPLOYMENT_ROLLBACK.md)
