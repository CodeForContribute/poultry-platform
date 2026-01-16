/**
 * Comprehensive error handling module for the seller web application.
 * Provides error types, codes, and user-friendly message mapping.
 */

// MARK: - Error Codes

export const ErrorCode = {
  // Network errors (1xxx)
  NO_CONNECTION: 1001,
  TIMEOUT: 1002,
  SSL_ERROR: 1003,

  // Server errors (5xxx)
  SERVER_ERROR: 500,
  SERVICE_UNAVAILABLE: 503,
  GATEWAY_TIMEOUT: 504,

  // Client errors (4xxx)
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  VALIDATION_ERROR: 422,
  RATE_LIMITED: 429,

  // Business errors (2xxx)
  INSUFFICIENT_STOCK: 2001,
  PAYMENT_FAILED: 2002,
  ORDER_CANCELLED: 2003,
  INVALID_COUPON: 2004,
  MINIMUM_ORDER_NOT_MET: 2005,

  // Unknown
  UNKNOWN: 9999,
} as const;

export type ErrorCodeType = (typeof ErrorCode)[keyof typeof ErrorCode];

// MARK: - Error Types

export type ErrorCategory =
  | "network"
  | "server"
  | "authentication"
  | "validation"
  | "business"
  | "unknown";

export interface AppErrorInfo {
  code: ErrorCodeType;
  title: string;
  message: string;
  category: ErrorCategory;
  isRecoverable: boolean;
  requiresAuth: boolean;
  originalError?: Error;
  fieldErrors?: Record<string, string>;
  retryAfter?: number;
}

// MARK: - App Error Class

/**
 * Custom error class for application errors with detailed information.
 */
export class AppError extends Error {
  public readonly code: ErrorCodeType;
  public readonly title: string;
  public readonly category: ErrorCategory;
  public readonly isRecoverable: boolean;
  public readonly requiresAuth: boolean;
  public readonly originalError?: Error;
  public readonly fieldErrors?: Record<string, string>;
  public readonly retryAfter?: number;

  constructor(info: AppErrorInfo) {
    super(info.message);
    this.name = "AppError";
    this.code = info.code;
    this.title = info.title;
    this.category = info.category;
    this.isRecoverable = info.isRecoverable;
    this.requiresAuth = info.requiresAuth;
    this.originalError = info.originalError;
    this.fieldErrors = info.fieldErrors;
    this.retryAfter = info.retryAfter;

    // Maintains proper stack trace for where error was thrown
    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, AppError);
    }
  }

  /**
   * Create error info object for serialization
   */
  toInfo(): AppErrorInfo {
    return {
      code: this.code,
      title: this.title,
      message: this.message,
      category: this.category,
      isRecoverable: this.isRecoverable,
      requiresAuth: this.requiresAuth,
      fieldErrors: this.fieldErrors,
      retryAfter: this.retryAfter,
    };
  }
}

// MARK: - Error Factory Functions

/**
 * Creates a network connection error
 */
export function createNetworkError(originalError?: Error): AppError {
  return new AppError({
    code: ErrorCode.NO_CONNECTION,
    title: "No Connection",
    message: "Unable to connect to the server. Please check your internet connection.",
    category: "network",
    isRecoverable: true,
    requiresAuth: false,
    originalError,
  });
}

/**
 * Creates a timeout error
 */
export function createTimeoutError(originalError?: Error): AppError {
  return new AppError({
    code: ErrorCode.TIMEOUT,
    title: "Request Timeout",
    message: "The request took too long. Please try again.",
    category: "network",
    isRecoverable: true,
    requiresAuth: false,
    originalError,
  });
}

/**
 * Creates a server error
 */
export function createServerError(
  statusCode: number,
  serverMessage?: string,
  originalError?: Error
): AppError {
  return new AppError({
    code: statusCode as ErrorCodeType,
    title: "Server Error",
    message: serverMessage || "Something went wrong on our end. Please try again later.",
    category: "server",
    isRecoverable: true,
    requiresAuth: false,
    originalError,
  });
}

/**
 * Creates an unauthorized error
 */
