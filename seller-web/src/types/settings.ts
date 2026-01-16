// Settings Types
export interface BusinessProfile {
  id: string;
  businessName: string;
  businessNameHi?: string;
  ownerName: string;
  email: string;
  phone: string;
  alternatePhone?: string;
  gstin?: string;
  pan?: string;
  fssaiLicense?: string;
  address: BusinessAddress;
  logo?: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface BusinessAddress {
  line1: string;
  line2?: string;
  city: string;
  state: string;
  pincode: string;
  landmark?: string;
  latitude?: number;
  longitude?: number;
}

export interface UpdateBusinessProfileRequest {
  businessName: string;
  businessNameHi?: string;
  ownerName: string;
  phone: string;
  alternatePhone?: string;
  gstin?: string;
  pan?: string;
  fssaiLicense?: string;
  address: BusinessAddress;
  description?: string;
}

export interface BankAccountDetails {
  id: string;
  accountHolderName: string;
  bankName: string;
  accountNumber: string;
  accountNumberLast4: string;
  ifscCode: string;
  accountType: "SAVINGS" | "CURRENT";
  branch?: string;
  isPrimary: boolean;
  isVerified: boolean;
}

export interface AddBankAccountRequest {
  accountHolderName: string;
  bankName: string;
  accountNumber: string;
  confirmAccountNumber: string;
  ifscCode: string;
  accountType: "SAVINGS" | "CURRENT";
  branch?: string;
}

export interface NotificationPreferences {
  emailNotifications: boolean;
  smsNotifications: boolean;
  pushNotifications: boolean;
  orderAlerts: boolean;
  paymentAlerts: boolean;
  disputeAlerts: boolean;
  promotionalEmails: boolean;
  weeklyReport: boolean;
  monthlyReport: boolean;
}

export interface SecuritySettings {
  twoFactorEnabled: boolean;
  lastPasswordChange: string;
  activeSessions: number;
  loginHistory: LoginHistoryEntry[];
}

export interface LoginHistoryEntry {
  id: string;
  deviceInfo: string;
  ipAddress: string;
  location?: string;
  loginAt: string;
  logoutAt?: string;
  isCurrent: boolean;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}
