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

export async function setup2FA(): Promise<ApiResponse<{ qrCode: string; secret: string }>> {
  const response = await apiClient.get<ApiResponse<{ qrCode: string; secret: string }>>(
    "/seller/security/2fa/setup"
  );
  return response.data;
}

export async function verify2FA(code: string): Promise<ApiResponse<void>> {
  const response = await apiClient.post<ApiResponse<void>>(
    "/seller/security/2fa/verify",
    { code }
  );
  return response.data;
}

export async function getBackupCodes(): Promise<ApiResponse<{ codes: string[] }>> {
  const response = await apiClient.get<ApiResponse<{ codes: string[] }>>(
    "/seller/security/2fa/backup-codes"
  );
  return response.data;
}

export async function logoutAllSessions(): Promise<ApiResponse<void>> {
  const response = await apiClient.post<ApiResponse<void>>(
    "/seller/security/logout-all"
  );
  return response.data;
}

