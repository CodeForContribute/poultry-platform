import apiClient from "./client";
import type {
  ApiResponse,
  BusinessProfile,
  UpdateBusinessProfileRequest,
  BankAccountDetails,
  AddBankAccountRequest,
  NotificationPreferences,
  SecuritySettings,
  ChangePasswordRequest,
} from "@/types";

export async function getBusinessProfile(): Promise<ApiResponse<BusinessProfile>> {
  const response = await apiClient.get<ApiResponse<BusinessProfile>>(
    "/seller/profile"
  );
  return response.data;
}

export async function updateBusinessProfile(
  data: UpdateBusinessProfileRequest
): Promise<ApiResponse<BusinessProfile>> {
  const response = await apiClient.put<ApiResponse<BusinessProfile>>(
    "/seller/profile",
    data
  );
  return response.data;
}

export async function getBankAccounts(): Promise<ApiResponse<BankAccountDetails[]>> {
  const response = await apiClient.get<ApiResponse<BankAccountDetails[]>>(
    "/seller/bank-accounts"
  );
  return response.data;
}

export async function addBankAccount(
  data: AddBankAccountRequest
): Promise<ApiResponse<BankAccountDetails>> {
  const response = await apiClient.post<ApiResponse<BankAccountDetails>>(
    "/seller/bank-accounts",
    data
  );
  return response.data;
}

export async function deleteBankAccount(
  accountId: string
): Promise<ApiResponse<void>> {
  const response = await apiClient.delete<ApiResponse<void>>(
    `/seller/bank-accounts/${accountId}`
  );
  return response.data;
}

export async function setPrimaryBankAccount(
  accountId: string
): Promise<ApiResponse<BankAccountDetails>> {
  const response = await apiClient.post<ApiResponse<BankAccountDetails>>(
    `/seller/bank-accounts/${accountId}/set-primary`
  );
  return response.data;
}

export async function getNotificationPreferences(): Promise<
  ApiResponse<NotificationPreferences>
> {
  const response = await apiClient.get<ApiResponse<NotificationPreferences>>(
    "/seller/preferences/notifications"
  );
  return response.data;
}

export async function updateNotificationPreferences(
  data: NotificationPreferences
): Promise<ApiResponse<NotificationPreferences>> {
  const response = await apiClient.put<ApiResponse<NotificationPreferences>>(
    "/seller/preferences/notifications",
    data
  );
  return response.data;
}

export async function getSecuritySettings(): Promise<ApiResponse<SecuritySettings>> {
  const response = await apiClient.get<ApiResponse<SecuritySettings>>(
    "/seller/security"
  );
  return response.data;
}

export async function changePassword(
  data: ChangePasswordRequest
): Promise<ApiResponse<void>> {
  const response = await apiClient.post<ApiResponse<void>>(
    "/seller/security/change-password",
    data
  );
  return response.data;
}

export async function enableTwoFactor(): Promise<ApiResponse<{ qrCode: string; secret: string }>> {
  const response = await apiClient.post<ApiResponse<{ qrCode: string; secret: string }>>(
    "/seller/security/2fa/enable"
  );
  return response.data;
}

export async function disableTwoFactor(
  password: string
): Promise<ApiResponse<void>> {
  const response = await apiClient.post<ApiResponse<void>>(
    "/seller/security/2fa/disable",
    { password }
  );
  return response.data;
}

export async function logoutAllSessions(): Promise<ApiResponse<void>> {
  const response = await apiClient.post<ApiResponse<void>>(
    "/seller/security/logout-all"
  );
  return response.data;
}

// Mock data for development
export function getMockBusinessProfile(): BusinessProfile {
  return {
    id: "seller-1",
    businessName: "Sharma Poultry Farm",
    businessNameHi: "शर्मा पोल्ट्री फार्म",
    ownerName: "Rajesh Sharma",
    email: "rajesh@sharmapoultry.com",
    phone: "9876543210",
    alternatePhone: "9876543211",
    gstin: "29ABCDE1234F1Z5",
    pan: "ABCDE1234F",
    fssaiLicense: "12345678901234",
    address: {
      line1: "Plot No. 45, Industrial Area",
      line2: "Near Railway Station",
      city: "Hyderabad",
      state: "Telangana",
      pincode: "500032",
      landmark: "Opposite SBI Bank",
    },
    description: "Quality poultry products since 2010",
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };
}

export function getMockBankAccounts(): BankAccountDetails[] {
  return [
    {
      id: "bank-1",
      accountHolderName: "Sharma Poultry Farm",
      bankName: "HDFC Bank",
      accountNumber: "XXXXXXXX4523",
      accountNumberLast4: "4523",
      ifscCode: "HDFC0001234",
      accountType: "CURRENT",
      branch: "Jubilee Hills",
      isPrimary: true,
      isVerified: true,
    },
  ];
}

export function getMockNotificationPreferences(): NotificationPreferences {
  return {
    emailNotifications: true,
    smsNotifications: true,
    pushNotifications: true,
    orderAlerts: true,
    paymentAlerts: true,
    disputeAlerts: true,
    promotionalEmails: false,
    weeklyReport: true,
    monthlyReport: true,
  };
}

export function getMockSecuritySettings(): SecuritySettings {
  return {
    twoFactorEnabled: false,
    lastPasswordChange: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString(),
    activeSessions: 2,
    loginHistory: [
      {
        id: "1",
        deviceInfo: "Chrome on Windows",
        ipAddress: "192.168.1.1",
        location: "Hyderabad, India",
        loginAt: new Date().toISOString(),
        isCurrent: true,
      },
      {
        id: "2",
        deviceInfo: "Safari on iPhone",
        ipAddress: "192.168.1.2",
        location: "Hyderabad, India",
        loginAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
        isCurrent: false,
      },
    ],
  };
}
