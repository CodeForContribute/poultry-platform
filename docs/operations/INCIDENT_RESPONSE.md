# Incident Response Plan

## Overview

This document outlines the incident response procedures for the Poultry Platform. All team members involved in operations must be familiar with these procedures and follow them during any incident.

**Document Owner**: Platform Engineering Team
**Last Updated**: January 2026
**Review Frequency**: Quarterly

---

## Table of Contents

1. [Incident Classification](#incident-classification)
2. [Roles and Responsibilities](#roles-and-responsibilities)
3. [Escalation Procedures](#escalation-procedures)
4. [Incident Response Process](#incident-response-process)
5. [Communication Templates](#communication-templates)
6. [Post-Mortem Process](#post-mortem-process)
7. [On-Call Rotation Guidelines](#on-call-rotation-guidelines)

---

## Incident Classification

### Priority Levels

| Priority | Name | Description | Response Time | Resolution Target | Examples |
|----------|------|-------------|---------------|-------------------|----------|
| **P1** | Critical | Complete service outage or data breach affecting all users | < 15 minutes | < 1 hour | Database down, security breach, payment system failure, complete API outage |
| **P2** | High | Major feature degradation affecting significant user base | < 30 minutes | < 4 hours | Payment processing delays, partial API failures, authentication issues, Kafka cluster down |
| **P3** | Medium | Minor feature degradation or isolated issues | < 2 hours | < 24 hours | Single endpoint errors, cache misses, slow queries, minor UI bugs |
| **P4** | Low | Minor issues with minimal user impact | < 8 hours | < 72 hours | Cosmetic issues, documentation errors, non-critical warnings |

### Impact Assessment Matrix

| Impact Level | User Impact | Revenue Impact | Data Impact |
|--------------|-------------|----------------|-------------|
| **Critical** | All users affected | Direct revenue loss | Data loss or corruption |
| **High** | >50% users affected | Significant revenue impact | Data integrity at risk |
| **Medium** | <50% users affected | Minor revenue impact | No data impact |
| **Low** | <10% users affected | No revenue impact | No data impact |

### Severity Escalation Criteria

Escalate to higher priority if:
- Duration exceeds resolution target by 50%
- User impact increases significantly
- Additional systems become affected
- Media or regulatory attention involved

---

## Roles and Responsibilities

### Incident Response Team

| Role | Responsibilities | Required Skills |
|------|------------------|-----------------|
| **Incident Commander (IC)** | Overall incident coordination, decision making, resource allocation | Leadership, technical overview, communication |
| **Technical Lead** | Technical investigation and resolution, coordinates engineering efforts | Deep technical knowledge, troubleshooting |
| **Communications Lead** | Internal and external communications, stakeholder updates | Clear communication, writing |
| **Scribe** | Documentation of timeline, actions taken, decisions made | Attention to detail, real-time documentation |
| **Subject Matter Experts** | Provide domain-specific expertise as needed | Specialized technical knowledge |

### RACI Matrix

| Activity | IC | Tech Lead | Comms Lead | Scribe | SME |
|----------|-----|-----------|------------|--------|-----|
| Declare Incident | A/R | C | C | I | I |
| Initial Assessment | A | R | I | R | C |
| Technical Resolution | A | R | I | I | R |
| Stakeholder Comms | A | C | R | I | I |
| Documentation | A | C | I | R | C |
| Post-Mortem | A | R | C | R | C |

**Legend**: R = Responsible, A = Accountable, C = Consulted, I = Informed

---

## Escalation Procedures

### P1 - Critical Incident Escalation

```
Time 0-15 min:
  ├─ On-call engineer receives alert
  ├─ Initial assessment and triage
  ├─ Notify Incident Commander
  └─ Create incident channel: #incident-YYYYMMDD-brief-desc

Time 15-30 min:
  ├─ IC takes command
  ├─ Assemble response team
  ├─ Begin technical investigation
  └─ Send initial stakeholder notification

Time 30-60 min:
  ├─ Implement mitigation/fix
  ├─ Escalate to management if unresolved
  ├─ Update status page
  └─ Notify affected customers

Beyond 60 min:
  ├─ Escalate to VP Engineering
  ├─ Consider engaging vendors
  ├─ Hourly stakeholder updates
  └─ Prepare for extended incident
```

### Escalation Contacts

| Level | Role | Contact Method | When to Contact |
|-------|------|----------------|-----------------|
| **L1** | On-Call Engineer | PagerDuty | All alerts |
| **L2** | Tech Lead | PagerDuty + Slack | P1/P2 not resolved in 30 min |
| **L3** | Engineering Manager | Phone + Slack | P1 not resolved in 1 hour |
| **L4** | VP Engineering | Phone | P1 > 2 hours, data breach |
| **L5** | CTO/CEO | Phone | P1 > 4 hours, regulatory |

### External Escalation

| Vendor | When to Contact | Contact Method | SLA |
|--------|-----------------|----------------|-----|
| AWS Support | Infrastructure issues | AWS Console/Phone | Per support tier |
| Razorpay | Payment failures | Dashboard/Email | 1 hour response |
| MSG91/Twilio | SMS delivery issues | Support portal | 2 hour response |
| PostgreSQL Support | Critical DB issues | Support contract | Per contract |

---

## Incident Response Process

### Phase 1: Detection and Alerting

```bash
# Monitoring stack automatically alerts on:
- Health check failures (/api/actuator/health)
- Error rate > 5%
- Response time p99 > 2s
- CPU > 80%
- Memory > 85%
- Database connection failures
- Kafka consumer lag > threshold
```

### Phase 2: Initial Response (First 15 minutes)

1. **Acknowledge the Alert**
   ```bash
   # Acknowledge in PagerDuty
   # Join incident Slack channel
   ```

2. **Initial Assessment**
   - What services are affected?
   - What is the user impact?
   - When did the issue start?
   - What changed recently?

3. **Quick Diagnostic Commands**
   ```bash
   # Kubernetes health check
   kubectl get pods -n poultry-platform
   kubectl describe pod <pod-name> -n poultry-platform

   # Application logs
   kubectl logs -f deployment/poultry-backend -n poultry-platform --tail=100

   # Health endpoints
   curl -s https://api.poultry-platform.com/api/actuator/health | jq

   # Metrics
   curl -s https://api.poultry-platform.com/api/actuator/prometheus | grep -E "(http_server|hikaricp|kafka)"
   ```

4. **Determine Priority and Escalate**

### Phase 3: Investigation and Diagnosis

1. **Gather Data**
   ```bash
   # Recent deployments
   kubectl rollout history deployment/poultry-backend -n poultry-platform

   # Resource usage
   kubectl top pods -n poultry-platform

   # Database connections
   kubectl exec -it postgres-0 -n poultry-platform -- psql -U postgres -c "SELECT count(*) FROM pg_stat_activity;"

   # Kafka consumer lag
   kubectl exec -it kafka-0 -n poultry-platform -- kafka-consumer-groups --bootstrap-server localhost:9092 --describe --all-groups
   ```

2. **Check Recent Changes**
   - Review last deployments
   - Check recent configuration changes
   - Review infrastructure changes
   - Check third-party service status

3. **Root Cause Identification**
   - Correlate symptoms with timeline
   - Identify affected components
   - Determine root cause vs symptoms

### Phase 4: Mitigation and Resolution

1. **Implement Mitigation** (Stop the bleeding)
   - Rollback if deployment-related
   - Scale resources if capacity-related
   - Failover if infrastructure-related
   - Disable feature flag if feature-related

2. **Apply Fix**
   - Emergency fix deployment if necessary
   - Configuration changes
   - Database fixes

3. **Verify Resolution**
   ```bash
   # Verify health
   kubectl get pods -n poultry-platform
   curl -s https://api.poultry-platform.com/api/actuator/health | jq

   # Verify error rates returning to normal
   # Check monitoring dashboards
   ```

### Phase 5: Post-Incident

1. **Confirm Resolution**
2. **Update Status Page**
3. **Notify Stakeholders**
4. **Schedule Post-Mortem**
5. **Document in Incident Log**

---

## Communication Templates

### Internal Status Update Template

```markdown
**Incident Update - [PRIORITY] - [Brief Description]**

**Status**: [Investigating | Identified | Monitoring | Resolved]
**Started**: YYYY-MM-DD HH:MM UTC
**Current Time**: YYYY-MM-DD HH:MM UTC
**Duration**: X hours Y minutes

**Incident Commander**: [Name]
**Technical Lead**: [Name]

**Impact Summary**:
- [Describe user/business impact]
- [Affected services/features]
- [Number of users affected if known]

**Current Actions**:
- [What is being done right now]
- [Next planned action]

**Timeline**:
- HH:MM - [Event/Action]
- HH:MM - [Event/Action]

**Next Update**: HH:MM UTC (or as situation changes)
```

### External Customer Communication Template

```markdown
**[Service Name] Status Update**

**Status**: [Investigating | Identified | Monitoring | Resolved]
**Time**: YYYY-MM-DD HH:MM UTC

**Summary**:
We are currently [investigating/experiencing/recovering from] an issue affecting [specific feature/service].

**Impact**:
[Clear description of what customers are experiencing]

**What We're Doing**:
[Brief, non-technical description of remediation efforts]

**Next Update**:
We will provide another update in [X] minutes/hours or when we have more information.

**Apology** (for significant incidents):
We apologize for any inconvenience this may cause and appreciate your patience.
```

### Initial Stakeholder Notification (P1)

```markdown
Subject: [P1 INCIDENT] - [Brief Description] - [Time]

A P1 incident has been declared.

**Summary**: [One sentence description]
**Impact**: [User/business impact]
**Started**: [Time]
**Incident Commander**: [Name]

**Current Actions**:
- Team assembled and investigating
- [Specific action being taken]

**Next update in 30 minutes or sooner if resolved.**

Incident Channel: #incident-[date]-[brief-desc]
```

### Resolution Notification

```markdown
Subject: [RESOLVED] - [Incident Description]

**Incident Resolved**

The incident affecting [services] has been resolved.

**Duration**: [Start time] to [End time] ([Total duration])

**Root Cause**:
[Brief, non-technical explanation]

**Resolution**:
[What was done to fix it]

**Prevention**:
[Brief mention of follow-up actions]

A post-mortem will be conducted and shared within 5 business days.
```

---

## Post-Mortem Process

### Timeline

| Timeframe | Action |
|-----------|--------|
| Within 48 hours | Schedule post-mortem meeting |
| Within 5 business days | Complete post-mortem document |
| Within 2 weeks | Complete all action items or create tickets |

### Post-Mortem Template

```markdown
# Post-Mortem: [Incident Title]

**Date**: YYYY-MM-DD
**Authors**: [Names]
**Status**: [Draft | Review | Final]
**Severity**: [P1/P2/P3/P4]

## Executive Summary
[2-3 sentence summary of the incident, impact, and resolution]

## Timeline (All times in UTC)
| Time | Event |
|------|-------|
| HH:MM | [Event description] |

## Impact
- **Duration**: X hours Y minutes
- **Users Affected**: [Number/Percentage]
- **Revenue Impact**: [If applicable]
- **SLA Impact**: [If applicable]

## Root Cause Analysis

### What Happened
[Detailed technical description]

### Why It Happened
[Analysis using 5 Whys or similar technique]

### Contributing Factors
- [Factor 1]
- [Factor 2]

## Detection
- **How was it detected?**: [Monitoring/User report/etc.]
- **Time to detect**: [Duration]
- **Could we have detected it earlier?**: [Yes/No - explanation]

## Response
- **Time to respond**: [Duration]
- **Time to mitigate**: [Duration]
- **Time to resolve**: [Duration]

### What Went Well
- [Positive aspect 1]
- [Positive aspect 2]

### What Could Be Improved
- [Improvement area 1]
- [Improvement area 2]

## Action Items
| Action | Owner | Priority | Due Date | Status |
|--------|-------|----------|----------|--------|
| [Action item] | [Name] | [High/Med/Low] | [Date] | [Todo/Done] |

## Lessons Learned
1. [Lesson 1]
2. [Lesson 2]

## References
- [Link to incident channel]
- [Link to monitoring dashboard]
- [Link to relevant documentation]
```

### Post-Mortem Meeting Agenda

1. **Introduction** (5 min)
   - Review ground rules (blameless)
   - Confirm attendees and roles

2. **Timeline Review** (15 min)
   - Walk through events chronologically
   - Fill in gaps

3. **Root Cause Analysis** (20 min)
   - Apply 5 Whys technique
   - Identify contributing factors

4. **What Went Well** (10 min)
   - Acknowledge effective responses

5. **What Could Be Improved** (15 min)
   - Identify process improvements
   - Identify technical improvements

6. **Action Items** (10 min)
   - Define specific, actionable items
   - Assign owners and due dates

7. **Wrap-up** (5 min)
   - Summarize key learnings
   - Schedule follow-up if needed

### Blameless Post-Mortem Guidelines

1. **Focus on systems, not individuals**
2. **Assume everyone acted with best intentions**
3. **Seek to understand, not to blame**
4. **Encourage honesty and transparency**
5. **Document everything for future learning**
6. **Follow up on action items**

---

## On-Call Rotation Guidelines

### On-Call Schedule

| Team | Primary Rotation | Secondary Rotation | Hours |
|------|------------------|-------------------|-------|
| Backend | Weekly, Monday 09:00 UTC | Weekly, offset by 12h | 24/7 |
| Infrastructure | Weekly | As needed | 24/7 |
| Database | Bi-weekly | As needed | 24/7 |

### On-Call Responsibilities

1. **Primary On-Call**
   - Respond to all alerts within SLA
   - Perform initial triage and investigation
   - Escalate when necessary
   - Document actions taken
   - Hand off to next on-call

2. **Secondary On-Call**
   - Backup for primary
   - Available for escalation
   - Assist with P1/P2 incidents

### On-Call Expectations

| Expectation | Requirement |
|-------------|-------------|
| Response Time | P1: 15 min, P2: 30 min, P3: 2 hours |
| Availability | Must be reachable and able to work |
| Equipment | Laptop, internet, phone |
| Knowledge | Familiar with runbooks and systems |

### On-Call Handoff Checklist

```markdown
## On-Call Handoff - [Date]

### Outgoing: [Name]
### Incoming: [Name]

**Current Status**: [Green/Yellow/Red]

**Active Issues**:
- [Issue 1 - Status]
- [Issue 2 - Status]

**Recent Incidents**:
- [Incident summary]

**Upcoming Changes**:
- [Deployment/maintenance scheduled]

**Notes**:
- [Anything the incoming on-call should know]

**Handoff Confirmed**: [Time]
```

### On-Call Best Practices

1. **Before Your Shift**
   - Review recent incidents
   - Check scheduled deployments
   - Verify access to all systems
   - Test alerting (receive test page)

2. **During Your Shift**
   - Keep laptop and phone charged
   - Stay within reliable network coverage
   - Document everything you do
   - Don't hesitate to escalate

3. **After Your Shift**
   - Complete handoff document
   - Brief incoming on-call
   - Ensure clean handoff of any active issues

### On-Call Wellness

- Maximum consecutive on-call days: 7
- Minimum rest between rotations: 7 days
- Comp time for after-hours incidents
- No penalties for escalating or calling for help

---

## Appendix

### Useful Links

| Resource | URL |
|----------|-----|
| Monitoring Dashboard | https://grafana.poultry-platform.com |
| Status Page | https://status.poultry-platform.com |
| Incident Slack Channel | #incidents |
| On-Call Schedule | https://pagerduty.com/schedules/poultry |
| Runbooks | /docs/operations/runbooks/ |

### Emergency Contacts

| Role | Name | Phone | Email |
|------|------|-------|-------|
| Engineering Manager | [Name] | [Phone] | [Email] |
| VP Engineering | [Name] | [Phone] | [Email] |
| CTO | [Name] | [Phone] | [Email] |
| Security Lead | [Name] | [Phone] | [Email] |

### Reference Documents

- [Production Runbooks](./runbooks/)
- [Alert Runbook](./ALERT_RUNBOOK.md)
- [Disaster Recovery Plan](./DISASTER_RECOVERY.md)
- [Deployment Guide](./DEPLOYMENT_GUIDE.md)
