# Kafka Issues Runbook

## Overview

This runbook covers troubleshooting procedures for Apache Kafka message queue issues in the Poultry Platform.

**Kafka Version**: Confluent Platform 7.5
**Deployment**: Kubernetes / Docker Compose
**Client**: Spring Kafka

---

## Table of Contents

1. [Quick Diagnostics](#quick-diagnostics)
2. [Broker Issues](#broker-issues)
3. [Producer Issues](#producer-issues)
4. [Consumer Issues](#consumer-issues)
5. [Topic Issues](#topic-issues)
6. [Zookeeper Issues](#zookeeper-issues)
7. [Performance Issues](#performance-issues)
8. [Recovery Procedures](#recovery-procedures)

---

## Quick Diagnostics

### Health Check Commands

```bash
# Kubernetes: Check Kafka pods
kubectl get pods -n poultry-platform -l app=kafka

# Check Kafka logs
kubectl logs -f kafka-0 -n poultry-platform --tail=100

# Check Zookeeper pods
kubectl get pods -n poultry-platform -l app=zookeeper

# Application health
curl -s https://api.poultry-platform.com/api/actuator/health | jq '.components.kafka'
```

### Kafka CLI Commands

```bash
# Connect to Kafka pod
kubectl exec -it kafka-0 -n poultry-platform -- bash

# List topics
kafka-topics --bootstrap-server localhost:9092 --list

# Describe cluster
kafka-metadata --bootstrap-server localhost:9092 --describe --status

# Check broker IDs
kafka-broker-api-versions --bootstrap-server localhost:9092

# Check consumer groups
kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

---

## Broker Issues

### Issue: Broker Not Starting

**Symptoms**:
- Kafka pod in CrashLoopBackOff
- "Broker not available" errors in application

**Diagnosis**:
```bash
# Check pod events
kubectl describe pod kafka-0 -n poultry-platform

# Check logs for startup errors
kubectl logs kafka-0 -n poultry-platform --previous

# Check Zookeeper connectivity
kubectl exec -it kafka-0 -n poultry-platform -- \
  zookeeper-shell zookeeper:2181 ls /brokers/ids
```

**Resolution**:
1. **Zookeeper connectivity issues**:
   ```bash
   # Verify Zookeeper is running
   kubectl get pods -n poultry-platform -l app=zookeeper

   # Test connectivity
   kubectl exec -it kafka-0 -n poultry-platform -- \
     nc -zv zookeeper 2181
   ```

2. **Clean restart**:
   ```bash
   kubectl delete pod kafka-0 -n poultry-platform
   ```

3. **Check disk space**:
   ```bash
   kubectl exec -it kafka-0 -n poultry-platform -- df -h /var/lib/kafka
   ```

### Issue: Broker Out of Sync

**Symptoms**:
- ISR (In-Sync Replicas) shrinking
- Partition leadership changes

**Diagnosis**:
```bash
# Check under-replicated partitions
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-topics --bootstrap-server localhost:9092 \
  --describe --under-replicated-partitions

# Check ISR
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-topics --bootstrap-server localhost:9092 \
  --describe --topic <topic-name>
```

**Resolution**:
1. Check network between brokers
2. Check broker disk I/O
3. Increase `replica.lag.time.max.ms` if legitimate lag
4. Restart lagging broker

### Issue: Broker Disk Full

**Symptoms**:
- "No space left on device" errors
- New messages not being accepted

**Diagnosis**:
```bash
# Check disk usage
kubectl exec -it kafka-0 -n poultry-platform -- df -h /var/lib/kafka

# Check log segment sizes
kubectl exec -it kafka-0 -n poultry-platform -- \
  du -sh /var/lib/kafka/data/*
```

**Resolution**:
1. **Emergency cleanup**:
   ```bash
   # Reduce retention temporarily
   kafka-configs --bootstrap-server localhost:9092 \
     --alter --entity-type topics --entity-name <topic> \
     --add-config retention.ms=3600000  # 1 hour

   # Force log cleanup
   kafka-configs --bootstrap-server localhost:9092 \
     --alter --entity-type topics --entity-name <topic> \
     --add-config cleanup.policy=delete
   ```

2. **Expand volume** (if using dynamic provisioning)

3. **Delete old topics** if no longer needed

---

## Producer Issues

### Issue: Producer Not Sending Messages

**Symptoms**:
- Messages not appearing in topics
- Producer timeout errors

**Diagnosis**:
```bash
# Check application logs for producer errors
kubectl logs deployment/poultry-backend -n poultry-platform | grep -i "producer\|kafka" | tail -30

# Test producer from command line
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-console-producer --bootstrap-server localhost:9092 --topic test-topic

# Check topic exists
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-topics --bootstrap-server localhost:9092 --describe --topic <topic-name>
```

**Resolution**:
1. Verify topic exists and create if needed:
   ```bash
   kafka-topics --bootstrap-server localhost:9092 \
     --create --topic <topic-name> \
     --partitions 3 --replication-factor 1
   ```

2. Check producer configuration:
   ```yaml
   spring:
     kafka:
       producer:
         bootstrap-servers: kafka:9092
         key-serializer: org.apache.kafka.common.serialization.StringSerializer
         value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
         acks: all
         retries: 3
   ```

3. Check for serialization errors in logs

### Issue: Producer Timeout

**Symptoms**:
- "TimeoutException" in producer
- Messages taking too long to acknowledge

**Diagnosis**:
```bash
# Check broker latency
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-producer-perf-test --topic test-topic \
  --throughput 100 --num-records 1000 \
  --record-size 100 --producer-props bootstrap.servers=localhost:9092

# Check for slow leader election
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-topics --bootstrap-server localhost:9092 --describe --topic <topic>
```

**Resolution**:
1. Increase producer timeout:
   ```yaml
   spring:
     kafka:
       producer:
         properties:
           request.timeout.ms: 30000
           delivery.timeout.ms: 120000
   ```

2. Reduce batch size for faster sends:
   ```yaml
   spring:
     kafka:
       producer:
         batch-size: 16384
         linger-ms: 5
   ```

3. Check network connectivity to brokers

---

## Consumer Issues

### Issue: Consumer Lag Building Up

**Symptoms**:
- Messages not being processed
- Consumer lag increasing
- Delayed order processing

**Diagnosis**:
```bash
# Check consumer lag
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group poultry-platform

# Check specific topic lag
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group poultry-platform | grep <topic-name>

# Check consumer state
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group poultry-platform --state
```

**Resolution**:
1. **Scale consumers**:
   ```bash
   # Increase consumer instances (up to partition count)
   kubectl scale deployment poultry-backend -n poultry-platform --replicas=5
   ```

2. **Check for processing errors**:
   ```bash
   kubectl logs deployment/poultry-backend -n poultry-platform | \
     grep -i "consumer\|exception" | tail -50
   ```

3. **Increase consumer throughput**:
   ```yaml
   spring:
     kafka:
       consumer:
         properties:
           max.poll.records: 500
           fetch.min.bytes: 1
           fetch.max.wait.ms: 500
   ```

4. **Reset offset if necessary** (data loss warning):
   ```bash
   kafka-consumer-groups --bootstrap-server localhost:9092 \
     --group poultry-platform --topic <topic> \
     --reset-offsets --to-latest --execute
   ```

### Issue: Consumer Rebalancing Frequently

**Symptoms**:
- "Rebalancing" messages in logs
- Intermittent processing delays
- Consumer group instability

**Diagnosis**:
```bash
# Check consumer group stability
kubectl logs deployment/poultry-backend -n poultry-platform | \
  grep -i "rebalanc" | tail -20

# Check consumer heartbeats
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group poultry-platform --members
```

**Resolution**:
1. **Increase session timeout**:
   ```yaml
   spring:
     kafka:
       consumer:
         properties:
           session.timeout.ms: 30000
           heartbeat.interval.ms: 10000
   ```

2. **Reduce max.poll.records** if processing takes too long:
   ```yaml
   spring:
     kafka:
       consumer:
         properties:
           max.poll.records: 100
           max.poll.interval.ms: 300000
   ```

3. **Use static membership** to reduce rebalances:
   ```yaml
   spring:
     kafka:
       consumer:
         properties:
           group.instance.id: ${HOSTNAME}
   ```

### Issue: Consumer Not Receiving Messages

**Symptoms**:
- Consumer running but no messages processed
- Topic has messages but consumer shows no activity

**Diagnosis**:
```bash
# Check consumer group status
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group poultry-platform

# Check topic partitions and offsets
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 --topic <topic-name>

# Consume messages manually to verify
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic <topic-name> --from-beginning --max-messages 5
```

**Resolution**:
1. Verify consumer group ID matches configuration
2. Check auto.offset.reset setting:
   ```yaml
   spring:
     kafka:
       consumer:
         auto-offset-reset: earliest  # or 'latest'
   ```
3. Ensure topic subscription is correct
4. Check for deserialization errors

---

## Topic Issues

### Issue: Topic Not Found

**Diagnosis**:
```bash
# List all topics
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-topics --bootstrap-server localhost:9092 --list

# Create topic if missing
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-topics --bootstrap-server localhost:9092 \
  --create --topic <topic-name> \
  --partitions 6 --replication-factor 1
```

### Issue: Topic Partitions

**Standard Topics in Poultry Platform**:

| Topic | Partitions | Description |
|-------|------------|-------------|
| order-events | 6 | Order lifecycle events |
| payment-events | 6 | Payment status updates |
| notification-events | 3 | SMS/Push notifications |
| inventory-events | 3 | Inventory updates |

**Increase partitions**:
```bash
kafka-topics --bootstrap-server localhost:9092 \
  --alter --topic order-events --partitions 12
```

**Note**: Partitions can only be increased, not decreased.

### Issue: Topic Retention

**Check and modify retention**:
```bash
# Check current retention
kafka-configs --bootstrap-server localhost:9092 \
  --describe --entity-type topics --entity-name <topic>

# Set retention to 7 days
kafka-configs --bootstrap-server localhost:9092 \
  --alter --entity-type topics --entity-name <topic> \
  --add-config retention.ms=604800000
```

---

## Zookeeper Issues

### Issue: Zookeeper Not Available

**Symptoms**:
- Kafka brokers not starting
- "Zookeeper connection failed" errors

**Diagnosis**:
```bash
# Check Zookeeper pods
kubectl get pods -n poultry-platform -l app=zookeeper

# Check Zookeeper logs
kubectl logs zookeeper-0 -n poultry-platform --tail=50

# Test Zookeeper from Kafka pod
kubectl exec -it kafka-0 -n poultry-platform -- \
  zookeeper-shell zookeeper:2181 ls /
```

**Resolution**:
1. Restart Zookeeper:
   ```bash
   kubectl rollout restart statefulset/zookeeper -n poultry-platform
   ```

2. Check Zookeeper data directory:
   ```bash
   kubectl exec -it zookeeper-0 -n poultry-platform -- \
     ls -la /var/lib/zookeeper
   ```

3. Verify Zookeeper service:
   ```bash
   kubectl get svc zookeeper -n poultry-platform
   ```

### Issue: Zookeeper Session Expired

**Symptoms**:
- "Session expired" in Kafka logs
- Broker disconnecting from cluster

**Resolution**:
1. Increase session timeout:
   ```properties
   zookeeper.session.timeout.ms=30000
   ```

2. Check network stability between Kafka and Zookeeper

3. Restart affected Kafka broker

---

## Performance Issues

### Issue: High Latency

**Diagnosis**:
```bash
# Producer latency test
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-producer-perf-test --topic test-topic \
  --throughput 1000 --num-records 10000 \
  --record-size 1024 --producer-props bootstrap.servers=localhost:9092

# Consumer latency test
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-consumer-perf-test --bootstrap-server localhost:9092 \
  --topic test-topic --messages 10000
```

**Resolution**:
1. Check broker disk I/O
2. Increase `num.io.threads` and `num.network.threads`
3. Tune producer batching:
   ```yaml
   batch.size: 32768
   linger.ms: 10
   ```

### Issue: Message Loss

**Diagnosis**:
```bash
# Check for unclean leader election
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-configs --bootstrap-server localhost:9092 \
  --describe --entity-type brokers --entity-default

# Verify producer acks setting
```

**Prevention**:
1. Use `acks=all` for producers
2. Set `min.insync.replicas=2` (with replication factor 3)
3. Disable unclean leader election:
   ```properties
   unclean.leader.election.enable=false
   ```

---

## Recovery Procedures

### Consumer Reset to Latest

```bash
# Stop consumers first
kubectl scale deployment poultry-backend -n poultry-platform --replicas=0

# Reset to latest offset
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group poultry-platform --topic <topic> \
  --reset-offsets --to-latest --execute

# Restart consumers
kubectl scale deployment poultry-backend -n poultry-platform --replicas=3
```

### Consumer Reset to Specific Time

```bash
# Reset to messages from 1 hour ago
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group poultry-platform --topic <topic> \
  --reset-offsets --to-datetime 2024-01-15T10:00:00.000 --execute
```

### Broker Recovery

```bash
# Remove broker from cluster (if corrupted)
kubectl delete pvc data-kafka-0 -n poultry-platform

# Let StatefulSet recreate
kubectl delete pod kafka-0 -n poultry-platform

# Verify broker rejoined
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-broker-api-versions --bootstrap-server localhost:9092
```

### Emergency: Delete and Recreate Topic

```bash
# WARNING: This deletes all messages
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-topics --bootstrap-server localhost:9092 \
  --delete --topic <topic-name>

kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-topics --bootstrap-server localhost:9092 \
  --create --topic <topic-name> \
  --partitions 6 --replication-factor 1
```

---

## Monitoring Queries

### Consumer Lag Dashboard

```bash
#!/bin/bash
# kafka-lag-check.sh

echo "=== Kafka Consumer Lag ==="
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group poultry-platform 2>/dev/null | \
  awk 'NR>1 {sum+=$6} END {print "Total Lag: " sum}'
```

### Topic Health Check

```bash
#!/bin/bash
# kafka-health.sh

echo "=== Broker Status ==="
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-broker-api-versions --bootstrap-server localhost:9092 | head -5

echo -e "\n=== Under-replicated Partitions ==="
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-topics --bootstrap-server localhost:9092 \
  --describe --under-replicated-partitions

echo -e "\n=== Consumer Groups ==="
kubectl exec -it kafka-0 -n poultry-platform -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

---

## Configuration Reference

### Producer Settings

```yaml
spring:
  kafka:
    producer:
      bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      properties:
        linger.ms: 10
        batch.size: 16384
        buffer.memory: 33554432
```

### Consumer Settings

```yaml
spring:
  kafka:
    consumer:
      group-id: poultry-platform
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.poultry.*
        max.poll.records: 500
        session.timeout.ms: 30000
```

---

## Escalation

If unable to resolve:
1. Check Confluent documentation
2. Escalate to infrastructure team
3. Consider Confluent support (if using managed service)

## Related Runbooks

- [API Issues](./API_ISSUES.md)
- [Database Issues](./DATABASE_ISSUES.md)
- [Payment Issues](./PAYMENT_ISSUES.md)
