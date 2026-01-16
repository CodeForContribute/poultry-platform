# GDPR Compliance Documentation

**Poultry Platform**

**Last Updated:** January 16, 2026
**Version:** 1.0

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Scope and Applicability](#2-scope-and-applicability)
3. [Data Processing Activities](#3-data-processing-activities)
4. [Lawful Basis for Processing](#4-lawful-basis-for-processing)
5. [Data Subject Rights](#5-data-subject-rights)
6. [Consent Management](#6-consent-management)
7. [Data Export Procedures](#7-data-export-procedures)
8. [Right to Deletion Process](#8-right-to-deletion-process)
9. [Data Breach Procedures](#9-data-breach-procedures)
10. [Data Protection Impact Assessment](#10-data-protection-impact-assessment)
11. [Sub-Processors](#11-sub-processors)
12. [International Data Transfers](#12-international-data-transfers)
13. [Records of Processing Activities](#13-records-of-processing-activities)
14. [Contact Information](#14-contact-information)

---

## 1. Introduction

### 1.1 Purpose
This document outlines Poultry Platform's compliance with the General Data Protection Regulation (GDPR) (EU) 2016/679. It serves as a comprehensive guide to our data protection practices, policies, and procedures for users within the European Economic Area (EEA).

### 1.2 Commitment
Poultry Platform is committed to:
- Protecting the fundamental rights and freedoms of natural persons
- Ensuring lawful, fair, and transparent data processing
- Implementing appropriate technical and organizational measures
- Facilitating the exercise of data subject rights
- Maintaining accountability for all data processing activities

### 1.3 Document Scope
This document covers:
- Data processing activities involving EEA residents
- Technical implementations for GDPR compliance
- Operational procedures for handling data subject requests
- Incident response for data breaches
- Vendor management and data transfers

---

## 2. Scope and Applicability

### 2.1 Territorial Scope
GDPR applies to our processing of personal data when:
- The data subject is located in the EEA
- We offer goods or services to individuals in the EEA
- We monitor behavior of individuals in the EEA

### 2.2 Material Scope
This compliance framework applies to:
- All personal data processed through the Platform
- Automated processing of personal data
- Manual processing of personal data in filing systems

### 2.3 Excluded Processing
The following are outside GDPR scope:
- Purely personal or household activities
- Anonymous data that cannot be attributed to individuals
- Data processed for law enforcement purposes (separate regime)

---

## 3. Data Processing Activities

### 3.1 Register of Processing Activities (ROPA)

#### 3.1.1 User Account Management

| Field | Details |
|-------|---------|
| **Processing Activity** | User registration and account management |
| **Categories of Data Subjects** | Buyers, Sellers, Administrators |
| **Categories of Personal Data** | Name, email, phone, address, business information |
| **Purpose** | Account creation, authentication, user identification |
| **Legal Basis** | Contract performance (Art. 6(1)(b)) |
| **Retention Period** | Account lifetime + 7 years |
| **Recipients** | Internal staff, Firebase (authentication) |
| **Technical Measures** | Encryption, access controls, audit logs |

#### 3.1.2 Transaction Processing

| Field | Details |
|-------|---------|
| **Processing Activity** | Order processing and payment handling |
| **Categories of Data Subjects** | Buyers, Sellers |
| **Categories of Personal Data** | Transaction details, payment information, delivery addresses |
| **Purpose** | Facilitating marketplace transactions |
| **Legal Basis** | Contract performance (Art. 6(1)(b)) |
| **Retention Period** | 8 years (tax/legal requirements) |
| **Recipients** | Counter-party (buyer/seller), Razorpay, delivery partners |
| **Technical Measures** | PCI-DSS compliant payment processing, encryption |

#### 3.1.3 Analytics and Improvement

| Field | Details |
|-------|---------|
| **Processing Activity** | Usage analytics and platform improvement |
| **Categories of Data Subjects** | All users |
| **Categories of Personal Data** | Usage patterns, device information, interaction data |
| **Purpose** | Service improvement, debugging, feature development |
| **Legal Basis** | Legitimate interests (Art. 6(1)(f)) |
| **Retention Period** | 2 years (aggregated indefinitely) |
| **Recipients** | Internal analytics team, Firebase Analytics |
| **Technical Measures** | Pseudonymization, data minimization |

#### 3.1.4 Marketing Communications

| Field | Details |
|-------|---------|
| **Processing Activity** | Promotional emails and notifications |
| **Categories of Data Subjects** | Users who have consented |
| **Categories of Personal Data** | Email address, name, preferences |
| **Purpose** | Marketing, promotions, newsletters |
| **Legal Basis** | Consent (Art. 6(1)(a)) |
| **Retention Period** | Until consent withdrawn |
| **Recipients** | Internal marketing team, email service provider |
| **Technical Measures** | Consent tracking, preference management |

#### 3.1.5 Customer Support

| Field | Details |
|-------|---------|
| **Processing Activity** | Handling support inquiries and complaints |
| **Categories of Data Subjects** | All users |
| **Categories of Personal Data** | Name, contact info, support ticket content |
| **Purpose** | Providing customer support |
| **Legal Basis** | Contract performance (Art. 6(1)(b)) |
| **Retention Period** | 5 years |
| **Recipients** | Support staff |
| **Technical Measures** | Ticketing system access controls |

### 3.2 Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           DATA SUBJECTS                                  │
│                    (Buyers, Sellers in EEA)                             │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         DATA COLLECTION POINTS                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                   │
│  │  Mobile App  │  │   Web App    │  │    APIs      │                   │
│  │   (iOS/Android)│ │(Seller Portal)│ │ (Integrations)│                  │
│  └──────────────┘  └──────────────┘  └──────────────┘                   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
           ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
           │   Firebase   │ │   Backend    │ │   Razorpay   │
           │Authentication│ │   Server     │ │   Payment    │
           │   (Google)   │ │   (GCP)      │ │   Gateway    │
           └──────────────┘ └──────────────┘ └──────────────┘
                    │               │               │
                    └───────────────┼───────────────┘
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          DATA STORAGE                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                   │
│  │  PostgreSQL  │  │   Redis      │  │    S3        │                   │
│  │  (Primary DB)│  │   (Cache)    │  │  (Files)     │                   │
│  │   Encrypted  │  │  Encrypted   │  │  Encrypted   │                   │
│  └──────────────┘  └──────────────┘  └──────────────┘                   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Lawful Basis for Processing

### 4.1 Legal Bases Used

| Legal Basis | GDPR Article | Use Cases |
|-------------|--------------|-----------|
| **Contract** | Art. 6(1)(b) | Account management, order processing, payment handling |
| **Consent** | Art. 6(1)(a) | Marketing communications, optional features |
| **Legitimate Interests** | Art. 6(1)(f) | Analytics, fraud prevention, security |
| **Legal Obligation** | Art. 6(1)(c) | Tax compliance, regulatory reporting |

### 4.2 Legitimate Interest Assessment (LIA)

#### 4.2.1 Analytics Processing

**Legitimate Interest Identified:**
Improving platform services and user experience through usage analytics.

**Necessity Test:**
- Analytics are essential for identifying bugs and performance issues
- Understanding user behavior helps prioritize feature development
- No less intrusive alternative available for these insights

**Balancing Test:**
- Data is pseudonymized where possible
- Users can opt-out of non-essential analytics
- Minimal impact on user privacy
- Processing aligns with user expectations

**Conclusion:** Processing is justified under legitimate interests.

#### 4.2.2 Fraud Prevention

**Legitimate Interest Identified:**
Protecting the Platform and users from fraudulent activity.

**Necessity Test:**
- Fraud prevention requires transaction monitoring
- Cannot effectively prevent fraud without pattern analysis

**Balancing Test:**
- Clear benefit to all users
- Limited scope (transaction patterns only)
- Users expect security measures
- Data minimization applied

**Conclusion:** Processing is justified under legitimate interests.

---

## 5. Data Subject Rights

### 5.1 Rights Overview

| Right | GDPR Article | Implementation |
|-------|--------------|----------------|
| **Right of Access** | Art. 15 | Data export feature in app |
| **Right to Rectification** | Art. 16 | Profile editing in app |
| **Right to Erasure** | Art. 17 | Account deletion feature |
| **Right to Restriction** | Art. 18 | Support request handling |
| **Right to Portability** | Art. 20 | Structured data export |
| **Right to Object** | Art. 21 | Marketing opt-out |
| **Rights related to automated decision-making** | Art. 22 | Manual review available |

### 5.2 Exercising Rights

#### 5.2.1 Self-Service Options
- **Profile Editing:** Settings > Profile
- **Marketing Opt-out:** Settings > Notifications > Marketing
- **Data Export:** Settings > Privacy > Download My Data
- **Account Deletion:** Settings > Privacy > Delete Account

#### 5.2.2 Contact Methods
- **Email:** gdpr@poultryplatform.com
- **In-App:** Settings > Privacy > Contact DPO
- **Mail:** [Registered Address]

### 5.3 Response Timelines

| Request Type | Initial Response | Completion |
|--------------|-----------------|------------|
| Information Request | 72 hours | 30 days |
| Access Request | 72 hours | 30 days |
| Rectification | 72 hours | 7 days |
| Erasure | 72 hours | 30 days |
| Restriction | 72 hours | 72 hours |
| Portability | 72 hours | 30 days |
| Objection | 72 hours | 30 days |

*Note: Complex requests may be extended by 2 months with notification.*

### 5.4 Identity Verification

Before processing requests, we verify identity through:
1. Account authentication (logged-in requests)
2. Email verification (email requests)
3. Phone verification (high-risk requests)
4. Government ID (in exceptional cases)

---

## 6. Consent Management

### 6.1 Consent Requirements

Valid consent must be:
- **Freely given:** No bundling with service terms
- **Specific:** Separate consent for different purposes
- **Informed:** Clear explanation of processing
- **Unambiguous:** Clear affirmative action

### 6.2 Consent Collection Points

| Purpose | Collection Point | Method |
|---------|-----------------|--------|
| Marketing emails | Registration, Settings | Checkbox (unchecked by default) |
| Push notifications | First app launch | OS permission dialog |
| Analytics cookies | Website visit | Cookie banner |
| Location access | Feature usage | OS permission dialog |

### 6.3 Consent Records

We maintain records of:
- Date and time of consent
- Method of consent
- Specific consent given
- Information provided at time of consent
- Consent withdrawal date (if applicable)

### 6.4 Withdrawal of Consent

Users can withdraw consent through:
- In-app settings
- Unsubscribe links in emails
- Contacting support
- Email to gdpr@poultryplatform.com

Withdrawal is processed within 72 hours.

### 6.5 Technical Implementation

```
Consent Record Schema:
{
  "user_id": "uuid",
  "consent_type": "marketing_email | push_notification | analytics",
  "status": "granted | withdrawn",
  "granted_at": "ISO 8601 timestamp",
  "withdrawn_at": "ISO 8601 timestamp | null",
  "collection_method": "registration_form | settings_page | cookie_banner",
  "version": "consent_policy_version",
  "ip_address": "hashed",
  "user_agent": "string"
}
```

---

## 7. Data Export Procedures

### 7.1 Right to Access (Art. 15)

Data subjects have the right to obtain:
- Confirmation of whether data is being processed
- Access to their personal data
- Information about processing purposes, categories, recipients
- Retention periods
- Information about rights
- Source of data (if not collected from subject)
- Existence of automated decision-making

### 7.2 Right to Portability (Art. 20)

Data subjects can receive their data in a structured, commonly used, machine-readable format for data:
- Provided by them directly
- Processed based on consent or contract
- Processed by automated means

### 7.3 Data Export Process

#### 7.3.1 Self-Service Export

1. User navigates to Settings > Privacy > Download My Data
2. User authenticates (password re-entry required)
3. System generates export package
4. User notified when ready (typically within 24 hours)
5. Download link sent via email (valid for 7 days)
6. Data provided in JSON and CSV formats

#### 7.3.2 Manual Export Request

1. User emails gdpr@poultryplatform.com
2. Identity verification performed
3. Request logged in tracking system
4. Export generated within 30 days
5. Secure delivery (encrypted file or secure portal)

### 7.4 Data Export Contents

**Account Data:**
- Profile information
- Contact details
- Business information
- Account settings

**Transaction Data:**
- Order history
- Payment records (masked)
- Invoices

**Activity Data:**
- Login history
- Feature usage logs
- Search history

**Communication Data:**
- Messages with other users
- Support tickets
- Notification history

**Technical Data:**
- Consent records
- Device information
- Session logs

### 7.5 Export Format

```json
{
  "export_metadata": {
    "generated_at": "2026-01-16T10:00:00Z",
    "user_id": "user_123",
    "format_version": "1.0"
  },
  "profile": {
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+91XXXXXXXXXX",
    "business_name": "Example Poultry",
    "created_at": "2025-06-15T08:30:00Z"
  },
  "orders": [
    {
      "order_id": "ORD-001",
      "date": "2025-12-01",
      "amount": 15000,
      "status": "completed"
    }
  ],
  "consents": [
    {
      "type": "marketing_email",
      "status": "granted",
      "date": "2025-06-15"
    }
  ]
}
```

---

## 8. Right to Deletion Process

### 8.1 Right to Erasure (Art. 17)

Data subjects can request deletion when:
- Data is no longer necessary for original purpose
- Consent is withdrawn (and no other legal basis exists)
- Data subject objects and no overriding legitimate grounds exist
- Data was unlawfully processed
- Legal obligation requires erasure
- Data was collected from a child

### 8.2 Exceptions to Erasure

We may retain data when processing is necessary for:
- Exercising freedom of expression
- Legal obligations (tax records, regulatory requirements)
- Public health purposes
- Archiving in public interest
- Establishment, exercise, or defense of legal claims

### 8.3 Deletion Process

#### 8.3.1 Self-Service Deletion

1. User navigates to Settings > Privacy > Delete Account
2. User reviews deletion implications
3. User confirms with password entry
4. 14-day cooling-off period begins
5. User can cancel during cooling-off period
6. After 14 days, deletion process initiates
7. Account anonymized within 30 days
8. Confirmation email sent

#### 8.3.2 Manual Deletion Request

1. Request received via gdpr@poultryplatform.com
2. Identity verification completed
3. Request logged and acknowledged (72 hours)
4. Data audit performed
5. Legal retention requirements assessed
6. Deletion executed or partial deletion with explanation
7. Completion notification sent (within 30 days)

### 8.4 Deletion Scope

| Data Category | Action | Rationale |
|---------------|--------|-----------|
| Profile information | Deleted | No retention requirement |
| Email/Phone | Anonymized | Needed for duplicate prevention |
| Transaction records | Retained (anonymized) | Tax law (8 years) |
| Payment details | Deleted from our systems | Razorpay retains per their policy |
| Messages | Deleted | No retention requirement |
| Usage analytics | Anonymized | Legitimate interest in aggregates |
| Consent records | Retained | Legal compliance proof |
| Audit logs | Retained (anonymized) | Security requirements |

### 8.5 Technical Deletion Process

```
Deletion Pipeline:
1. Mark account for deletion
2. Revoke all active sessions
3. Remove from active databases
4. Delete from caches (Redis)
5. Remove files from S3
6. Request deletion from third parties
7. Anonymize retained records
8. Update audit log
9. Send confirmation
```

### 8.6 Third-Party Notification

We notify the following processors of deletion requests:
- Firebase (user account)
- Razorpay (payment data)
- Email service provider (contact lists)
- Analytics services (user data)

---

## 9. Data Breach Procedures

### 9.1 Definition

A personal data breach is a security incident leading to accidental or unlawful:
- Destruction of personal data
- Loss of personal data
- Alteration of personal data
- Unauthorized disclosure of personal data
- Unauthorized access to personal data

### 9.2 Breach Response Team

| Role | Responsibility |
|------|----------------|
| **Data Protection Officer** | Overall coordination, regulatory notification |
| **Security Team Lead** | Technical investigation, containment |
| **Legal Counsel** | Regulatory compliance, liability assessment |
| **Communications Lead** | User notification, PR management |
| **Engineering Lead** | System remediation |
| **Customer Support Lead** | User inquiry handling |

### 9.3 Breach Response Timeline

| Time | Action |
|------|--------|
| **0-1 hour** | Detect, contain, assemble response team |
| **1-4 hours** | Initial assessment, determine scope |
| **4-24 hours** | Detailed investigation, impact analysis |
| **24-48 hours** | Determine notification requirements |
| **48-72 hours** | Notify supervisory authority (if required) |
| **ASAP** | Notify affected data subjects (if high risk) |
| **Ongoing** | Remediation, documentation, lessons learned |

### 9.4 Supervisory Authority Notification

**When Required:** Breach likely to result in risk to rights and freedoms of natural persons.

**Timeline:** Within 72 hours of becoming aware.

**Content (Art. 33):**
1. Nature of breach (categories and approximate number of subjects affected)
2. Name and contact details of DPO
3. Likely consequences of breach
4. Measures taken or proposed to address breach

**Notification Template:**
```
PERSONAL DATA BREACH NOTIFICATION

To: [Relevant Supervisory Authority]
From: Poultry Platform Private Limited
Date: [Date]
Reference: BREACH-[Year]-[Number]

1. NATURE OF BREACH
   - Type: [Confidentiality/Integrity/Availability]
   - Discovery Date: [Date/Time]
   - Incident Date: [Date/Time if known]

2. DATA SUBJECTS AFFECTED
   - Categories: [Buyers/Sellers/etc.]
   - Approximate Number: [Number]
   - Geographic Scope: [Countries]

3. DATA CATEGORIES AFFECTED
   - [List categories: names, emails, etc.]

4. LIKELY CONSEQUENCES
   - [Description of potential harm]

5. MEASURES TAKEN
   - Containment: [Actions taken]
   - Remediation: [Planned actions]
   - Prevention: [Future measures]

6. CONTACT
   - DPO: [Name]
   - Email: dpo@poultryplatform.com
   - Phone: [Number]
```

### 9.5 Data Subject Notification

**When Required:** Breach likely to result in HIGH risk to rights and freedoms.

**Content (Art. 34):**
1. Nature of breach in clear, plain language
2. Name and contact details of DPO
3. Likely consequences
4. Measures taken and recommended user actions

**Notification Channels:**
- Email (primary)
- In-app notification
- SMS (for high-severity breaches)
- Website banner (if email unavailable)

### 9.6 Breach Documentation

All breaches are documented with:
- Facts surrounding the breach
- Effects of the breach
- Remedial action taken
- Reasoning for decisions made

Documentation retained for 5 years.

### 9.7 Breach Severity Classification

| Level | Criteria | Notification |
|-------|----------|--------------|
| **Low** | No sensitive data, <100 users, contained quickly | Internal only |
| **Medium** | Limited sensitive data, <1000 users | Authority notification likely |
| **High** | Sensitive data, >1000 users, ongoing risk | Authority + user notification |
| **Critical** | Financial data, large scale, active exploitation | Immediate all-party notification |

---

## 10. Data Protection Impact Assessment

### 10.1 When Required

DPIA is required for processing likely to result in high risk, including:
- Systematic and extensive profiling with significant effects
- Large-scale processing of special categories of data
- Systematic monitoring of publicly accessible areas

### 10.2 DPIA Process

1. **Identify Need:** Assess if DPIA is required
2. **Describe Processing:** Document the processing activity
3. **Assess Necessity:** Evaluate if processing is necessary and proportionate
4. **Identify Risks:** Assess risks to individuals' rights and freedoms
5. **Identify Measures:** Determine measures to mitigate risks
6. **Sign Off:** DPO review and approval
7. **Monitor:** Ongoing monitoring and review

### 10.3 DPIA Template

```
DATA PROTECTION IMPACT ASSESSMENT

Project: [Name]
Date: [Date]
Owner: [Name]
DPO Review: [Name, Date]

1. PROCESSING DESCRIPTION
   - Purpose: [Why is data being processed?]
   - Data Categories: [What data is involved?]
   - Data Subjects: [Whose data?]
   - Processing Operations: [What happens to the data?]
   - Recipients: [Who receives the data?]
   - Retention: [How long is data kept?]

2. NECESSITY AND PROPORTIONALITY
   - Legal Basis: [Which Art. 6 basis?]
   - Purpose Limitation: [Is purpose clearly defined?]
   - Data Minimization: [Is only necessary data collected?]
   - Accuracy: [How is accuracy maintained?]
   - Storage Limitation: [Is retention period appropriate?]

3. RISK ASSESSMENT
   [For each identified risk:]
   - Risk Description: [What could go wrong?]
   - Likelihood: [Low/Medium/High]
   - Severity: [Low/Medium/High]
   - Overall Risk: [Low/Medium/High]

4. MITIGATION MEASURES
   [For each risk:]
   - Measure: [What will reduce the risk?]
   - Residual Risk: [Risk level after measure]

5. CONSULTATION
   - DPO Consulted: [Yes/No, Date]
   - Data Subjects Consulted: [If applicable]
   - Supervisory Authority: [If required]

6. DECISION
   - Proceed: [Yes/No]
   - Conditions: [Any conditions]
   - Review Date: [When to reassess]
```

---

## 11. Sub-Processors

### 11.1 Authorized Sub-Processors

| Sub-Processor | Location | Purpose | Data Processed | Safeguards |
|---------------|----------|---------|----------------|------------|
| **Google Cloud Platform** | US/EU | Infrastructure hosting | All platform data | SCCs, SOC 2 |
| **Firebase (Google)** | US | Authentication, analytics, notifications | User IDs, device info | SCCs, Privacy Shield |
| **Razorpay** | India | Payment processing | Payment data | PCI-DSS, contract |
| **Amazon Web Services** | Singapore | File storage | Uploaded files | SCCs, SOC 2 |
| **SendGrid (Twilio)** | US | Email delivery | Email addresses, content | SCCs, Privacy Shield |
| **Sentry** | US | Error tracking | Device info, crash data | SCCs, DPA |

### 11.2 Sub-Processor Agreements

All sub-processors have Data Processing Agreements covering:
- Processing only on documented instructions
- Confidentiality obligations
- Security measures implementation
- Sub-processor restrictions
- Assistance with data subject rights
- Audit rights
- Breach notification requirements
- Data return/deletion upon termination

### 11.3 Sub-Processor Changes

We will:
- Maintain an up-to-date list of sub-processors
- Notify customers of intended changes
- Provide opportunity to object (14 days)
- Not engage objected processors for that customer's data

---

## 12. International Data Transfers

### 12.1 Transfer Mechanisms

| Destination | Mechanism | Documentation |
|-------------|-----------|---------------|
| **United States** | Standard Contractual Clauses (2021) | On file |
| **European Union** | Adequacy | N/A |
| **India** | Controller-Processor contract | On file |

### 12.2 Standard Contractual Clauses

We use the European Commission's SCCs (2021) for:
- Controller to Processor transfers (Module 2)
- Processor to Processor transfers (Module 3)

### 12.3 Supplementary Measures

Where required by Schrems II assessment:
- Encryption of data in transit and at rest
- Access controls and logging
- Contractual commitments from recipients
- Regular security assessments

### 12.4 Transfer Impact Assessment

For each transfer, we assess:
- Nature of data transferred
- Destination country laws
- Effectiveness of safeguards
- Need for supplementary measures

---

## 13. Records of Processing Activities

### 13.1 Controller Records (Art. 30(1))

We maintain records of:
- Name and contact details of controller and DPO
- Purposes of processing
- Categories of data subjects and personal data
- Categories of recipients
- Transfers to third countries and safeguards
- Retention periods
- Security measures description

### 13.2 Processor Records (Art. 30(2))

For processing on behalf of controllers:
- Name and contact details of processor and controller
- Categories of processing carried out
- Transfers to third countries and safeguards
- Security measures description

### 13.3 Record Maintenance

- Records are maintained electronically
- Updated at least quarterly
- Available to supervisory authority on request
- Reviewed annually for accuracy

---

## 14. Contact Information

### 14.1 Data Protection Officer

**Name:** [DPO Name]
**Email:** dpo@poultryplatform.com
**Phone:** [Contact Number]
**Address:** [Address]

### 14.2 Data Controller

**Poultry Platform Private Limited**
**Email:** privacy@poultryplatform.com
**Address:** [Registered Address]

### 14.3 EU Representative (if applicable)

[To be appointed if no establishment in EU]
**Name:** [Representative Name]
**Address:** [EU Address]
**Email:** eu-representative@poultryplatform.com

### 14.4 Supervisory Authority Contacts

**Lead Authority:** [Primary supervisory authority]

**Other Relevant Authorities:**
- Ireland: Data Protection Commission
- Germany: State data protection authorities
- France: CNIL

---

## Appendices

### Appendix A: Consent Form Templates

[Templates for various consent collection points]

### Appendix B: Data Subject Request Forms

[Standard forms for access, deletion, portability requests]

### Appendix C: Data Processing Agreement Template

[Standard DPA for vendors and partners]

### Appendix D: DPIA Register

[List of completed DPIAs]

### Appendix E: Breach Log

[Record of all personal data breaches]

---

*This document is reviewed quarterly and updated as needed to reflect changes in processing activities, legal requirements, or organizational structure.*

**Document Control:**
| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | January 16, 2026 | Legal Team | Initial release |
