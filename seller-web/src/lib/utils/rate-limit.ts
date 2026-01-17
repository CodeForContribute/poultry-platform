import { useState, useCallback, useRef, useEffect } from "react";

interface RateLimitConfig {
  maxAttempts: number;
  windowMs: number;
}

interface RateLimitState {
  attempts: number;
  resetAt: number;
}

interface RateLimiter {
  canAttempt: () => boolean;
  recordAttempt: () => void;
  getRemainingAttempts: () => number;
  getTimeUntilReset: () => number;
  reset: () => void;
}

export function createRateLimiter(config: RateLimitConfig): RateLimiter {
  let state: RateLimitState = {
    attempts: 0,
    resetAt: Date.now() + config.windowMs,
  };

  const checkAndResetWindow = () => {
    if (Date.now() >= state.resetAt) {
      state = {
        attempts: 0,
        resetAt: Date.now() + config.windowMs,
      };
    }
  };

  return {
    canAttempt: () => {
      checkAndResetWindow();
      return state.attempts < config.maxAttempts;
    },
    recordAttempt: () => {
      checkAndResetWindow();
      state.attempts++;
    },
    getRemainingAttempts: () => {
      checkAndResetWindow();
      return Math.max(0, config.maxAttempts - state.attempts);
    },
    getTimeUntilReset: () => {
      return Math.max(0, state.resetAt - Date.now());
    },
    reset: () => {
      state = {
        attempts: 0,
        resetAt: Date.now() + config.windowMs,
      };
    },
  };
}

// Hook for rate-limited actions
export function useRateLimitedAction(config: RateLimitConfig) {
  const limiterRef = useRef<RateLimiter | null>(null);
  const [isBlocked, setIsBlocked] = useState(false);
  const [timeUntilReset, setTimeUntilReset] = useState(0);
  const [remainingAttempts, setRemainingAttempts] = useState(config.maxAttempts);

  // Initialize limiter
  if (!limiterRef.current) {
    limiterRef.current = createRateLimiter(config);
  }

  const limiter = limiterRef.current;

  // Update timer when blocked
  useEffect(() => {
    if (!isBlocked) return;

    const interval = setInterval(() => {
      const remaining = limiter.getTimeUntilReset();
      setTimeUntilReset(remaining);

      if (remaining <= 0) {
        setIsBlocked(false);
        setRemainingAttempts(limiter.getRemainingAttempts());
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [isBlocked, limiter]);

  const attempt = useCallback(
    async <T>(action: () => Promise<T>): Promise<T> => {
      if (!limiter.canAttempt()) {
        setIsBlocked(true);
        setTimeUntilReset(limiter.getTimeUntilReset());
        throw new Error(
          `Rate limit exceeded. Try again in ${Math.ceil(
            limiter.getTimeUntilReset() / 1000
          )} seconds.`
        );
      }

      limiter.recordAttempt();
      setRemainingAttempts(limiter.getRemainingAttempts());

      if (!limiter.canAttempt()) {
        setIsBlocked(true);
        setTimeUntilReset(limiter.getTimeUntilReset());
      }

      return action();
    },
    [limiter]
  );

  const reset = useCallback(() => {
    limiter.reset();
    setIsBlocked(false);
    setTimeUntilReset(0);
    setRemainingAttempts(config.maxAttempts);
  }, [limiter, config.maxAttempts]);

  return {
    attempt,
    isBlocked,
    timeUntilReset,
    remainingAttempts,
    reset,
  };
}

// Pre-configured rate limiters for common use cases
export const LoginRateLimiter = () =>
  createRateLimiter({
    maxAttempts: 5,
    windowMs: 60 * 1000, // 5 attempts per minute
  });

export const FormSubmissionRateLimiter = () =>
  createRateLimiter({
    maxAttempts: 10,
    windowMs: 60 * 1000, // 10 attempts per minute
  });

export const ApiCallRateLimiter = () =>
  createRateLimiter({
    maxAttempts: 100,
    windowMs: 60 * 1000, // 100 calls per minute
  });

// Rate limit hook configurations
export const useLoginRateLimit = () =>
  useRateLimitedAction({ maxAttempts: 5, windowMs: 60 * 1000 });

export const useFormSubmissionRateLimit = () =>
  useRateLimitedAction({ maxAttempts: 10, windowMs: 60 * 1000 });
