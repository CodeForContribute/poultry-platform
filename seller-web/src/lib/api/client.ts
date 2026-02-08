import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import { useAuthStore } from "@/lib/store/auth-store";
import { createRateLimitedError, type AppError } from "@/lib/errors";

// Global error handler callback
type GlobalErrorHandler = (error: AppError) => void;
let globalErrorHandler: GlobalErrorHandler | null = null;

export function setGlobalErrorHandler(handler: GlobalErrorHandler): void {
  globalErrorHandler = handler;
}

export function clearGlobalErrorHandler(): void {
  globalErrorHandler = null;
}

export function getGlobalErrorHandler(): GlobalErrorHandler | null {
  return globalErrorHandler;
}

// Validate API URL is configured in production
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL;

if (!API_BASE_URL && process.env.NODE_ENV === "production") {
  throw new Error("NEXT_PUBLIC_API_URL environment variable is required in production");
}

const baseUrl = API_BASE_URL || "http://localhost:8080";

// Rate limit event emitter for UI notifications
type RateLimitHandler = (retryAfter: number) => void;
let rateLimitHandler: RateLimitHandler | null = null;

export function setRateLimitHandler(handler: RateLimitHandler): void {
  rateLimitHandler = handler;
}

export function clearRateLimitHandler(): void {
  rateLimitHandler = null;
}

export function getRateLimitHandler(): RateLimitHandler | null {
  return rateLimitHandler;
}

export const apiClient = axios.create({
  baseURL: `${baseUrl}/v1`,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 30000,
  withCredentials: true,
});

// Request interceptor - add auth token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const accessToken = useAuthStore.getState().accessToken;
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle 401 and 429 rate limits
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    // Handle rate limit (429)
    if (error.response?.status === 429) {
      const retryAfter = parseInt(
        error.response.headers["retry-after"] as string || "60",
        10
      );

      // Notify UI about rate limit
      if (rateLimitHandler) {
        rateLimitHandler(retryAfter);
      }

      // Create a proper rate limited error
      const rateLimitError = createRateLimitedError(retryAfter, error);
      return Promise.reject(rateLimitError);
    }

    // Handle 401 - unauthorized
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshed = await useAuthStore.getState().refreshToken();
        if (refreshed) {
          // Retry original request with new token
          const newToken = useAuthStore.getState().accessToken;
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          return apiClient(originalRequest);
        }
      } catch (refreshError) {
        // Refresh failed, logout
        useAuthStore.getState().logout();
        if (typeof window !== "undefined") {
          window.location.href = "/login";
        }
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
