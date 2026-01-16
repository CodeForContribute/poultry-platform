"use client";

import { useCallback, useEffect, useState } from "react";
import { AppError, parseApiError } from "@/lib/errors";
import { setGlobalErrorHandler, clearGlobalErrorHandler } from "@/lib/api/client";

// MARK: - Types

interface ErrorState {
  error: AppError | null;
  isVisible: boolean;
}

interface UseErrorHandlerOptions {
  /** Duration in ms before auto-dismissing the error. 0 means no auto-dismiss */
  autoDismissDelay?: number;
  /** Callback when error requires authentication */
  onAuthRequired?: () => void;
  /** Callback when error occurs */
  onError?: (error: AppError) => void;
}

interface UseErrorHandlerReturn {
  /** Current error state */
  error: AppError | null;
  /** Whether the error is visible */
  isVisible: boolean;
  /** Show an error */
  showError: (error: AppError | Error | string) => void;
  /** Clear the current error */
  clearError: () => void;
  /** Handle an error from an async operation */
  handleError: (error: unknown) => AppError;
}

// MARK: - Hook Implementation

/**
 * Hook for managing error state in components.
 * Provides error display state and methods for showing/clearing errors.
 */
export function useErrorHandler(options: UseErrorHandlerOptions = {}): UseErrorHandlerReturn {
  const { autoDismissDelay = 5000, onAuthRequired, onError } = options;

  const [errorState, setErrorState] = useState<ErrorState>({
    error: null,
    isVisible: false,
  });

  // Auto-dismiss timer
  useEffect(() => {
    if (errorState.isVisible && autoDismissDelay > 0) {
      const timer = setTimeout(() => {
        setErrorState((prev) => ({ ...prev, isVisible: false }));
      }, autoDismissDelay);

      return () => clearTimeout(timer);
    }
  }, [errorState.isVisible, autoDismissDelay]);

  const showError = useCallback(
    (error: AppError | Error | string) => {
      let appError: AppError;

      if (error instanceof AppError) {
        appError = error;
      } else if (error instanceof Error) {
        appError = parseApiError(error);
      } else {
        appError = parseApiError(new Error(error));
      }

      setErrorState({ error: appError, isVisible: true });
      onError?.(appError);

      // Handle auth required errors
      if (appError.requiresAuth && onAuthRequired) {
        onAuthRequired();
      }
    },
    [onError, onAuthRequired]
  );

  const clearError = useCallback(() => {
    setErrorState({ error: null, isVisible: false });
  }, []);

  const handleError = useCallback(
    (error: unknown): AppError => {
      const appError = parseApiError(error);
      showError(appError);
      return appError;
    },
    [showError]
  );

  return {
    error: errorState.error,
    isVisible: errorState.isVisible,
    showError,
    clearError,
    handleError,
  };
}

// MARK: - Global Error Handler Hook

/**
 * Hook that sets up the global error handler for API calls.
 * Should be used at the app root level.
 */
