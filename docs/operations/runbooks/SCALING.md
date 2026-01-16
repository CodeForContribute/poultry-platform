# Scaling Runbook

## Overview

This runbook covers procedures for scaling the Poultry Platform components to handle increased load or optimize resource usage.

**Orchestration**: Kubernetes / Docker Compose
**Auto-scaling**: Horizontal Pod Autoscaler (HPA)
**Load Balancer**: Kubernetes Service / Nginx

---

## Table of Contents

1. [Scaling Indicators](#scaling-indicators)
2. [Manual Scaling](#manual-scaling)
3. [Auto-scaling Configuration](#auto-scaling-configuration)
4. [Component-Specific Scaling](#component-specific-scaling)
5. [Scaling for Events](#scaling-for-events)
6. [Cost Optimization](#cost-optimization)
7. [Troubleshooting Scaling Issues](#troubleshooting-scaling-issues)

---

## Scaling Indicators

### When to Scale Up

| Metric | Threshold | Action |
|--------|-----------|--------|
| CPU Usage | > 70% sustained for 5 min | Scale up |
| Memory Usage | > 80% | Scale up or increase limits |
| Response Time (p99) | > 2 seconds | Scale up |
| Request Queue Depth | > 100 | Scale up |
| Error Rate | > 2% | Investigate, possibly scale |
| Database Connections | > 80% of pool | Scale app or increase pool |

### When to Scale Down

| Metric | Threshold | Action |
|--------|-----------|--------|
| CPU Usage | < 30% sustained for 15 min | Consider scale down |
| Memory Usage | < 40% | Consider scale down |
| Request Rate | 50% below normal | Scale down |

### Monitoring Commands

```bash
# Check current resource usage
kubectl top pods -n poultry-platform

# Check HPA status
kubectl get hpa -n poultry-platform

# Check node capacity
kubectl top nodes

# Check metrics
curl -s https://api.poultry-platform.com/api/actuator/prometheus | \
  grep -E "^(http_server_requests|jvm_memory|hikaricp)" | head -30
```

---

## Manual Scaling

### Kubernetes: Scale Deployment

```bash
# Scale up backend
kubectl scale deployment poultry-backend -n poultry-platform --replicas=5

# Scale down backend
kubectl scale deployment poultry-backend -n poultry-platform --replicas=2

# Verify scaling
kubectl get pods -n poultry-platform -l app=poultry-backend -w

# Check events
kubectl get events -n poultry-platform --sort-by='.lastTimestamp' | tail -10
```

### Docker Compose: Scale Service

```bash
# Scale up
docker compose up -d --scale backend=3

# Scale down
docker compose up -d --scale backend=1

# Verify
docker compose ps
```

### Scaling Verification

```bash
# Wait for all pods to be ready
kubectl wait --for=condition=ready pod -l app=poultry-backend -n poultry-platform --timeout=300s

# Check load distribution
kubectl get endpoints poultry-backend -n poultry-platform

# Verify traffic is being distributed
for i in {1..10}; do
  curl -s https://api.poultry-platform.com/api/actuator/health | jq -r '.components.diskSpace.details.free' &
done
wait
```

---

## Auto-scaling Configuration

### Current HPA Configuration

```yaml
# infra/k8s/backend-hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: poultry-backend-hpa
  namespace: poultry-platform
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: poultry-backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 15
      - type: Pods
        value: 4
        periodSeconds: 15
      selectPolicy: Max
```

### Modify HPA Settings

```bash
# Edit HPA
kubectl edit hpa poultry-backend-hpa -n poultry-platform

# Or patch specific values
kubectl patch hpa poultry-backend-hpa -n poultry-platform \
  --patch '{"spec":{"maxReplicas":15}}'

# Increase minimum replicas for high-traffic period
kubectl patch hpa poultry-backend-hpa -n poultry-platform \
  --patch '{"spec":{"minReplicas":5}}'
```

### Disable Auto-scaling Temporarily

```bash
# Remove HPA (manual scaling only)
kubectl delete hpa poultry-backend-hpa -n poultry-platform

# Re-apply HPA
kubectl apply -f infra/k8s/backend-hpa.yaml
```

---

## Component-Specific Scaling

### Backend API Scaling

```bash
# Current capacity
kubectl get deployment poultry-backend -n poultry-platform

# Scale based on expected load
# Rule of thumb: 1 pod per 500 concurrent users

# Scale for 2500 concurrent users
kubectl scale deployment poultry-backend -n poultry-platform --replicas=5
```

**Resource Guidelines per Pod**:
| Load Level | CPU | Memory | Replicas |
|------------|-----|--------|----------|
| Low | 250m | 512Mi | 2 |
| Medium | 500m | 1Gi | 3-5 |
| High | 1000m | 2Gi | 5-10 |

### Database Scaling

**PostgreSQL cannot be horizontally scaled in the same way**. Options:

1. **Vertical Scaling** (increase resources):
   ```bash
   # Update StatefulSet resources
   kubectl patch statefulset postgres -n poultry-platform \
     --patch '{"spec":{"template":{"spec":{"containers":[{"name":"postgres","resources":{"requests":{"memory":"4Gi","cpu":"2"},"limits":{"memory":"8Gi","cpu":"4"}}}]}}}}'
   ```

2. **Connection Pool Tuning**:
   ```yaml
   # Increase max connections in PostgreSQL
   max_connections: 200

   # Adjust HikariCP per pod
   hikari:
     maximum-pool-size: 10  # Reduce per pod as pods increase
   ```

3. **Read Replicas** (for read-heavy workloads):
   - Set up PostgreSQL streaming replication
   - Route read queries to replicas

### Redis Scaling

```bash
# Vertical scaling
kubectl patch statefulset redis -n poultry-platform \
  --patch '{"spec":{"template":{"spec":{"containers":[{"name":"redis","resources":{"limits":{"memory":"2Gi"}}}]}}}}'

# For high availability, consider Redis Cluster
```

### Kafka Scaling

```bash
# Add Kafka brokers
kubectl scale statefulset kafka -n poultry-platform --replicas=3

# Increase partitions for topics
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-topics --bootstrap-server localhost:9092 \
  --alter --topic order-events --partitions 6
```

---

## Scaling for Events

### Pre-scaling for Expected Traffic

```bash
#!/bin/bash
# pre-scale.sh - Run before expected high-traffic events

echo "Pre-scaling for high traffic event..."

# Scale backend
kubectl scale deployment poultry-backend -n poultry-platform --replicas=8

# Increase HPA minimums
kubectl patch hpa poultry-backend-hpa -n poultry-platform \
  --patch '{"spec":{"minReplicas":6,"maxReplicas":15}}'

# Verify scaling
kubectl get pods -n poultry-platform -l app=poultry-backend

echo "Pre-scaling complete. Monitor: kubectl get hpa -n poultry-platform -w"
```

### Post-event Scale Down

```bash
#!/bin/bash
# post-scale.sh - Run after high-traffic event

echo "Scaling down after event..."

# Restore normal HPA settings
kubectl patch hpa poultry-backend-hpa -n poultry-platform \
  --patch '{"spec":{"minReplicas":2,"maxReplicas":10}}'

# Let HPA handle scaling down naturally
echo "HPA will gradually scale down based on metrics"

# Optional: Force immediate scale down
# kubectl scale deployment poultry-backend -n poultry-platform --replicas=2
```

### Capacity Planning

```
Expected Users | Concurrent | Pods | Database Connections
-------------- | ---------- | ---- | --------------------
1,000          | 100        | 2    | 20
5,000          | 500        | 3    | 30
10,000         | 1,000      | 5    | 50
50,000         | 5,000      | 10   | 100
```

---

## Cost Optimization

### Right-sizing Resources

```bash
# Analyze resource usage over time
kubectl top pods -n poultry-platform --sort-by=cpu
kubectl top pods -n poultry-platform --sort-by=memory

# Check for over-provisioned pods
kubectl describe pod <pod-name> -n poultry-platform | grep -A 5 "Requests:\|Limits:"
```

### Recommended Resource Settings

```yaml
# Production settings
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "1000m"

# Development settings
resources:
  requests:
    memory: "256Mi"
    cpu: "100m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

### Spot/Preemptible Instances

```yaml
# Use spot instances for non-critical workloads
nodeSelector:
  cloud.google.com/gke-spot: "true"
# Or for AWS
nodeSelector:
  eks.amazonaws.com/capacityType: SPOT

# Add tolerations
tolerations:
- key: "cloud.google.com/gke-spot"
  operator: "Equal"
  value: "true"
  effect: "NoSchedule"
```

### Scale to Zero (Non-production)

```bash
# Scale to zero during off-hours (development/staging)
kubectl scale deployment poultry-backend -n poultry-staging --replicas=0

# Schedule-based scaling with CronJob
apiVersion: batch/v1
kind: CronJob
metadata:
  name: scale-down-night
spec:
  schedule: "0 22 * * *"  # 10 PM
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: kubectl
            image: bitnami/kubectl
            command:
            - /bin/sh
            - -c
            - kubectl scale deployment poultry-backend -n poultry-staging --replicas=0
```

---

## Troubleshooting Scaling Issues

### Issue: Pods Not Scaling Up

**Symptoms**:
- HPA shows desired replicas but actual doesn't increase
- Pods stuck in Pending state

**Diagnosis**:
```bash
# Check HPA status
kubectl describe hpa poultry-backend-hpa -n poultry-platform

# Check for pending pods
kubectl get pods -n poultry-platform | grep Pending

# Check pod events
kubectl describe pod <pending-pod> -n poultry-platform | grep -A 20 Events

# Check node capacity
kubectl describe nodes | grep -A 10 "Allocated resources"
```

**Resolution**:
1. **Insufficient node capacity**: Add nodes to cluster
2. **Resource quotas exceeded**: Increase namespace quotas
3. **PVC binding issues**: Check storage class and provisioner
4. **Image pull issues**: Check registry credentials

### Issue: Pods Not Scaling Down

**Symptoms**:
- HPA shows lower desired replicas but pods remain
- Higher than expected costs

**Diagnosis**:
```bash
# Check HPA stabilization window
kubectl describe hpa poultry-backend-hpa -n poultry-platform | grep -A 10 Behavior

# Check scale-down policies
kubectl get hpa poultry-backend-hpa -n poultry-platform -o yaml | grep -A 20 scaleDown
```

**Resolution**:
1. Reduce stabilization window:
   ```bash
   kubectl patch hpa poultry-backend-hpa -n poultry-platform \
     --patch '{"spec":{"behavior":{"scaleDown":{"stabilizationWindowSeconds":60}}}}'
   ```
2. Check for PodDisruptionBudget blocking scale-down
3. Manually scale down if needed

### Issue: Uneven Load Distribution

**Symptoms**:
- Some pods overloaded while others idle
- Requests not distributed evenly

**Diagnosis**:
```bash
# Check endpoint weights
kubectl describe endpoints poultry-backend -n poultry-platform

# Check pod readiness
kubectl get pods -n poultry-platform -l app=poultry-backend -o wide
```

**Resolution**:
1. Verify all pods are Ready
2. Check session affinity settings
3. Verify load balancer configuration
4. Check for slow-starting pods

### Issue: Memory-based Scaling Not Working

**Symptoms**:
- Memory usage high but no scaling
- HPA shows unknown for memory metrics

**Diagnosis**:
```bash
# Check if metrics server is running
kubectl get deployment metrics-server -n kube-system

# Check if memory metrics are available
kubectl top pods -n poultry-platform

# Check HPA events
kubectl describe hpa poultry-backend-hpa -n poultry-platform | grep -i memory
```

**Resolution**:
1. Install/fix metrics server
2. Ensure resource requests are defined
3. Wait for metrics to stabilize (2-3 minutes after pod start)

---

## Scaling Automation Scripts

### Emergency Scale Up

```bash
#!/bin/bash
# emergency-scale-up.sh

NAMESPACE="poultry-platform"
DEPLOYMENT="poultry-backend"
TARGET_REPLICAS=${1:-10}

echo "Emergency scaling to $TARGET_REPLICAS replicas..."

# Temporarily disable HPA
kubectl patch hpa ${DEPLOYMENT}-hpa -n $NAMESPACE \
  --patch "{\"spec\":{\"minReplicas\":$TARGET_REPLICAS,\"maxReplicas\":$TARGET_REPLICAS}}"

# Scale deployment
kubectl scale deployment $DEPLOYMENT -n $NAMESPACE --replicas=$TARGET_REPLICAS

# Wait for pods
kubectl wait --for=condition=ready pod -l app=$DEPLOYMENT -n $NAMESPACE --timeout=300s

echo "Scaling complete. Current pods:"
kubectl get pods -n $NAMESPACE -l app=$DEPLOYMENT
```

### Load-based Scaling Check

```bash
#!/bin/bash
# scaling-check.sh

echo "=== Scaling Status Check ==="

# HPA status
echo -e "\n--- HPA Status ---"
kubectl get hpa -n poultry-platform

# Current pods
echo -e "\n--- Pod Status ---"
kubectl get pods -n poultry-platform -l app=poultry-backend

# Resource usage
echo -e "\n--- Resource Usage ---"
kubectl top pods -n poultry-platform -l app=poultry-backend

# Node capacity
echo -e "\n--- Node Capacity ---"
kubectl describe nodes | grep -A 5 "Allocated resources"
```

---

## Escalation

If scaling issues persist:
1. Check cloud provider quotas
2. Contact infrastructure team
3. Consider vertical scaling as alternative
4. Engage cloud provider support for capacity issues

## Related Runbooks

- [API Issues](./API_ISSUES.md)
- [Database Issues](./DATABASE_ISSUES.md)
- [Kafka Issues](./KAFKA_ISSUES.md)
- [Deployment Guide](../DEPLOYMENT_GUIDE.md)