export function createUnauthorizedError(serverMessage?: string, originalError?: Error): AppError {
  return new AppError({
    code: ErrorCode.UNAUTHORIZED,
    title: "Session Expired",
    message: serverMessage || "Your session has expired. Please login again.",
    category: "authentication",
    isRecoverable: false,
    requiresAuth: true,
    originalError,
  });
}

/**
 * Creates a forbidden error
 */
export function createForbiddenError(serverMessage?: string, originalError?: Error): AppError {
  return new AppError({
    code: ErrorCode.FORBIDDEN,
    title: "Access Denied",
    message: serverMessage || "You don't have permission to perform this action.",
    category: "authentication",
    isRecoverable: false,
    requiresAuth: false,
    originalError,
  });
}

/**
 * Creates a not found error
 */
export function createNotFoundError(serverMessage?: string, originalError?: Error): AppError {
  return new AppError({
    code: ErrorCode.NOT_FOUND,
    title: "Not Found",
    message: serverMessage || "The requested resource was not found.",
    category: "unknown",
    isRecoverable: false,
    requiresAuth: false,
    originalError,
  });
}

/**
 * Creates a validation error
 */
export function createValidationError(
  serverMessage?: string,
  fieldErrors?: Record<string, string>,
  originalError?: Error
): AppError {
  return new AppError({
    code: ErrorCode.VALIDATION_ERROR,
    title: "Invalid Input",
    message: serverMessage || "Please check your input and try again.",
    category: "validation",
    isRecoverable: false,
    requiresAuth: false,
    fieldErrors,
    originalError,
  });
}

/**
 * Creates a rate limited error
 */
export function createRateLimitedError(retryAfter?: number, originalError?: Error): AppError {
  const message = retryAfter
    ? `Too many requests. Please wait ${retryAfter} seconds and try again.`
    : "Too many requests. Please wait and try again.";

  return new AppError({
    code: ErrorCode.RATE_LIMITED,
    title: "Too Many Requests",
    message,
    category: "network",
    isRecoverable: true,
    requiresAuth: false,
    retryAfter,
    originalError,
  });
}

/**
 * Creates an unknown error
 */
export function createUnknownError(originalError?: Error): AppError {
  return new AppError({
    code: ErrorCode.UNKNOWN,
    title: "Error",
    message: originalError?.message || "An unexpected error occurred. Please try again.",
    category: "unknown",
    isRecoverable: true,
    requiresAuth: false,
    originalError,
  });
}

// MARK: - Error Message Mapping

const errorMessages: Record<ErrorCodeType, { title: string; message: string }> = {
  [ErrorCode.NO_CONNECTION]: {
    title: "No Connection",
    message: "Unable to connect to the server. Please check your internet connection.",
  },
  [ErrorCode.TIMEOUT]: {
    title: "Request Timeout",
    message: "The request took too long. Please try again.",
  },
  [ErrorCode.SSL_ERROR]: {
    title: "Security Error",
    message: "Unable to establish a secure connection.",
  },
  [ErrorCode.SERVER_ERROR]: {
    title: "Server Error",
    message: "Something went wrong on our end. Please try again later.",
  },
  [ErrorCode.SERVICE_UNAVAILABLE]: {
    title: "Service Unavailable",
    message: "The service is temporarily unavailable. Please try again later.",
  },
  [ErrorCode.GATEWAY_TIMEOUT]: {
    title: "Gateway Timeout",
    message: "The server took too long to respond. Please try again.",
  },
  [ErrorCode.BAD_REQUEST]: {
    title: "Bad Request",
    message: "The request was invalid. Please check your input.",
  },
  [ErrorCode.UNAUTHORIZED]: {
    title: "Session Expired",
    message: "Your session has expired. Please login again.",
  },
  [ErrorCode.FORBIDDEN]: {
    title: "Access Denied",
    message: "You don't have permission to perform this action.",
  },
  [ErrorCode.NOT_FOUND]: {
    title: "Not Found",
    message: "The requested resource was not found.",
  },
  [ErrorCode.VALIDATION_ERROR]: {
    title: "Validation Error",
    message: "Please check your input and try again.",
  },
  [ErrorCode.RATE_LIMITED]: {
    title: "Too Many Requests",
    message: "You've made too many requests. Please wait and try again.",
  },
  [ErrorCode.INSUFFICIENT_STOCK]: {
    title: "Out of Stock",
    message: "This product is currently out of stock.",
  },
  [ErrorCode.PAYMENT_FAILED]: {
    title: "Payment Failed",
    message: "Payment could not be processed. Please try again.",
  },
  [ErrorCode.ORDER_CANCELLED]: {
    title: "Order Cancelled",
    message: "This order has been cancelled.",
  },
  [ErrorCode.INVALID_COUPON]: {
    title: "Invalid Coupon",
    message: "This coupon code is invalid or has expired.",
  },
  [ErrorCode.MINIMUM_ORDER_NOT_MET]: {
    title: "Minimum Not Met",
    message: "Minimum order amount has not been met.",
  },
  [ErrorCode.UNKNOWN]: {
    title: "Error",
    message: "An unexpected error occurred. Please try again.",
  },
};

