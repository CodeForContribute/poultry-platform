import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import { jwtDecode } from "jwt-decode";
import type { SellerUser, TokenResponse, SellerRole } from "@/types";
import * as authApi from "@/lib/api/auth";

interface AuthState {
  accessToken: string | null;
  refreshTokenValue: string | null;
  user: SellerUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  mustChangePassword: boolean;

  // Actions
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshToken: () => Promise<boolean>;
  setTokens: (response: TokenResponse) => void;
  clearAuth: () => void;
}

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

      login: async (email: string, password: string) => {
        set({ isLoading: true });
        try {
          const response = await authApi.login({ email, password });
          if (response.success && response.data) {
            get().setTokens(response.data);
          } else {
            throw new Error(response.message || "Login failed");
          }
        } finally {
          set({ isLoading: false });
        }
      },

      logout: async () => {
        try {
          await authApi.logout();
        } catch (error) {
          // Ignore logout errors
        } finally {
          get().clearAuth();
        }
      },

      refreshToken: async () => {
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
        });
      },

      clearAuth: () => {
        set({
          accessToken: null,
          refreshTokenValue: null,
          user: null,
          isAuthenticated: false,
          mustChangePassword: false,
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
