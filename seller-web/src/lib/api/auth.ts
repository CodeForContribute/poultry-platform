import apiClient from "./client";
import type {
  ApiResponse,
  SellerLoginRequest,
  TokenResponse,
  RefreshTokenRequest,
  ChangePasswordRequest,
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