/**
 * Gets user-friendly error message for an error code
 */
export function getErrorMessage(code: ErrorCodeType): { title: string; message: string } {
  return errorMessages[code] || errorMessages[ErrorCode.UNKNOWN];
}

// MARK: - Error Parsing

/**
 * Parse error from axios or fetch response
 */
export function parseApiError(error: unknown): AppError {
  // Handle axios errors
  if (isAxiosError(error)) {
    const status = error.response?.status;
    const data = error.response?.data as { message?: string; errors?: Record<string, string> } | undefined;
    const message = data?.message;
    const fieldErrors = data?.errors;

    // Network errors
    if (error.code === "ECONNABORTED") {
      return createTimeoutError(error);
    }
    if (error.code === "ERR_NETWORK" || !error.response) {
      return createNetworkError(error);
    }

    // HTTP errors
    switch (status) {
      case 400:
      case 422:
        return createValidationError(message, fieldErrors, error);
      case 401:
        return createUnauthorizedError(message, error);
      case 403:
        return createForbiddenError(message, error);
      case 404:
        return createNotFoundError(message, error);
      case 429:
        const retryAfter = parseInt(error.response?.headers?.["retry-after"] || "0", 10);
        return createRateLimitedError(retryAfter || undefined, error);
      case 500:
      case 502:
      case 503:
      case 504:
        return createServerError(status, message, error);
      default:
        return createUnknownError(error);
    }
  }

  // Handle AppError
  if (error instanceof AppError) {
    return error;
  }

  // Handle generic errors
  if (error instanceof Error) {
    return createUnknownError(error);
  }

  return createUnknownError();
}

// MARK: - Type Guards

interface AxiosErrorLike {
  isAxiosError: boolean;
  code?: string;
  response?: {
    status?: number;
    data?: unknown;
    headers?: Record<string, string>;
  };
}

function isAxiosError(error: unknown): error is AxiosErrorLike & Error {
  return (
    typeof error === "object" &&
    error !== null &&
    "isAxiosError" in error &&
    (error as AxiosErrorLike).isAxiosError === true
  );
}

// MARK: - Utility Functions

/**
 * Check if an error is an AppError
 */
export function isAppError(error: unknown): error is AppError {
  return error instanceof AppError;
}

/**
 * Get error category color for UI
 */
export function getErrorCategoryColor(category: ErrorCategory): string {
  switch (category) {
    case "network":
      return "orange";
    case "server":
      return "red";
    case "authentication":
      return "yellow";
    case "validation":
      return "orange";
    case "business":
      return "blue";
    case "unknown":
      return "red";
  }
}

/**
 * Get error icon name for UI
 */
export function getErrorIcon(category: ErrorCategory): string {
  switch (category) {
    case "network":
      return "wifi-off";
    case "server":
      return "server";
    case "authentication":
      return "lock";
    case "validation":
      return "alert-triangle";
    case "business":
      return "info";
    case "unknown":
      return "alert-circle";
  }
}
