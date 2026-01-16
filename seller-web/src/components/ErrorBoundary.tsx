"use client";

import React, { Component, ErrorInfo, ReactNode } from "react";
import { AlertTriangle, RefreshCw, Home } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";

// MARK: - Types

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
  onReset?: () => void;
  showDetails?: boolean;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
}

// MARK: - Error Boundary Component

/**
 * React Error Boundary component that catches JavaScript errors in child components.
 * Provides a fallback UI and error reporting capabilities.
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
    };
  }

  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    this.setState({ errorInfo });

    // Call the onError callback if provided
    this.props.onError?.(error, errorInfo);

    // Log to console in development
    if (process.env.NODE_ENV === "development") {
      console.error("Error Boundary caught an error:", error, errorInfo);
    }

    // TODO: Send to error reporting service (Sentry, etc.)
    // reportError(error, errorInfo);
  }

  handleReset = (): void => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
    });
    this.props.onReset?.();
  };

  handleGoHome = (): void => {
    window.location.href = "/";
  };

  handleRefresh = (): void => {
    window.location.reload();
  };

  render(): ReactNode {
    if (this.state.hasError) {
      // Use custom fallback if provided
      if (this.props.fallback) {
        return this.props.fallback;
      }

      // Default error UI
      return (
        <ErrorFallback
          error={this.state.error}
          errorInfo={this.state.errorInfo}
          onReset={this.handleReset}
          onGoHome={this.handleGoHome}
          onRefresh={this.handleRefresh}
          showDetails={this.props.showDetails}
        />
      );
    }

    return this.props.children;
  }
}

// MARK: - Error Fallback Component

interface ErrorFallbackProps {
  error: Error | null;
  errorInfo: ErrorInfo | null;
  onReset: () => void;
  onGoHome: () => void;
  onRefresh: () => void;
  showDetails?: boolean;
}

function ErrorFallback({
  error,
  errorInfo,
  onReset,
  onGoHome,
  onRefresh,
  showDetails = process.env.NODE_ENV === "development",
}: ErrorFallbackProps): ReactNode {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-red-100">
            <AlertTriangle className="h-8 w-8 text-red-600" />
          </div>
          <CardTitle className="text-xl font-semibold text-gray-900">
            Something went wrong
          </CardTitle>
        </CardHeader>

        <CardContent className="space-y-4">
          <p className="text-center text-gray-600">
            We&apos;re sorry, but something unexpected happened. Please try refreshing the page or
            contact support if the problem persists.
          </p>

          {showDetails && error && (
            <details className="rounded-lg bg-gray-100 p-3">
              <summary className="cursor-pointer text-sm font-medium text-gray-700">
                Error Details
              </summary>
              <div className="mt-2 space-y-2">
                <p className="text-sm text-red-600">
                  <strong>Error:</strong> {error.message}
                </p>
                {errorInfo?.componentStack && (
                  <pre className="max-h-40 overflow-auto rounded bg-gray-200 p-2 text-xs text-gray-700">
                    {errorInfo.componentStack}
                  </pre>
                )}
              </div>
            </details>
          )}
        </CardContent>

        <CardFooter className="flex flex-col gap-2 sm:flex-row">
          <Button variant="outline" className="w-full sm:w-auto" onClick={onGoHome}>
            <Home className="mr-2 h-4 w-4" />
            Go Home
          </Button>
          <Button variant="outline" className="w-full sm:w-auto" onClick={onReset}>
            Try Again
          </Button>
          <Button className="w-full sm:w-auto" onClick={onRefresh}>
            <RefreshCw className="mr-2 h-4 w-4" />
            Refresh Page
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}

// MARK: - Page Error Boundary

/**
 * A pre-configured error boundary for page-level errors.
 */
export function PageErrorBoundary({ children }: { children: ReactNode }): ReactNode {
  return (
    <ErrorBoundary
      onError={(error, errorInfo) => {
        // Log to analytics or error reporting service
        console.error("Page Error:", error.message);
        console.error("Component Stack:", errorInfo.componentStack);
      }}
    >
      {children}
    </ErrorBoundary>
  );
}

// MARK: - Component Error Boundary

/**
 * A lightweight error boundary for component-level errors.
 * Shows a minimal error message without navigation options.
 */
interface ComponentErrorBoundaryProps {
  children: ReactNode;
  fallbackMessage?: string;
}

export function ComponentErrorBoundary({
  children,
  fallbackMessage = "Failed to load this section",
}: ComponentErrorBoundaryProps): ReactNode {
  return (
    <ErrorBoundary
      fallback={
        <div className="flex items-center justify-center rounded-lg bg-red-50 p-4 text-red-600">
          <AlertTriangle className="mr-2 h-5 w-5" />
          <span className="text-sm">{fallbackMessage}</span>
        </div>
      }
    >
      {children}
    </ErrorBoundary>
  );
}

// MARK: - Async Error Boundary Hook

/**
 * Hook for handling async errors that can be caught by error boundaries.
 */
export function useAsyncError(): (error: Error) => void {
  const [, setError] = React.useState<Error>();

  return React.useCallback((error: Error) => {
    setError(() => {
      throw error;
    });
  }, []);
}

export default ErrorBoundary;
