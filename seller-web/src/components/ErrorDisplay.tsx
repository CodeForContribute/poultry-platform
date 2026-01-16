"use client";

import React, { ReactNode } from "react";
import {
  AlertCircle,
  AlertTriangle,
  Info,
  Lock,
  RefreshCw,
  Server,
  WifiOff,
  X,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { AppError, ErrorCategory, getErrorCategoryColor } from "@/lib/errors";
import { cn } from "@/lib/utils";

// MARK: - Types

interface ErrorDisplayProps {
  error: AppError | Error | string;
  onRetry?: () => void;
  onDismiss?: () => void;
  onLogin?: () => void;
  variant?: "card" | "inline" | "banner" | "fullscreen";
  className?: string;
}

// MARK: - Error Display Component

/**
 * A reusable error display component with different variants for different contexts.
 */
export function ErrorDisplay({
  error,
  onRetry,
  onDismiss,
  onLogin,
  variant = "card",
  className,
}: ErrorDisplayProps): ReactNode {
  const errorInfo = normalizeError(error);

  switch (variant) {
    case "inline":
      return (
        <InlineError
          errorInfo={errorInfo}
          onRetry={onRetry}
          onDismiss={onDismiss}
          className={className}
        />
      );
    case "banner":
      return (
        <ErrorBanner
          errorInfo={errorInfo}
          onRetry={onRetry}
          onDismiss={onDismiss}
          className={className}
        />
      );
    case "fullscreen":
      return (
        <FullscreenError
          errorInfo={errorInfo}
          onRetry={onRetry}
          onLogin={onLogin}
          className={className}
        />
      );
    case "card":
    default:
      return (
        <ErrorCard
          errorInfo={errorInfo}
          onRetry={onRetry}
          onDismiss={onDismiss}
          onLogin={onLogin}
          className={className}
        />
      );
  }
}

// MARK: - Normalized Error Info

interface NormalizedError {
  title: string;
  message: string;
  category: ErrorCategory;
  isRecoverable: boolean;
  requiresAuth: boolean;
  fieldErrors?: Record<string, string>;
}

function normalizeError(error: AppError | Error | string): NormalizedError {
  if (error instanceof AppError) {
    return {
      title: error.title,
      message: error.message,
      category: error.category,
      isRecoverable: error.isRecoverable,
      requiresAuth: error.requiresAuth,
      fieldErrors: error.fieldErrors,
    };
  }

  if (error instanceof Error) {
    return {
      title: "Error",
      message: error.message,
      category: "unknown",
      isRecoverable: true,
      requiresAuth: false,
    };
  }

  return {
    title: "Error",
    message: error,
    category: "unknown",
    isRecoverable: true,
    requiresAuth: false,
  };
}

// MARK: - Error Card

interface ErrorCardProps {
  errorInfo: NormalizedError;
  onRetry?: () => void;
  onDismiss?: () => void;
  onLogin?: () => void;
  className?: string;
}

function ErrorCard({
  errorInfo,
  onRetry,
  onDismiss,
  onLogin,
  className,
}: ErrorCardProps): ReactNode {
  const Icon = getCategoryIcon(errorInfo.category);
  const iconColor = getIconColorClass(errorInfo.category);
  const bgColor = getBgColorClass(errorInfo.category);

  return (
    <Card className={cn("w-full max-w-md", className)}>
      <CardHeader className="text-center">
        <div
          className={cn(
            "mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full",
            bgColor
          )}
        >
          <Icon className={cn("h-8 w-8", iconColor)} />
        </div>
        <CardTitle className="text-xl font-semibold text-gray-900">{errorInfo.title}</CardTitle>
      </CardHeader>

      <CardContent>
        <p className="text-center text-gray-600">{errorInfo.message}</p>

        {errorInfo.fieldErrors && Object.keys(errorInfo.fieldErrors).length > 0 && (
          <div className="mt-4 space-y-1">
            {Object.entries(errorInfo.fieldErrors).map(([field, error]) => (
              <p key={field} className="text-sm text-red-600">
                <strong>{field}:</strong> {error}
              </p>
            ))}
          </div>
        )}
      </CardContent>

      <CardFooter className="flex justify-center gap-2">
        {onDismiss && (
          <Button variant="outline" onClick={onDismiss}>
            Dismiss
          </Button>
        )}
        {errorInfo.requiresAuth && onLogin ? (
          <Button onClick={onLogin}>
            <Lock className="mr-2 h-4 w-4" />
            Login
          </Button>
        ) : (
          errorInfo.isRecoverable &&
          onRetry && (
            <Button onClick={onRetry}>
              <RefreshCw className="mr-2 h-4 w-4" />
              Retry
            </Button>
          )
        )}
      </CardFooter>
    </Card>
  );
}

// MARK: - Inline Error

interface InlineErrorProps {
  errorInfo: NormalizedError;
  onRetry?: () => void;
  onDismiss?: () => void;
  className?: string;
}

function InlineError({ errorInfo, onRetry, onDismiss, className }: InlineErrorProps): ReactNode {
  const Icon = getCategoryIcon(errorInfo.category);
  const alertVariant = errorInfo.category === "validation" ? "default" : "destructive";

  return (
    <Alert variant={alertVariant} className={cn("relative", className)}>
      <Icon className="h-4 w-4" />
      <AlertTitle>{errorInfo.title}</AlertTitle>
      <AlertDescription className="flex items-center justify-between">
        <span>{errorInfo.message}</span>
        <div className="flex gap-2">
          {errorInfo.isRecoverable && onRetry && (
            <Button variant="ghost" size="sm" onClick={onRetry}>
              <RefreshCw className="h-3 w-3" />
            </Button>
          )}
          {onDismiss && (
            <Button variant="ghost" size="sm" onClick={onDismiss}>
              <X className="h-3 w-3" />
            </Button>
          )}
        </div>
      </AlertDescription>
    </Alert>
  );
}

// MARK: - Error Banner

interface ErrorBannerProps {
  errorInfo: NormalizedError;
  onRetry?: () => void;
  onDismiss?: () => void;
  className?: string;
}

function ErrorBanner({ errorInfo, onRetry, onDismiss, className }: ErrorBannerProps): ReactNode {
  const Icon = getCategoryIcon(errorInfo.category);
  const bgColor = getBgColorClass(errorInfo.category);
  const iconColor = getIconColorClass(errorInfo.category);

  return (
    <div
      className={cn(
        "flex items-center justify-between rounded-lg p-4",
        bgColor,
        className
      )}
    >
      <div className="flex items-center gap-3">
        <Icon className={cn("h-5 w-5", iconColor)} />
        <div>
          <p className={cn("font-medium", iconColor)}>{errorInfo.title}</p>
          <p className="text-sm text-gray-600">{errorInfo.message}</p>
        </div>
      </div>
      <div className="flex items-center gap-2">
        {errorInfo.isRecoverable && onRetry && (
          <Button variant="ghost" size="sm" onClick={onRetry}>
            <RefreshCw className={cn("h-4 w-4", iconColor)} />
          </Button>
        )}
        {onDismiss && (
          <Button variant="ghost" size="sm" onClick={onDismiss}>
            <X className="h-4 w-4 text-gray-500" />
          </Button>
        )}
      </div>
    </div>
  );
}

// MARK: - Fullscreen Error

interface FullscreenErrorProps {
  errorInfo: NormalizedError;
  onRetry?: () => void;
  onLogin?: () => void;
  className?: string;
}

function FullscreenError({
  errorInfo,
  onRetry,
  onLogin,
  className,
}: FullscreenErrorProps): ReactNode {
  const Icon = getCategoryIcon(errorInfo.category);
  const iconColor = getIconColorClass(errorInfo.category);
  const bgColor = getBgColorClass(errorInfo.category);

  return (
    <div
      className={cn(
        "flex min-h-screen flex-col items-center justify-center bg-gray-50 p-8",
        className
      )}
    >
      <div
        className={cn(
          "mb-6 flex h-24 w-24 items-center justify-center rounded-full",
          bgColor
        )}
      >
        <Icon className={cn("h-12 w-12", iconColor)} />
      </div>

      <h1 className="mb-2 text-2xl font-bold text-gray-900">{errorInfo.title}</h1>
      <p className="mb-8 max-w-md text-center text-gray-600">{errorInfo.message}</p>

      <div className="flex gap-4">
        {errorInfo.requiresAuth && onLogin ? (
          <Button size="lg" onClick={onLogin}>
            <Lock className="mr-2 h-5 w-5" />
            Login
          </Button>
        ) : (
          errorInfo.isRecoverable &&
          onRetry && (
            <Button size="lg" onClick={onRetry}>
              <RefreshCw className="mr-2 h-5 w-5" />
              Try Again
            </Button>
          )
        )}
      </div>
    </div>
  );
}

// MARK: - Helper Functions

function getCategoryIcon(
  category: ErrorCategory
): React.ComponentType<{ className?: string }> {
  switch (category) {
    case "network":
      return WifiOff;
    case "server":
      return Server;
    case "authentication":
      return Lock;
    case "validation":
      return AlertTriangle;
    case "business":
      return Info;
    case "unknown":
    default:
      return AlertCircle;
  }
}

function getIconColorClass(category: ErrorCategory): string {
  switch (category) {
    case "network":
      return "text-orange-600";
    case "server":
      return "text-red-600";
    case "authentication":
      return "text-yellow-600";
    case "validation":
      return "text-orange-600";
    case "business":
      return "text-blue-600";
    case "unknown":
    default:
      return "text-red-600";
  }
}

function getBgColorClass(category: ErrorCategory): string {
  switch (category) {
    case "network":
      return "bg-orange-100";
    case "server":
      return "bg-red-100";
    case "authentication":
      return "bg-yellow-100";
    case "validation":
      return "bg-orange-100";
    case "business":
      return "bg-blue-100";
    case "unknown":
    default:
      return "bg-red-100";
  }
}

// MARK: - Empty State Component

interface EmptyStateProps {
  title: string;
  message: string;
  icon?: React.ComponentType<{ className?: string }>;
  action?: {
    label: string;
    onClick: () => void;
  };
  className?: string;
}

export function EmptyState({
  title,
  message,
  icon: Icon = Info,
  action,
  className,
}: EmptyStateProps): ReactNode {
  return (
    <div className={cn("flex flex-col items-center justify-center p-8 text-center", className)}>
      <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-gray-100">
        <Icon className="h-8 w-8 text-gray-400" />
      </div>
      <h3 className="mb-2 text-lg font-medium text-gray-900">{title}</h3>
      <p className="mb-4 max-w-sm text-gray-600">{message}</p>
      {action && (
        <Button variant="outline" onClick={action.onClick}>
          {action.label}
        </Button>
      )}
    </div>
  );
}

// MARK: - Loading Error Container

interface LoadingErrorContainerProps {
  isLoading: boolean;
  error: AppError | Error | string | null;
  onRetry?: () => void;
  loadingComponent?: ReactNode;
  children: ReactNode;
  className?: string;
}

export function LoadingErrorContainer({
  isLoading,
  error,
  onRetry,
  loadingComponent,
  children,
  className,
}: LoadingErrorContainerProps): ReactNode {
  if (isLoading) {
    return (
      loadingComponent || (
        <div className={cn("flex items-center justify-center p-8", className)}>
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-gray-200 border-t-primary" />
        </div>
      )
    );
  }

  if (error) {
    return (
      <div className={cn("flex items-center justify-center p-8", className)}>
        <ErrorDisplay error={error} onRetry={onRetry} variant="card" />
      </div>
    );
  }

  return <>{children}</>;
}

export default ErrorDisplay;
