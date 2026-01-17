import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import { jwtDecode } from "jwt-decode";
import type { SellerUser, TokenResponse, SellerRole } from "@/types";
import * as authApi from "@/lib/api/auth";

interface TwoFactorResult {
  requires2FA: boolean;
  twoFactorToken: string;
}

interface AuthState {
  accessToken: string | null;
  refreshTokenValue: string | null;
  user: SellerUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  mustChangePassword: boolean;
  isDemoMode: boolean;

  // Actions
  login: (email: string, password: string) => Promise<TwoFactorResult | void>;
  verify2FA: (twoFactorToken: string, code: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshToken: () => Promise<boolean>;
  setTokens: (response: TokenResponse) => void;
  setDemoAuth: () => void;
  clearAuth: () => void;
}

// Demo user data for testing purposes
const DEMO_USER: SellerUser = {
  id: "demo-user-id",
  name: "Demo Seller",
  email: "demo@example.com",
  role: "SELLER_ADMIN" as SellerRole,
  sellerId: "demo-seller-123",
  businessName: "Demo Poultry Farm",
};

interface JwtPayload {
  sub: string;
  exp: number;
  iat: number;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshTokenValue: null,
      user: null,
      isAuthenticated: false,
      isLoading: false,
      mustChangePassword: false,
      isDemoMode: false,

      login: async (email: string, password: string) => {
        set({ isLoading: true });
        try {
          const response = await authApi.login({ email, password });
          if (response.success && response.data) {
            // Check if 2FA is required
            if (response.data.requires2FA && response.data.twoFactorToken) {
              return {
                requires2FA: true,
                twoFactorToken: response.data.twoFactorToken,
              };
            }
            get().setTokens(response.data);
          } else {
            throw new Error(response.message || "Login failed");
          }
        } finally {
          set({ isLoading: false });
        }
      },

      verify2FA: async (twoFactorToken: string, code: string) => {
        set({ isLoading: true });
        try {
          const response = await authApi.verify2FA({ twoFactorToken, code });
          if (response.success && response.data) {
            get().setTokens(response.data);
          } else {
            throw new Error(response.message || "2FA verification failed");
          }
        } finally {
          set({ isLoading: false });
        }
      },

      logout: async () => {
        const isDemoMode = get().isDemoMode;

        // Skip API call for demo mode
        if (!isDemoMode) {
          try {
            await authApi.logout();
          } catch (error) {
            // Ignore logout errors
          }
        }

        get().clearAuth();
      },

      refreshToken: async () => {
        // Demo mode doesn't need token refresh
        if (get().isDemoMode) {
          return true;
        }

        const refreshTokenValue = get().refreshTokenValue;
        if (!refreshTokenValue) {
          get().clearAuth();
          return false;
        }

        try {
          const response = await authApi.refreshAccessToken({
            refreshToken: refreshTokenValue,
          });

          if (response.success && response.data) {
            get().setTokens(response.data);
            return true;
          }
        } catch (error) {
          get().clearAuth();
        }
        return false;
      },

      setTokens: (response: TokenResponse) => {
        const user: SellerUser = {
          id: response.userId,
          name: response.name,
          email: response.email,
          role: response.role,
          sellerId: response.sellerId,
          businessName: response.businessName,
        };

        set({
          accessToken: response.accessToken,
          refreshTokenValue: response.refreshToken,
          user,
          isAuthenticated: true,
          mustChangePassword: response.mustChangePassword,
          isDemoMode: false,
        });
      },

      setDemoAuth: () => {
        set({
          accessToken: "demo-access-token",
          refreshTokenValue: "demo-refresh-token",
          user: DEMO_USER,
          isAuthenticated: true,
          mustChangePassword: false,
          isDemoMode: true,
        });
      },

      clearAuth: () => {
        set({
          accessToken: null,
          refreshTokenValue: null,
          user: null,
          isAuthenticated: false,
          mustChangePassword: false,
          isDemoMode: false,
        });
      },
    }),
    {
      name: "seller-auth",
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshTokenValue: state.refreshTokenValue,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
        mustChangePassword: state.mustChangePassword,
        isDemoMode: state.isDemoMode,
      }),
    }
  )
);

// Helper hook to check if token is expired
export function isTokenExpired(token: string): boolean {
  try {
    const decoded = jwtDecode<JwtPayload>(token);
    return decoded.exp * 1000 < Date.now();
  } catch {
    return true;
  }
}
