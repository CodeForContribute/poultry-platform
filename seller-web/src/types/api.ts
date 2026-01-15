// API Response Types
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  error?: ApiError;
  timestamp: string;
  correlationId?: string;
}

export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, unknown>;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
