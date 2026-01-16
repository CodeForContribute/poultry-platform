# Data Handling Documentation

**Poultry Platform**

**Last Updated:** January 16, 2026
**Version:** 1.0
**Classification:** Internal / Confidential

---

## Table of Contents

1. [Overview](#1-overview)
2. [Data Classification](#2-data-classification)
3. [Data Flow Diagrams](#3-data-flow-diagrams)
4. [Encryption Standards](#4-encryption-standards)
5. [Access Controls](#5-access-controls)
6. [Audit Procedures](#6-audit-procedures)
7. [Data Lifecycle Management](#7-data-lifecycle-management)
8. [Security Measures](#8-security-measures)
9. [Incident Response](#9-incident-response)
10. [Compliance Monitoring](#10-compliance-monitoring)

---

## 1. Overview

### 1.1 Purpose

This document provides comprehensive documentation of data handling practices for the Poultry Platform. It serves as a reference for:
- Development teams implementing data handling
- Security teams conducting audits
- Compliance teams ensuring regulatory adherence
- Operations teams managing data infrastructure

### 1.2 Scope

This document covers:
- All personal and business data processed by the Platform
- Data at rest, in transit, and in processing
- All system components including backend, mobile apps, and web portal
- Third-party integrations and data sharing

### 1.3 Definitions

| Term | Definition |
|------|------------|
| **PII** | Personally Identifiable Information |
| **SPI** | Sensitive Personal Information |
| **At Rest** | Data stored in databases, files, or backups |
| **In Transit** | Data being transmitted between systems |
| **In Processing** | Data actively being processed in memory |

---

## 2. Data Classification

### 2.1 Classification Levels

| Level | Description | Examples | Handling Requirements |
|-------|-------------|----------|----------------------|
| **Public** | Information intended for public access | Marketing content, public listings | Standard protection |
| **Internal** | Business information for internal use | Analytics reports, operational data | Access restricted to employees |
| **Confidential** | Sensitive business or personal data | User PII, transaction records | Encryption required, access logging |
| **Restricted** | Highly sensitive data | Bank accounts, passwords, payment info | Strong encryption, strict access controls |

### 2.2 Data Inventory

#### 2.2.1 User Data

| Data Element | Classification | Storage Location | Encryption | Retention |
|--------------|----------------|------------------|------------|-----------|
| User ID | Confidential | PostgreSQL | At rest | Permanent |
| Email | Confidential | PostgreSQL | At rest | Account + 7 years |
| Phone | Confidential | PostgreSQL | At rest | Account + 7 years |
| Password | Restricted | PostgreSQL | Bcrypt hash | Account lifetime |
| Name | Confidential | PostgreSQL | At rest | Account + 7 years |
| Address | Confidential | PostgreSQL | At rest | Account + 7 years |
| Profile Photo | Internal | S3 | At rest | Account lifetime |

#### 2.2.2 Business Data

| Data Element | Classification | Storage Location | Encryption | Retention |
|--------------|----------------|------------------|------------|-----------|
| Business Name | Confidential | PostgreSQL | At rest | Account + 7 years |
| GSTIN | Confidential | PostgreSQL | At rest | Account + 8 years |
| PAN | Restricted | PostgreSQL | At rest (AES-256) | Account + 8 years |
| FSSAI License | Confidential | PostgreSQL | At rest | Account + 7 years |
| Bank Account | Restricted | PostgreSQL | AES-256-GCM | Account + 8 years |
| UPI ID | Restricted | PostgreSQL | AES-256-GCM | Account + 8 years |

#### 2.2.3 Transaction Data

| Data Element | Classification | Storage Location | Encryption | Retention |
|--------------|----------------|------------------|------------|-----------|
| Order ID | Confidential | PostgreSQL | At rest | 8 years |
| Order Amount | Confidential | PostgreSQL | At rest | 8 years |
| Payment ID | Restricted | PostgreSQL | At rest | 8 years |
| Invoice | Confidential | S3 | At rest | 8 years |
| Delivery Address | Confidential | PostgreSQL | At rest | 8 years |

#### 2.2.4 Technical Data

| Data Element | Classification | Storage Location | Encryption | Retention |
|--------------|----------------|------------------|------------|-----------|
| Session Token | Restricted | Redis | In memory | 24 hours |
| Refresh Token | Restricted | PostgreSQL | Hashed | 30 days |
| Device ID | Internal | PostgreSQL | At rest | Account lifetime |
| IP Address | Internal | Logs | At rest | 90 days |
| User Agent | Internal | Logs | At rest | 90 days |

---

## 3. Data Flow Diagrams

### 3.1 System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                    CLIENTS                                           │
│  ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐                    │
│  │   iOS App       │   │  Android App    │   │   Web Portal    │                    │
│  │  (Buyer)        │   │   (Buyer)       │   │   (Seller)      │                    │
│  └────────┬────────┘   └────────┬────────┘   └────────┬────────┘                    │
│           │                     │                     │                              │
│           └─────────────────────┼─────────────────────┘                              │
│                                 │                                                    │
│                           HTTPS (TLS 1.3)                                           │
│                                 │                                                    │
└─────────────────────────────────┼────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              EDGE LAYER                                              │
│  ┌─────────────────────────────────────────────────────────────────────────────┐    │
│  │                        Cloud Load Balancer                                   │    │
│  │                  (SSL Termination, DDoS Protection)                         │    │
│  └─────────────────────────────────────────────────────────────────────────────┘    │
│                                 │                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────────┐    │
│  │                          WAF (Web Application Firewall)                      │    │
│  │                    (OWASP Rules, Rate Limiting, IP Filtering)               │    │
│  └─────────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────┼────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                           APPLICATION LAYER                                          │
│                                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────────┐    │
│  │                           API Gateway                                        │    │
│  │            (Authentication, Rate Limiting, Request Validation)               │    │
│  └────────────────────────────────┬────────────────────────────────────────────┘    │
│                                   │                                                  │
│           ┌───────────────────────┼───────────────────────┐                         │
│           │                       │                       │                         │
│           ▼                       ▼                       ▼                         │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐                  │
│  │  Auth Service   │    │  Core Service   │    │ Payment Service │                  │
│  │                 │    │                 │    │                 │                  │
│  │ - Login/Register│    │ - Users         │    │ - Orders        │                  │
│  │ - Token Mgmt    │    │ - Products      │    │ - Payments      │                  │
│  │ - Password Reset│    │ - Categories    │    │ - Refunds       │                  │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘                  │
│                                                                                      │
└─────────────────────────────────┼────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              DATA LAYER                                              │
│                                                                                      │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐                  │
│  │   PostgreSQL    │    │     Redis       │    │    AWS S3       │                  │
│  │                 │    │                 │    │                 │                  │
│  │ - Primary DB    │    │ - Session Cache │    │ - File Storage  │                  │
│  │ - TDE Enabled   │    │ - Rate Limits   │    │ - SSE-S3        │                  │
│  │ - SSL Required  │    │ - In-Memory     │    │ - Versioning    │                  │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘                  │
│                                                                                      │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 User Registration Flow

```
┌─────────┐     ┌──────────┐     ┌────────────┐     ┌──────────┐     ┌──────────┐
│  User   │     │  Client  │     │  Backend   │     │ Firebase │     │ Database │
│         │     │   App    │     │   API      │     │   Auth   │     │          │
└────┬────┘     └────┬─────┘     └─────┬──────┘     └────┬─────┘     └────┬─────┘
     │               │                 │                 │                │
     │ 1. Enter      │                 │                 │                │
     │    Details    │                 │                 │                │
     │──────────────>│                 │                 │                │
     │               │                 │                 │                │
     │               │ 2. Validate     │                 │                │
     │               │    Input        │                 │                │
     │               │────────────────>│                 │                │
     │               │                 │                 │                │
     │               │                 │ 3. Create       │                │
     │               │                 │    Firebase User│                │
     │               │                 │────────────────>│                │
     │               │                 │                 │                │
     │               │                 │   4. Firebase   │                │
     │               │                 │      UID        │                │
     │               │                 │<────────────────│                │
     │               │                 │                 │                │
     │               │                 │ 5. Hash Password│                │
     │               │                 │    (bcrypt)     │                │
     │               │                 │────────┐        │                │
     │               │                 │        │        │                │
     │               │                 │<───────┘        │                │
     │               │                 │                 │                │
     │               │                 │ 6. Store User   │                │
     │               │                 │    (Encrypted)  │                │
     │               │                 │────────────────────────────────>│
     │               │                 │                 │                │
     │               │                 │                 │    7. Confirm │
     │               │                 │<────────────────────────────────│
     │               │                 │                 │                │
     │               │ 8. JWT Token    │                 │                │
     │               │<────────────────│                 │                │
     │               │                 │                 │                │
     │ 9. Success    │                 │                 │                │
     │<──────────────│                 │                 │                │
     │               │                 │                 │                │
```

### 3.3 Order and Payment Flow

```
┌─────────┐  ┌─────────┐  ┌──────────┐  ┌─────────┐  ┌──────────┐  ┌──────────┐
│  Buyer  │  │  App    │  │ Backend  │  │Razorpay │  │ Database │  │  Seller  │
└────┬────┘  └────┬────┘  └────┬─────┘  └────┬────┘  └────┬─────┘  └────┬─────┘
     │            │            │             │            │             │
     │ 1. Place   │            │             │            │             │
     │    Order   │            │             │            │             │
     │───────────>│            │             │            │             │
     │            │            │             │            │             │
     │            │ 2. Create  │             │            │             │
     │            │    Order   │             │            │             │
     │            │───────────>│             │            │             │
     │            │            │             │            │             │
     │            │            │ 3. Validate │            │             │
     │            │            │    & Store  │            │             │
     │            │            │─────────────────────────>│             │
     │            │            │             │            │             │
     │            │            │ 4. Create   │            │             │
     │            │            │    Razorpay │            │             │
     │            │            │    Order    │            │             │
     │            │            │────────────>│            │             │
     │            │            │             │            │             │
     │            │            │  5. Order   │            │             │
     │            │            │     ID      │            │             │
     │            │            │<────────────│            │             │
     │            │            │             │            │             │
     │            │ 6. Payment │             │            │             │
     │            │    Options │             │            │             │
     │            │<───────────│             │            │             │
     │            │            │             │            │             │
     │ 7. Select  │            │             │            │             │
     │    UPI     │            │             │            │             │
     │───────────>│            │             │            │             │
     │            │            │             │            │             │
     │            │ 8. Process │             │            │             │
     │            │    Payment │             │            │             │
     │            │───────────────────────────>           │             │
     │            │            │             │            │             │
     │            │            │ 9. Webhook  │            │             │
     │            │            │    Success  │            │             │
     │            │            │<────────────│            │             │
     │            │            │             │            │             │
     │            │            │ 10. Update  │            │             │
     │            │            │     Status  │            │             │
     │            │            │─────────────────────────>│             │
     │            │            │             │            │             │
     │            │            │ 11. Notify  │            │             │
     │            │            │     Seller  │            │             │
     │            │            │──────────────────────────────────────>│
     │            │            │             │            │             │
     │            │ 12. Order  │             │            │             │
     │            │ Confirmed  │             │            │             │
     │            │<───────────│             │            │             │
     │            │            │             │            │             │
     │ 13. Receipt│            │             │            │             │
     │<───────────│            │             │            │             │
     │            │            │             │            │             │
```

### 3.4 Data Synchronization Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        PRIMARY REGION (GCP)                              │
│                                                                          │
│  ┌─────────────┐         ┌─────────────┐         ┌─────────────┐        │
│  │  Backend    │ ──────> │  PostgreSQL │ ──────> │   Replica   │        │
│  │  Service    │         │   Primary   │  Sync   │   (Read)    │        │
│  └─────────────┘         └─────────────┘         └─────────────┘        │
│         │                       │                                        │
│         │                       │ Backup (Daily)                         │
│         │                       ▼                                        │
│         │                ┌─────────────┐                                 │
│         │                │  GCS Bucket │                                 │
│         │                │  (Encrypted)│                                 │
│         │                └─────────────┘                                 │
│         │                                                                │
│         │ Cache                                                          │
│         ▼                                                                │
│  ┌─────────────┐                                                         │
│  │    Redis    │                                                         │
│  │   Cluster   │                                                         │
│  └─────────────┘                                                         │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
                                  │
                                  │ Cross-Region Replication
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       DISASTER RECOVERY REGION                           │
│                                                                          │
│  ┌─────────────┐         ┌─────────────┐                                │
│  │   Backup    │         │  PostgreSQL │                                │
│  │   Storage   │         │   Standby   │                                │
│  └─────────────┘         └─────────────┘                                │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Encryption Standards

### 4.1 Encryption at Rest

| Component | Algorithm | Key Size | Key Management |
|-----------|-----------|----------|----------------|
| PostgreSQL (TDE) | AES | 256-bit | GCP KMS |
| Redis | N/A | Session data | In-memory only |
| S3 Files | AES | 256-bit | AWS KMS |
| Backups | AES | 256-bit | GCP KMS |
| Bank Accounts | AES-GCM | 256-bit | Application KMS |

### 4.2 Encryption in Transit

| Connection | Protocol | Version | Certificate |
|------------|----------|---------|-------------|
| Client to Load Balancer | TLS | 1.3 | Let's Encrypt |
| Internal Services | TLS | 1.2+ | Internal CA |
| Database Connections | TLS | 1.2+ | GCP Managed |
| API to Razorpay | TLS | 1.2+ | Razorpay Cert |

### 4.3 Sensitive Field Encryption

Bank account and UPI details use application-level encryption:

```
Encryption Process:
1. Generate random 96-bit IV (Initialization Vector)
2. Derive data encryption key from master key
3. Encrypt plaintext using AES-256-GCM
4. Store: IV || Ciphertext || Auth Tag

Storage Format:
{
  "encrypted_bank_account": "base64(IV || Ciphertext || Tag)",
  "key_version": "v1",
  "encrypted_at": "ISO8601 timestamp"
}

Key Rotation:
- Master keys rotated quarterly
- Re-encryption performed during maintenance windows
- Old key versions retained for 90 days for decryption
```

### 4.4 Password Hashing

```
Algorithm: bcrypt
Cost Factor: 12
Salt: Auto-generated (128-bit)

Process:
1. Receive plaintext password
2. Validate password strength requirements
3. Generate bcrypt hash with cost factor 12
4. Store hash in database
5. Plaintext never logged or stored
```

### 4.5 Key Management

```
Key Hierarchy:
┌─────────────────────────────────────┐
│           Master Key                │
│     (GCP Cloud KMS / HSM)           │
└─────────────────┬───────────────────┘
                  │
        ┌─────────┴─────────┐
        │                   │
        ▼                   ▼
┌───────────────┐   ┌───────────────┐
│   Data Keys   │   │  Key Wrapping │
│               │   │     Keys      │
└───────────────┘   └───────────────┘
        │
        ▼
┌───────────────────────────────────┐
│         Encrypted Data            │
│   (Bank accounts, sensitive PII)  │
└───────────────────────────────────┘

Key Access:
- Master key access: Infrastructure team only
- Data key derivation: Application service account
- Audit logging: All key operations logged
```

---

## 5. Access Controls

### 5.1 Role-Based Access Control (RBAC)

#### 5.1.1 Application Roles

| Role | Description | Data Access |
|------|-------------|-------------|
| **Buyer** | End-user purchasing products | Own profile, orders, payments |
| **Seller** | Business user selling products | Own profile, products, orders |
| **Support** | Customer support staff | Read user profiles, orders (masked) |
| **Admin** | Platform administrator | Full read, limited write |
| **Super Admin** | System administrator | Full access |

#### 5.1.2 Database Roles

| Role | Permissions | Use Case |
|------|-------------|----------|
| **app_read** | SELECT on all tables | Read replicas |
| **app_write** | SELECT, INSERT, UPDATE | Application service |
| **app_admin** | Full DML, limited DDL | Migrations |
| **dba** | Full access | Database administration |

### 5.2 Authentication

```
Authentication Flow:
1. User provides credentials (phone + password OR Firebase token)
2. Validate credentials against stored hash
3. Generate JWT access token (15 min expiry)
4. Generate refresh token (30 day expiry)
5. Store refresh token hash in database
6. Return tokens to client

JWT Structure:
{
  "header": {
    "alg": "RS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user_uuid",
    "role": "buyer|seller",
    "iat": timestamp,
    "exp": timestamp,
    "jti": "unique_token_id"
  }
}
```

### 5.3 Authorization Matrix

| Resource | Buyer | Seller | Support | Admin |
|----------|-------|--------|---------|-------|
| Own Profile | RWD | RWD | R | RW |
| Other Profiles | - | - | R (masked) | R |
| Own Orders | R | R | R | RW |
| Other Orders | - | - | R | RW |
| Products | R | RWD (own) | R | RWD |
| Payments | R (own) | R (own) | R (masked) | R |
| Analytics | - | R (own) | - | R |
| System Config | - | - | - | RW |

*R = Read, W = Write, D = Delete*

### 5.4 API Security

```
Request Authentication:
1. Extract Authorization header
2. Validate JWT signature
3. Check token expiration
4. Verify token not revoked
5. Extract user context
6. Authorize against resource

Rate Limiting:
- Anonymous: 10 requests/minute
- Authenticated: 100 requests/minute
- API keys: 1000 requests/minute
- Admin: 500 requests/minute

IP-based Restrictions:
- Admin endpoints: VPN only
- Webhooks: Whitelisted IPs
- Public APIs: Geographic restrictions (optional)
```

### 5.5 Infrastructure Access

| System | Access Method | Authentication | Authorization |
|--------|---------------|----------------|---------------|
| GCP Console | Web/CLI | SSO + MFA | IAM Roles |
| Database (Prod) | Cloud SQL Proxy | Service Account | IAM + DB Roles |
| Servers | SSH via IAP | Certificate + MFA | IAM |
| Logs | Cloud Logging | SSO | IAM Roles |
| Secrets | Secret Manager | Service Account | IAM |

---

## 6. Audit Procedures

### 6.1 Audit Log Categories

| Category | Events Captured | Retention |
|----------|-----------------|-----------|
| **Authentication** | Login, logout, failed attempts, password changes | 1 year |
| **Authorization** | Access denied, privilege escalation | 1 year |
| **Data Access** | Read sensitive data, bulk exports | 2 years |
| **Data Modification** | Create, update, delete records | 3 years |
| **Admin Actions** | Configuration changes, user management | 5 years |
| **System Events** | Service starts, errors, deployments | 90 days |

### 6.2 Audit Log Schema

```json
{
  "event_id": "uuid",
  "timestamp": "ISO8601",
  "event_type": "authentication|authorization|data_access|data_modification|admin|system",
  "action": "login|read|create|update|delete|etc",
  "actor": {
    "user_id": "uuid",
    "role": "string",
    "ip_address": "string",
    "user_agent": "string"
  },
  "resource": {
    "type": "user|order|product|etc",
    "id": "uuid",
    "attributes_accessed": ["field1", "field2"]
  },
  "context": {
    "request_id": "uuid",
    "session_id": "uuid",
    "service": "string"
  },
  "outcome": {
    "status": "success|failure",
    "reason": "string (if failure)"
  },
  "metadata": {
    "old_values": {},
    "new_values": {}
  }
}
```

### 6.3 Audit Procedures

#### 6.3.1 Daily Checks
- [ ] Review failed authentication attempts
- [ ] Check for unusual access patterns
- [ ] Verify backup completion
- [ ] Monitor error rates

#### 6.3.2 Weekly Reviews
- [ ] Analyze access control exceptions
- [ ] Review new user registrations
- [ ] Check for dormant accounts
- [ ] Verify key system configurations

#### 6.3.3 Monthly Audits
- [ ] Access rights review
- [ ] Third-party access audit
- [ ] Security patch status
- [ ] Compliance checklist review

#### 6.3.4 Quarterly Audits
- [ ] Penetration testing
- [ ] Vulnerability assessment
- [ ] Policy review and updates
- [ ] Key rotation verification

#### 6.3.5 Annual Audits
- [ ] Full security audit
- [ ] GDPR compliance review
- [ ] Disaster recovery test
- [ ] Third-party vendor assessment

### 6.4 Audit Alerts

| Alert | Threshold | Notification |
|-------|-----------|--------------|
| Failed logins | 5 in 10 min | Security team |
| Bulk data access | >1000 records | Security + Admin |
| Admin login | Any | Security team |
| Config change | Any | Operations + Security |
| New country access | Any | Security review |
| Off-hours access | Outside 6am-10pm | Security team |

### 6.5 Log Protection

```
Immutability:
- Logs written to append-only storage
- No delete permissions for application roles
- Archive to cold storage after 90 days

Integrity:
- Log entries include hash chain
- External log shipping to SIEM
- Tamper detection alerts

Access:
- Read: Security team, Auditors
- Write: Application service accounts only
- Delete: Requires dual approval
```

---

## 7. Data Lifecycle Management

### 7.1 Data Creation

```
Validation Pipeline:
1. Input sanitization
2. Format validation
3. Business rule validation
4. Duplicate detection
5. Encryption (if sensitive)
6. Storage
7. Audit log creation
```

### 7.2 Data Usage

```
Access Control:
1. Authenticate user
2. Authorize access to resource
3. Apply field-level filtering
4. Mask sensitive fields if applicable
5. Log access
6. Return data
```

### 7.3 Data Retention

| Data Type | Active Period | Archive Period | Deletion |
|-----------|---------------|----------------|----------|
| User accounts | Account lifetime | +7 years | Anonymize |
| Transaction records | 2 years | +6 years (8 total) | Delete |
| Session data | 24 hours | - | Auto-delete |
| Logs (application) | 90 days | - | Auto-delete |
| Logs (audit) | 1-5 years | +2 years | Archive |
| Backups | 30 days | 90 days | Delete |
| Marketing consents | Until withdrawn | - | Delete |

### 7.4 Data Archival

```
Archive Process:
1. Identify data past active period
2. Verify no active references
3. Extract to archive format
4. Encrypt archive
5. Transfer to cold storage
6. Verify integrity
7. Remove from active storage
8. Update audit trail
```

### 7.5 Data Deletion

```
Deletion Process:
1. Receive deletion request
2. Verify authorization
3. Check retention requirements
4. Identify all data locations:
   - Primary database
   - Replicas
   - Caches
   - Backups
   - Third-party systems
5. Execute deletion/anonymization
6. Verify deletion
7. Update audit trail
8. Notify requester

Secure Deletion:
- Database: SQL DELETE with verification
- Files: Secure overwrite or versioned delete
- Caches: Immediate invalidation
- Backups: Excluded from future restores
```

---

## 8. Security Measures

### 8.1 Network Security

```
Architecture:
┌─────────────────────────────────────────────────────────┐
│                     PUBLIC INTERNET                      │
└─────────────────────────┬───────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                    DDoS Protection                       │
│                   (Cloud Armor)                          │
└─────────────────────────┬───────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                  Web Application Firewall                │
│              (OWASP Top 10 Rules, Custom Rules)         │
└─────────────────────────┬───────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                     VPC Network                          │
│  ┌─────────────────────────────────────────────────┐    │
│  │                Public Subnet                     │    │
│  │            (Load Balancers only)                 │    │
│  └──────────────────────┬──────────────────────────┘    │
│                         │                                │
│  ┌──────────────────────┴──────────────────────────┐    │
│  │               Private Subnet                     │    │
│  │    (Application Servers, Internal Services)      │    │
│  └──────────────────────┬──────────────────────────┘    │
│                         │                                │
│  ┌──────────────────────┴──────────────────────────┐    │
│  │               Database Subnet                    │    │
│  │         (PostgreSQL, Redis - No Public IP)       │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

### 8.2 Application Security

| Control | Implementation |
|---------|----------------|
| Input Validation | Server-side validation, parameterized queries |
| Output Encoding | Context-aware encoding |
| Authentication | JWT with RS256, bcrypt passwords |
| Session Management | Secure tokens, short expiry, rotation |
| CSRF Protection | SameSite cookies, CSRF tokens |
| XSS Prevention | Content Security Policy, sanitization |
| SQL Injection | ORM with parameterized queries |
| File Upload | Type validation, virus scanning, isolated storage |

### 8.3 API Security

```
Security Headers:
- Strict-Transport-Security: max-age=31536000; includeSubDomains
- X-Content-Type-Options: nosniff
- X-Frame-Options: DENY
- X-XSS-Protection: 1; mode=block
- Content-Security-Policy: default-src 'self'
- Referrer-Policy: strict-origin-when-cross-origin

Rate Limiting:
- Per-IP limits
- Per-user limits
- Per-endpoint limits
- Graduated penalties for abuse
```

### 8.4 Database Security

| Control | Implementation |
|---------|----------------|
| Access | Service accounts, principle of least privilege |
| Encryption | TDE (AES-256), TLS connections |
| Audit | Query logging, access logging |
| Backups | Encrypted, access controlled, tested restores |
| Patching | Managed service, auto-patching enabled |
| Network | Private IP only, VPC peering |

### 8.5 Secrets Management

```
Secret Storage:
- GCP Secret Manager for all secrets
- No secrets in code or config files
- Secrets accessed at runtime only
- Secret rotation enforced

Secret Categories:
- Database credentials: 90-day rotation
- API keys: 180-day rotation
- Encryption keys: Quarterly rotation
- Service accounts: Annual rotation

Access:
- Application: Service account with minimal scope
- Developers: No production secret access
- Operations: Read-only via approved process
```

---

## 9. Incident Response

### 9.1 Incident Classification

| Severity | Description | Response Time | Examples |
|----------|-------------|---------------|----------|
| **Critical** | Active breach, data exfiltration | Immediate | Unauthorized access, ransomware |
| **High** | Potential breach, vulnerability exploited | 1 hour | Unusual data access, failed auth spike |
| **Medium** | Security misconfiguration, suspicious activity | 4 hours | Missing patches, unusual patterns |
| **Low** | Policy violation, minor issues | 24 hours | Weak password, minor misconfiguration |

### 9.2 Response Procedures

```
Phase 1: Detection (0-15 min)
- Alert received
- Initial triage
- Classify severity
- Notify response team

Phase 2: Containment (15-60 min)
- Isolate affected systems
- Preserve evidence
- Block attack vectors
- Assess scope

Phase 3: Eradication (1-24 hours)
- Identify root cause
- Remove threat
- Patch vulnerabilities
- Reset credentials

Phase 4: Recovery (1-48 hours)
- Restore from backups if needed
- Verify system integrity
- Re-enable services
- Monitor for recurrence

Phase 5: Lessons Learned (1 week)
- Document incident
- Identify improvements
- Update procedures
- Train team
```

### 9.3 Contact Escalation

| Level | Time | Contacts |
|-------|------|----------|
| L1 | 0-15 min | On-call engineer |
| L2 | 15-30 min | Security team lead |
| L3 | 30-60 min | Engineering director |
| L4 | 1-2 hours | CTO, Legal |
| L5 | 2+ hours | CEO, Board (if required) |

---

## 10. Compliance Monitoring

### 10.1 Compliance Frameworks

| Framework | Applicability | Status |
|-----------|---------------|--------|
| GDPR | EEA users | Compliant |
| DPDP Act 2023 | Indian users | Compliant |
| IT Act 2000 | All users | Compliant |
| PCI-DSS | Payment data | Via Razorpay |
| RBI Guidelines | Financial data | Compliant |

### 10.2 Monitoring Dashboards

```
Security Dashboard:
- Failed authentication rate
- Suspicious activity alerts
- Vulnerability scan results
- Patch compliance status

Privacy Dashboard:
- Data subject requests (count, status)
- Consent rate
- Data deletion requests
- Cross-border transfer volume

Compliance Dashboard:
- Policy acknowledgment rate
- Training completion
- Audit findings (open/closed)
- Third-party assessment status
```

### 10.3 Continuous Improvement

| Activity | Frequency | Owner |
|----------|-----------|-------|
| Vulnerability scanning | Weekly | Security |
| Penetration testing | Quarterly | External vendor |
| Policy review | Annually | Legal + Security |
| Training | Annually | HR + Security |
| Compliance assessment | Annually | External auditor |
| Disaster recovery test | Annually | Operations |

---

## Appendices

### Appendix A: Data Classification Decision Tree

```
Is the data about an identified or identifiable person?
├── No → Is it business confidential?
│       ├── No → PUBLIC
│       └── Yes → INTERNAL
└── Yes → Is it financial, health, or identity data?
        ├── No → CONFIDENTIAL
        └── Yes → RESTRICTED
```

### Appendix B: Encryption Algorithm Reference

| Use Case | Algorithm | Key Size | Mode |
|----------|-----------|----------|------|
| Database TDE | AES | 256 | GCM |
| File encryption | AES | 256 | GCM |
| Password hashing | bcrypt | N/A | N/A |
| Token signing | RS256 | 2048 | N/A |
| TLS | ECDHE+AES | 256 | GCM |

### Appendix C: Glossary

| Term | Definition |
|------|------------|
| AES | Advanced Encryption Standard |
| GCM | Galois/Counter Mode |
| TDE | Transparent Data Encryption |
| KMS | Key Management Service |
| HSM | Hardware Security Module |
| RBAC | Role-Based Access Control |
| JWT | JSON Web Token |
| TLS | Transport Layer Security |
| IAM | Identity and Access Management |

---

*This document is classified as INTERNAL and should not be shared externally without approval.*

**Document Control:**
| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | January 16, 2026 | Engineering Team | Initial release |
