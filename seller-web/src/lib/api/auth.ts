import apiClient from "./client";
import type {
  ApiResponse,
  SellerLoginRequest,
  TokenResponse,
  RefreshTokenRequest,
  ChangePasswordRequest,
  TwoFactorVerifyRequest,
} from "@/types";

export async function login(
  credentials: SellerLoginRequest
): Promise<ApiResponse<TokenResponse>> {
  const response = await apiClient.post<ApiResponse<TokenResponse>>(
    "/auth/seller/login",
    credentials
  );
  return response.data;
}

export async function refreshAccessToken(
  request: RefreshTokenRequest
): Promise<ApiResponse<TokenResponse>> {
  const response = await apiClient.post<ApiResponse<TokenResponse>>(
    "/auth/seller/refresh",
    request
  );
  return response.data;
}

export async function changePassword(
  request: ChangePasswordRequest
): Promise<ApiResponse<null>> {
  const response = await apiClient.post<ApiResponse<null>>(
    "/auth/seller/change-password",
    request
  );
  return response.data;
}

export async function logout(): Promise<void> {
  await apiClient.post("/auth/seller/logout");
}

export async function forgotPassword(email: string): Promise<ApiResponse<null>> {
  const response = await apiClient.post<ApiResponse<null>>(
    "/auth/seller/forgot-password",
    { email }
  );
  return response.data;
}

export async function resetPassword(
  token: string,
  newPassword: string
): Promise<ApiResponse<null>> {
  const response = await apiClient.post<ApiResponse<null>>(
    "/auth/seller/reset-password",
    { token, newPassword }
  );
  return response.data;
}

export async function verify2FA(
  request: TwoFactorVerifyRequest
): Promise<ApiResponse<TokenResponse>> {
  const response = await apiClient.post<ApiResponse<TokenResponse>>(
    "/auth/seller/verify-2fa",
    request
  );
  return response.data;
}
