export interface SellerLoginRequest {
  email: string;
  password: string;
  deviceId?: string;
  deviceInfo?: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  expiresAt: string;
  userId: string;
  userType: "SELLER";
  name: string;
  email: string;
  role: SellerRole;
  sellerId: string;
  businessName: string;
  mustChangePassword: boolean;
  newUser: boolean;
  requires2FA?: boolean;
  twoFactorToken?: string;
}

export interface TwoFactorVerifyRequest {
  twoFactorToken: string;
  code: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
  deviceId?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export type SellerRole = "SELLER_ADMIN" | "SELLER_STAFF" | "SELLER_USER";

export interface SellerUser {
  id: string;
  name: string;
  email: string;
  role: SellerRole;
  sellerId: string;
  businessName: string;
}
