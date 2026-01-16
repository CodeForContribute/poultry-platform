# App Store Privacy Compliance

**Poultry Platform**

**Last Updated:** January 16, 2026

This document provides the required privacy disclosures for Apple App Store and Google Play Store submissions.

---

## Table of Contents

1. [iOS App Privacy Labels](#1-ios-app-privacy-labels)
2. [Google Play Data Safety](#2-google-play-data-safety)
3. [Required Disclosures](#3-required-disclosures)
4. [SDK Privacy Documentation](#4-sdk-privacy-documentation)

---

## 1. iOS App Privacy Labels

### 1.1 Data Types Collected

Use these responses for the App Store Connect privacy questionnaire:

#### Contact Info

| Data Type | Collected | Usage | Linked to User | Tracking |
|-----------|-----------|-------|----------------|----------|
| **Name** | Yes | App Functionality | Yes | No |
| **Email Address** | Yes | App Functionality, Analytics | Yes | No |
| **Phone Number** | Yes | App Functionality | Yes | No |
| **Physical Address** | Yes | App Functionality | Yes | No |

#### Financial Info

| Data Type | Collected | Usage | Linked to User | Tracking |
|-----------|-----------|-------|----------------|----------|
| **Payment Info** | Yes | App Functionality | Yes | No |
| **Other Financial Info** | Yes | App Functionality | Yes | No |

*Note: Bank account details are collected for seller payouts only.*

#### Location

| Data Type | Collected | Usage | Linked to User | Tracking |
|-----------|-----------|-------|----------------|----------|
| **Precise Location** | Yes (with permission) | App Functionality | Yes | No |
| **Coarse Location** | Yes | App Functionality | Yes | No |

*Note: Location is used for delivery services and showing nearby sellers.*

#### Identifiers

| Data Type | Collected | Usage | Linked to User | Tracking |
|-----------|-----------|-------|----------------|----------|
| **User ID** | Yes | App Functionality, Analytics | Yes | No |
| **Device ID** | Yes | App Functionality, Analytics | Yes | No |

#### Purchases

| Data Type | Collected | Usage | Linked to User | Tracking |
|-----------|-----------|-------|----------------|----------|
| **Purchase History** | Yes | App Functionality | Yes | No |

#### Usage Data

| Data Type | Collected | Usage | Linked to User | Tracking |
|-----------|-----------|-------|----------------|----------|
| **Product Interaction** | Yes | Analytics | Yes | No |
| **Advertising Data** | No | - | - | - |
| **Other Usage Data** | Yes | Analytics | Yes | No |

#### Diagnostics

| Data Type | Collected | Usage | Linked to User | Tracking |
|-----------|-----------|-------|----------------|----------|
| **Crash Data** | Yes | App Functionality | Yes | No |
| **Performance Data** | Yes | App Functionality | Yes | No |
| **Other Diagnostic Data** | Yes | App Functionality | Yes | No |

#### Other Data

| Data Type | Collected | Usage | Linked to User | Tracking |
|-----------|-----------|-------|----------------|----------|
| **Business Information** | Yes | App Functionality | Yes | No |

*Note: Includes GST number, business name, FSSAI license.*

### 1.2 Data Use Descriptions

#### App Functionality
- Account creation and authentication
- Processing orders and payments
- Facilitating communication between buyers and sellers
- Delivery tracking and coordination
- Customer support

#### Analytics
- Understanding app usage patterns
- Improving app features and performance
- Debugging and error resolution

### 1.3 Data Linked to User
All collected data is linked to user accounts for functionality purposes.

### 1.4 Tracking
We do NOT track users across third-party apps or websites for advertising purposes.

### 1.5 Privacy Nutrition Label Summary

```
DATA USED TO TRACK YOU
- None

DATA LINKED TO YOU
- Contact Info (Name, Email, Phone, Address)
- Financial Info (Payment Info, Bank Details)
- Location (Precise, Coarse)
- Identifiers (User ID, Device ID)
- Purchases (Purchase History)
- Usage Data (Product Interaction)
- Diagnostics (Crash Data, Performance Data)

DATA NOT LINKED TO YOU
- None
```

---

## 2. Google Play Data Safety

### 2.1 Data Collection and Sharing

Use these responses for the Google Play Console Data Safety form:

#### Does your app collect or share any of the required user data types?
**Yes**

#### Is all of the user data collected by your app encrypted in transit?
**Yes** - All data transmitted using TLS 1.2 or higher

#### Do you provide a way for users to request that their data be deleted?
**Yes** - Users can request deletion via app settings or by contacting support

### 2.2 Data Types

#### Location

| Data Type | Collected | Shared | Purpose | Optional |
|-----------|-----------|--------|---------|----------|
| **Approximate location** | Yes | No | App functionality | No |
| **Precise location** | Yes | No | App functionality | Yes |

*Purpose:* Delivery services and showing nearby sellers

#### Personal Info

| Data Type | Collected | Shared | Purpose | Optional |
|-----------|-----------|--------|---------|----------|
| **Name** | Yes | Yes (with sellers) | App functionality | No |
| **Email address** | Yes | No | App functionality, Account management | No |
| **Phone number** | Yes | Yes (with sellers) | App functionality | No |
| **Address** | Yes | Yes (with sellers) | App functionality | No |

*Purpose:* Account management, order fulfillment, customer support

#### Financial Info

| Data Type | Collected | Shared | Purpose | Optional |
|-----------|-----------|--------|---------|----------|
| **User payment info** | Yes | Yes (Razorpay) | App functionality | No |
| **Other financial info** | Yes | No | App functionality | No |

*Purpose:* Processing payments, seller payouts

#### App Activity

| Data Type | Collected | Shared | Purpose | Optional |
|-----------|-----------|--------|---------|----------|
| **App interactions** | Yes | No | Analytics | No |
| **In-app search history** | Yes | No | Analytics, Personalization | No |
| **Other user-generated content** | Yes | Yes (with sellers) | App functionality | No |

*Purpose:* Improving app experience, order processing

#### App Info and Performance

| Data Type | Collected | Shared | Purpose | Optional |
|-----------|-----------|--------|---------|----------|
| **Crash logs** | Yes | No | App functionality | No |
| **Diagnostics** | Yes | No | App functionality | No |
| **Other performance data** | Yes | No | App functionality | No |

*Purpose:* Debugging, performance improvement

#### Device or Other IDs

| Data Type | Collected | Shared | Purpose | Optional |
|-----------|-----------|--------|---------|----------|
| **Device or other IDs** | Yes | No | Analytics, App functionality | No |

*Purpose:* Device identification, push notifications

### 2.3 Data Handling Practices

#### Data Encryption
- **In Transit:** Yes, using TLS 1.2+
- **At Rest:** Yes, using AES-256 encryption

#### Data Deletion
- Users can request data deletion through:
  - App Settings > Privacy > Delete My Data
  - Email: privacy@poultryplatform.com
- Deletion completed within 30 days

#### Security Practices
- Regular security audits
- Penetration testing
- Bug bounty program
- SOC 2 compliance (planned)

### 2.4 Data Safety Section Summary

```
DATA SHARED WITH THIRD PARTIES
- Name, Phone, Address (with sellers for order fulfillment)
- Payment info (with Razorpay for payment processing)

DATA COLLECTED
- Location (approximate and precise)
- Personal info (name, email, phone, address)
- Financial info (payment info, bank details)
- App activity (interactions, search history)
- App info (crash logs, diagnostics)
- Device identifiers

SECURITY PRACTICES
- Data is encrypted in transit
- You can request that data be deleted
- Data is encrypted at rest
```

---

## 3. Required Disclosures

### 3.1 Purpose of Data Collection

| Data Category | Purpose | Necessity |
|---------------|---------|-----------|
| **Account Data** | User identification, authentication | Essential for service |
| **Contact Data** | Communication, support, delivery | Essential for service |
| **Payment Data** | Transaction processing | Essential for service |
| **Location Data** | Delivery, nearby sellers | Core functionality |
| **Usage Data** | Analytics, improvement | Service optimization |
| **Diagnostic Data** | Bug fixing, stability | Service maintenance |

### 3.2 Data Retention Periods

| Data Type | Retention Period | Reason |
|-----------|------------------|--------|
| Account information | Account lifetime + 7 years | Tax/legal compliance |
| Transaction records | 8 years | Indian tax law requirements |
| Payment information | 8 years | RBI regulations |
| Location data | 90 days | Service optimization |
| Usage analytics | 2 years | Product improvement |
| Crash logs | 1 year | Bug resolution |

### 3.3 Third-Party SDKs and Services

| SDK/Service | Data Accessed | Purpose |
|-------------|---------------|---------|
| **Firebase Auth** | User credentials, email | Authentication |
| **Firebase Analytics** | Usage data, device info | Analytics |
| **Firebase Crashlytics** | Crash data, device info | Crash reporting |
| **Firebase Cloud Messaging** | Device tokens | Push notifications |
| **Razorpay** | Payment info | Payment processing |
| **Google Maps** | Location | Maps, directions |

### 3.4 Permissions Required

#### iOS Permissions

| Permission | Usage Description | When Requested |
|------------|-------------------|----------------|
| **Location (When In Use)** | To show nearby sellers and enable delivery tracking | When searching for sellers |
| **Location (Always)** | Not requested | - |
| **Camera** | To capture product photos for listings (sellers) | When adding products |
| **Photo Library** | To select product images (sellers) | When adding products |
| **Notifications** | To send order updates and alerts | At app launch |
| **Contacts** | Not requested | - |

#### Android Permissions

| Permission | Usage Description | When Requested |
|------------|-------------------|----------------|
| **ACCESS_FINE_LOCATION** | To show nearby sellers and enable delivery tracking | When searching for sellers |
| **ACCESS_COARSE_LOCATION** | To show approximate location for delivery | At app launch |
| **CAMERA** | To capture product photos for listings | When adding products |
| **READ_EXTERNAL_STORAGE** | To select product images | When adding products |
| **POST_NOTIFICATIONS** | To send order updates and alerts | At app launch |
| **INTERNET** | To connect to our servers | Automatic |

---

## 4. SDK Privacy Documentation

### 4.1 Firebase (Google)

**Data Collected:**
- Device identifiers (Instance ID, App Instance ID)
- IP address (logged temporarily)
- Usage analytics events
- Crash reports with device information

**Data Retention:**
- Analytics: 14 months (default), configurable
- Crash reports: 90 days

**Privacy Policy:** https://firebase.google.com/support/privacy

**Configuration:**
- Analytics data collection can be disabled
- Crash reporting can be disabled
- IP anonymization enabled

### 4.2 Razorpay

**Data Collected:**
- Payment method details
- Transaction information
- Device fingerprint (fraud prevention)
- IP address

**Data Handling:**
- PCI-DSS Level 1 compliant
- Data encrypted in transit and at rest
- Card details not stored on our servers

**Privacy Policy:** https://razorpay.com/privacy/

### 4.3 Google Maps SDK

**Data Collected:**
- Device location (when permitted)
- Map interaction data

**Privacy Policy:** https://policies.google.com/privacy

### 4.4 Apple Services (iOS)

**Data Collected:**
- Apple Push Notification tokens
- Sign in with Apple data (if enabled)

**Privacy Policy:** https://www.apple.com/legal/privacy/

---

## 5. Compliance Checklist

### 5.1 iOS App Store Submission

- [ ] Privacy Policy URL added to App Store Connect
- [ ] App Privacy Labels completed accurately
- [ ] Privacy manifest (PrivacyInfo.xcprivacy) included
- [ ] Required reason APIs declared
- [ ] Tracking Transparency implemented (if applicable)
- [ ] Purpose strings added for all permissions

### 5.2 Google Play Submission

- [ ] Privacy Policy URL added to store listing
- [ ] Data Safety form completed
- [ ] Permissions justified in app listing
- [ ] Target API level compliance
- [ ] Data deletion mechanism implemented
- [ ] Families Policy compliance (if applicable) - N/A

### 5.3 Both Platforms

- [ ] Privacy Policy accessible in-app
- [ ] Terms of Service accessible in-app
- [ ] Data export feature implemented
- [ ] Account deletion feature implemented
- [ ] Age gate implemented (18+)
- [ ] Consent mechanisms for data collection

---

## 6. Updates and Maintenance

### 6.1 When to Update This Document

- Adding new data collection
- Integrating new third-party SDKs
- Changing data retention periods
- Modifying data sharing practices
- App store policy changes

### 6.2 Review Schedule

- **Quarterly:** Review for accuracy
- **Annually:** Comprehensive audit
- **Ad-hoc:** After any significant changes

### 6.3 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | January 16, 2026 | Initial release |

---

*This document should be reviewed by legal counsel and updated as app functionality or privacy regulations change.*
