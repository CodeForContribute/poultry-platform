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

// CSRF Token Management
let csrfToken: string | null = null;
let csrfTokenPromise: Promise<string | null> | null = null;

// Validate API URL is configured in production
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL;

if (!API_BASE_URL && process.env.NODE_ENV === "production") {
  throw new Error("NEXT_PUBLIC_API_URL environment variable is required in production");
}

const baseUrl = API_BASE_URL || "http://localhost:8080";

export async function fetchCsrfToken(): Promise<string | null> {
  // If already fetching, return the existing promise
  if (csrfTokenPromise) {
    return csrfTokenPromise;
  }

  csrfTokenPromise = (async () => {
    try {
      const response = await axios.get(`${baseUrl}/csrf-token`, {
        withCredentials: true,
      });
      csrfToken = response.data?.token || response.headers["x-csrf-token"] || null;
      return csrfToken;
    } catch (error) {
      console.warn("Failed to fetch CSRF token:", error);
      return null;
    } finally {
      csrfTokenPromise = null;
    }
  })();

  return csrfTokenPromise;
}

export function getCsrfToken(): string | null {
  return csrfToken;
}

export function setCsrfToken(token: string | null): void {
  csrfToken = token;
}

export function clearCsrfToken(): void {
  csrfToken = null;
}

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

// Check if request is a mutating method that needs CSRF protection
function isMutatingMethod(method: string | undefined): boolean {
  const mutatingMethods = ["post", "put", "delete", "patch"];
  return mutatingMethods.includes((method || "").toLowerCase());
}

export const apiClient = axios.create({
  baseURL: `${baseUrl}/v1`,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 30000,
  withCredentials: true,
});

// Request interceptor - add auth token and CSRF token
apiClient.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    // Add auth token
    const accessToken = useAuthStore.getState().accessToken;
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }

    // Add CSRF token for mutating requests
    if (isMutatingMethod(config.method)) {
      // Fetch CSRF token if not available
      if (!csrfToken) {
        await fetchCsrfToken();
      }
      if (csrfToken) {
        config.headers["X-CSRF-Token"] = csrfToken;
      }
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle 401, 403 CSRF errors, and 429 rate limits
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
      _csrfRetry?: boolean;
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

    // Handle CSRF token error (403 with CSRF-related message)
    if (
      error.response?.status === 403 &&
      !originalRequest._csrfRetry &&
      isMutatingMethod(originalRequest.method)
    ) {
      const responseData = error.response.data as { message?: string; error?: string } | undefined;
      const errorMessage = responseData?.message || responseData?.error || "";

      if (
        errorMessage.toLowerCase().includes("csrf") ||
        errorMessage.toLowerCase().includes("token")
      ) {
        originalRequest._csrfRetry = true;

        // Clear and refetch CSRF token
        clearCsrfToken();
        await fetchCsrfToken();

        if (csrfToken) {
          originalRequest.headers["X-CSRF-Token"] = csrfToken;
          return apiClient(originalRequest);
        }
      }
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

// Initialize CSRF token on app startup (call this in your app's entry point)
export async function initializeSecurity(): Promise<void> {
  await fetchCsrfToken();
}

export default apiClient;