export function useGlobalErrorHandler(
  handler: (error: AppError) => void,
  deps: React.DependencyList = []
): void {
  useEffect(() => {
    setGlobalErrorHandler(handler);
    return () => clearGlobalErrorHandler();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
}

// MARK: - Async Error Handler

interface AsyncState<T> {
  data: T | null;
  error: AppError | null;
  isLoading: boolean;
}

interface UseAsyncOptions {
  /** Initial data value */
  initialData?: unknown;
  /** Whether to run immediately */
  immediate?: boolean;
  /** Callback on success */
  onSuccess?: (data: unknown) => void;
  /** Callback on error */
  onError?: (error: AppError) => void;
}

interface UseAsyncReturn<T> {
  data: T | null;
  error: AppError | null;
  isLoading: boolean;
  execute: (...args: unknown[]) => Promise<T | null>;
  reset: () => void;
  clearError: () => void;
}

/**
 * Hook for handling async operations with loading and error states.
 */
export function useAsync<T>(
  asyncFunction: (...args: unknown[]) => Promise<T>,
  options: UseAsyncOptions = {}
): UseAsyncReturn<T> {
  const { initialData = null, immediate = false, onSuccess, onError } = options;

  const [state, setState] = useState<AsyncState<T>>({
    data: initialData as T | null,
    error: null,
    isLoading: immediate,
  });

  const execute = useCallback(
    async (...args: unknown[]): Promise<T | null> => {
      setState((prev) => ({ ...prev, isLoading: true, error: null }));

      try {
        const result = await asyncFunction(...args);
        setState({ data: result, error: null, isLoading: false });
        onSuccess?.(result);
        return result;
      } catch (error) {
        const appError = parseApiError(error);
        setState((prev) => ({ ...prev, error: appError, isLoading: false }));
        onError?.(appError);
        return null;
      }
    },
    [asyncFunction, onSuccess, onError]
  );

  const reset = useCallback(() => {
    setState({ data: initialData as T | null, error: null, isLoading: false });
  }, [initialData]);

  const clearError = useCallback(() => {
    setState((prev) => ({ ...prev, error: null }));
  }, []);

  // Run immediately if specified
  useEffect(() => {
    if (immediate) {
      execute();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [immediate]);

  return {
    data: state.data,
    error: state.error,
    isLoading: state.isLoading,
    execute,
    reset,
    clearError,
  };
}

// MARK: - Mutation Hook

interface MutationState<T> {
  data: T | null;
  error: AppError | null;
  isLoading: boolean;
  isSuccess: boolean;
}

interface UseMutationOptions<T> {
  onSuccess?: (data: T) => void;
  onError?: (error: AppError) => void;
  onSettled?: () => void;
}

interface UseMutationReturn<TVariables, TData> {
  mutate: (variables: TVariables) => Promise<TData | null>;
  mutateAsync: (variables: TVariables) => Promise<TData>;
  data: TData | null;
  error: AppError | null;
  isLoading: boolean;
  isSuccess: boolean;
  reset: () => void;
}

/**
 * Hook for handling mutations with loading and error states.
 */
export function useMutation<TVariables, TData>(
  mutationFn: (variables: TVariables) => Promise<TData>,
  options: UseMutationOptions<TData> = {}
): UseMutationReturn<TVariables, TData> {
  const { onSuccess, onError, onSettled } = options;

  const [state, setState] = useState<MutationState<TData>>({
    data: null,
    error: null,
    isLoading: false,
    isSuccess: false,
  });

  const mutate = useCallback(
    async (variables: TVariables): Promise<TData | null> => {
      setState({ data: null, error: null, isLoading: true, isSuccess: false });

      try {
        const result = await mutationFn(variables);
        setState({ data: result, error: null, isLoading: false, isSuccess: true });
        onSuccess?.(result);
        onSettled?.();
        return result;
      } catch (error) {
        const appError = parseApiError(error);
        setState({ data: null, error: appError, isLoading: false, isSuccess: false });
        onError?.(appError);
        onSettled?.();
        return null;
      }
    },
    [mutationFn, onSuccess, onError, onSettled]
  );

  const mutateAsync = useCallback(
    async (variables: TVariables): Promise<TData> => {
      setState({ data: null, error: null, isLoading: true, isSuccess: false });

      try {
        const result = await mutationFn(variables);
        setState({ data: result, error: null, isLoading: false, isSuccess: true });
        onSuccess?.(result);
        onSettled?.();
        return result;
      } catch (error) {
        const appError = parseApiError(error);
        setState({ data: null, error: appError, isLoading: false, isSuccess: false });
        onError?.(appError);
        onSettled?.();
        throw appError;
      }
    },
    [mutationFn, onSuccess, onError, onSettled]
  );

  const reset = useCallback(() => {
    setState({ data: null, error: null, isLoading: false, isSuccess: false });
  }, []);

  return {
    mutate,
    mutateAsync,
    data: state.data,
    error: state.error,
    isLoading: state.isLoading,
    isSuccess: state.isSuccess,
    reset,
  };
}

export default useErrorHandler;
