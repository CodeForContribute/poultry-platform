"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, Play, Shield } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuthStore } from "@/lib/store/auth-store";

const loginSchema = z.object({
  email: z.string().email("Please enter a valid email address"),
  password: z.string().min(1, "Password is required"),
});

const twoFactorSchema = z.object({
  code: z
    .string()
    .length(6, "Code must be 6 digits")
    .regex(/^\d+$/, "Code must contain only numbers"),
});

type LoginFormData = z.infer<typeof loginSchema>;
type TwoFactorFormData = z.infer<typeof twoFactorSchema>;

export function LoginForm() {
  const router = useRouter();
  const { login, verify2FA, setDemoAuth, isLoading } = useAuthStore();
  const [error, setError] = React.useState<string | null>(null);
  const [requires2FA, setRequires2FA] = React.useState(false);
  const [twoFactorToken, setTwoFactorToken] = React.useState<string | null>(null);

  const handleDemoMode = () => {
    setDemoAuth();
    router.push("/analytics");
  };

  const loginForm = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const twoFactorForm = useForm<TwoFactorFormData>({
    resolver: zodResolver(twoFactorSchema),
  });

  const onLoginSubmit = async (data: LoginFormData) => {
    setError(null);
    try {
      const result = await login(data.email, data.password);
      if (result?.requires2FA && result?.twoFactorToken) {
        setRequires2FA(true);
        setTwoFactorToken(result.twoFactorToken);
      } else {
        router.push("/analytics");
      }
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Invalid email or password"
      );
    }
  };

  const on2FASubmit = async (data: TwoFactorFormData) => {
    setError(null);
    if (!twoFactorToken) return;

    try {
      await verify2FA(twoFactorToken, data.code);
      router.push("/analytics");
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Invalid verification code"
      );
    }
  };

  const handleBack = () => {
    setRequires2FA(false);
    setTwoFactorToken(null);
    setError(null);
    twoFactorForm.reset();
  };

  // 2FA Verification Form
  if (requires2FA) {
    return (
      <form onSubmit={twoFactorForm.handleSubmit(on2FASubmit)} className="space-y-4">
        <div className="text-center space-y-2">
          <div className="mx-auto w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center">
            <Shield className="h-6 w-6 text-primary" />
          </div>
          <h3 className="text-lg font-semibold">Two-Factor Authentication</h3>
          <p className="text-sm text-muted-foreground">
            Enter the 6-digit code from your authenticator app
          </p>
        </div>

        {error && (
          <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
            {error}
          </div>
        )}

        <div className="space-y-2">
          <Label htmlFor="code">Verification Code</Label>
          <Input
            id="code"
            type="text"
            inputMode="numeric"
            maxLength={6}
            placeholder="000000"
            className="text-center text-2xl tracking-widest"
            disabled={isLoading}
            {...twoFactorForm.register("code")}
          />
          {twoFactorForm.formState.errors.code && (
            <p className="text-sm text-destructive">
              {twoFactorForm.formState.errors.code.message}
            </p>
          )}
        </div>

        <Button type="submit" className="w-full" disabled={isLoading}>
          {isLoading ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Verifying...
            </>
          ) : (
            "Verify"
          )}
        </Button>

        <Button
          type="button"
          variant="ghost"
          className="w-full"
          onClick={handleBack}
          disabled={isLoading}
        >
          Back to Login
        </Button>
      </form>
    );
  }

  // Login Form
  return (
    <form onSubmit={loginForm.handleSubmit(onLoginSubmit)} className="space-y-4">
      {error && (
        <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
          {error}
        </div>
      )}

      <div className="space-y-2">
        <Label htmlFor="email">Email</Label>
        <Input
          id="email"
          type="email"
          placeholder="seller@example.com"
          autoComplete="email"
          disabled={isLoading}
          {...loginForm.register("email")}
        />
        {loginForm.formState.errors.email && (
          <p className="text-sm text-destructive">{loginForm.formState.errors.email.message}</p>
        )}
      </div>

      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <Label htmlFor="password">Password</Label>
          <Link
            href="/forgot-password"
            className="text-sm text-muted-foreground hover:text-primary"
          >
            Forgot password?
          </Link>
        </div>
        <Input
          id="password"
          type="password"
          placeholder="Enter your password"
          autoComplete="current-password"
          disabled={isLoading}
          {...loginForm.register("password")}
        />
        {loginForm.formState.errors.password && (
          <p className="text-sm text-destructive">{loginForm.formState.errors.password.message}</p>
        )}
      </div>

      <Button type="submit" className="w-full" disabled={isLoading}>
        {isLoading ? (
          <>
            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            Signing in...
          </>
        ) : (
          "Sign In"
        )}
      </Button>

      <div className="relative">
        <div className="absolute inset-0 flex items-center">
          <span className="w-full border-t" />
        </div>
        <div className="relative flex justify-center text-xs uppercase">
          <span className="bg-background px-2 text-muted-foreground">Or</span>
        </div>
      </div>

      <Button
        type="button"
        variant="outline"
        className="w-full"
        onClick={handleDemoMode}
        disabled={isLoading}
      >
        <Play className="mr-2 h-4 w-4" />
        Try Demo
      </Button>
    </form>
  );
}
