"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  Shield,
  ShieldCheck,
  ShieldOff,
  QrCode,
  Key,
  Copy,
  Check,
  Loader2,
  Eye,
  EyeOff,
  AlertTriangle,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { useTwoFactorSetup } from "@/lib/hooks/useSettings";

const verifySchema = z.object({
  code: z
    .string()
    .length(6, "Code must be 6 digits")
    .regex(/^\d+$/, "Code must contain only numbers"),
});

const disableSchema = z.object({
  password: z.string().min(1, "Password is required"),
});

type VerifyFormData = z.infer<typeof verifySchema>;
type DisableFormData = z.infer<typeof disableSchema>;

type SetupStep = "initial" | "qr-code" | "verify" | "backup-codes";

interface TwoFactorSetupProps {
  isEnabled: boolean;
  onSetupComplete?: () => void;
  onDisableComplete?: () => void;
}

export function TwoFactorSetup({
  isEnabled,
  onSetupComplete,
  onDisableComplete,
}: TwoFactorSetupProps) {
  const [step, setStep] = useState<SetupStep>("initial");
  const [showDisableForm, setShowDisableForm] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [copiedSecret, setCopiedSecret] = useState(false);
  const [copiedCodes, setCopiedCodes] = useState(false);

  const {
    setupData,
    backupCodes,
    isSettingUp,
    isVerifying,
    isDisabling,
    isFetchingBackupCodes,
    setup2FA,
    verify2FA,
    disable2FA,
    getBackupCodes,
    reset,
  } = useTwoFactorSetup();

  const verifyForm = useForm<VerifyFormData>({
    resolver: zodResolver(verifySchema),
    defaultValues: { code: "" },
  });

  const disableForm = useForm<DisableFormData>({
    resolver: zodResolver(disableSchema),
    defaultValues: { password: "" },
  });

  const handleStartSetup = async () => {
    try {
      await setup2FA();
      setStep("qr-code");
    } catch {
      // Error handled in hook
    }
  };

  const handleVerify = async (data: VerifyFormData) => {
    try {
      await verify2FA(data.code);
      setStep("backup-codes");
      await getBackupCodes();
    } catch {
      // Error handled in hook
    }
  };

  const handleDisable = async (data: DisableFormData) => {
    try {
      await disable2FA(data.password);
      setShowDisableForm(false);
      disableForm.reset();
      onDisableComplete?.();
    } catch {
      // Error handled in hook
    }
  };

  const handleComplete = () => {
    setStep("initial");
    verifyForm.reset();
    reset();
    onSetupComplete?.();
  };

  const copyToClipboard = async (text: string, type: "secret" | "codes") => {
    try {
      await navigator.clipboard.writeText(text);
      if (type === "secret") {
        setCopiedSecret(true);
        setTimeout(() => setCopiedSecret(false), 2000);
      } else {
        setCopiedCodes(true);
        setTimeout(() => setCopiedCodes(false), 2000);
      }
    } catch {
      // Clipboard not available
    }
  };

  // Render enabled state with disable option
  if (isEnabled && step === "initial") {
    return (
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <ShieldCheck className="h-5 w-5 text-green-600" />
            <CardTitle className="text-lg">Two-Factor Authentication</CardTitle>
          </div>
          <CardDescription>
            Your account is protected with two-factor authentication.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <Alert className="border-green-200 bg-green-50">
            <ShieldCheck className="h-4 w-4 text-green-600" />
            <AlertTitle className="text-green-800">2FA is enabled</AlertTitle>
            <AlertDescription className="text-green-700">
              Your account has an extra layer of security. You will need to
              enter a verification code from your authenticator app when logging
              in.
            </AlertDescription>
          </Alert>

          {!showDisableForm ? (
            <Button
              variant="outline"
              onClick={() => setShowDisableForm(true)}
              className="text-red-600 hover:text-red-700 hover:bg-red-50"
            >
              <ShieldOff className="h-4 w-4 mr-2" />
              Disable 2FA
            </Button>
          ) : (
            <div className="space-y-4 border rounded-lg p-4 bg-red-50/50">
              <Alert variant="destructive">
                <AlertTriangle className="h-4 w-4" />
                <AlertTitle>Confirm disable</AlertTitle>
                <AlertDescription>
                  Disabling 2FA will make your account less secure. Enter your
                  password to confirm.
                </AlertDescription>
              </Alert>

              <form
                onSubmit={disableForm.handleSubmit(handleDisable)}
                className="space-y-4"
              >
                <div className="space-y-2">
                  <Label htmlFor="disable-password">Password</Label>
                  <div className="relative">
                    <Input
                      id="disable-password"
                      type={showPassword ? "text" : "password"}
                      placeholder="Enter your password"
                      {...disableForm.register("password")}
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="absolute right-0 top-0 h-full px-3"
                      onClick={() => setShowPassword(!showPassword)}
                    >
                      {showPassword ? (
                        <EyeOff className="h-4 w-4" />
                      ) : (
                        <Eye className="h-4 w-4" />
                      )}
                    </Button>
                  </div>
                  {disableForm.formState.errors.password && (
                    <p className="text-sm text-red-500">
                      {disableForm.formState.errors.password.message}
                    </p>
                  )}
                </div>

                <div className="flex gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setShowDisableForm(false);
                      disableForm.reset();
                    }}
                  >
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    variant="destructive"
                    disabled={isDisabling}
                  >
                    {isDisabling ? (
                      <>
                        <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                        Disabling...
                      </>
                    ) : (
                      "Disable 2FA"
                    )}
                  </Button>
                </div>
              </form>
            </div>
          )}
        </CardContent>
      </Card>
    );
  }

  // Initial state - not enabled
  if (step === "initial") {
    return (
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Shield className="h-5 w-5" />
            <CardTitle className="text-lg">Two-Factor Authentication</CardTitle>
          </div>
          <CardDescription>
            Add an extra layer of security to your account by enabling
            two-factor authentication.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <Alert>
            <Shield className="h-4 w-4" />
            <AlertTitle>Recommended</AlertTitle>
            <AlertDescription>
              Two-factor authentication adds an extra layer of security to your
              account. You will need an authenticator app like Google
              Authenticator or Authy.
            </AlertDescription>
          </Alert>

          <Button onClick={handleStartSetup} disabled={isSettingUp}>
            {isSettingUp ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Setting up...
              </>
            ) : (
              <>
                <ShieldCheck className="h-4 w-4 mr-2" />
                Enable 2FA
              </>
            )}
          </Button>
        </CardContent>
      </Card>
    );
  }

  // QR Code step
  if (step === "qr-code") {
    return (
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <QrCode className="h-5 w-5" />
            <CardTitle className="text-lg">Scan QR Code</CardTitle>
          </div>
          <CardDescription>
            Scan this QR code with your authenticator app (Google Authenticator,
            Authy, etc.)
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {setupData?.qrCode && (
            <div className="flex justify-center">
              <div className="p-4 bg-white rounded-lg border">
                <img
                  src={setupData.qrCode}
                  alt="2FA QR Code"
                  className="w-48 h-48"
                />
              </div>
            </div>
          )}

          {setupData?.secret && (
            <div className="space-y-2">
              <Label>Manual entry code</Label>
              <div className="flex items-center gap-2">
                <code className="flex-1 p-2 bg-muted rounded text-sm font-mono break-all">
                  {setupData.secret}
                </code>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => copyToClipboard(setupData.secret, "secret")}
                >
                  {copiedSecret ? (
                    <Check className="h-4 w-4" />
                  ) : (
                    <Copy className="h-4 w-4" />
                  )}
                </Button>
              </div>
              <p className="text-xs text-muted-foreground">
                If you cannot scan the QR code, enter this code manually in your
                authenticator app.
              </p>
            </div>
          )}

          <div className="flex gap-2">
            <Button
              variant="outline"
              onClick={() => {
                setStep("initial");
                reset();
              }}
            >
              Cancel
            </Button>
            <Button onClick={() => setStep("verify")}>
              Continue to Verification
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  // Verify step
  if (step === "verify") {
    return (
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Key className="h-5 w-5" />
            <CardTitle className="text-lg">Verify Setup</CardTitle>
          </div>
          <CardDescription>
            Enter the 6-digit code from your authenticator app to complete
            setup.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form
            onSubmit={verifyForm.handleSubmit(handleVerify)}
            className="space-y-4"
          >
            <div className="space-y-2">
              <Label htmlFor="verify-code">Verification Code</Label>
              <Input
                id="verify-code"
                type="text"
                inputMode="numeric"
                maxLength={6}
                placeholder="000000"
                className="text-center text-2xl tracking-widest"
                {...verifyForm.register("code")}
              />
              {verifyForm.formState.errors.code && (
                <p className="text-sm text-red-500">
                  {verifyForm.formState.errors.code.message}
                </p>
              )}
            </div>

            <div className="flex gap-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => setStep("qr-code")}
              >
                Back
              </Button>
              <Button type="submit" disabled={isVerifying}>
                {isVerifying ? (
                  <>
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    Verifying...
                  </>
                ) : (
                  "Verify & Enable"
                )}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    );
  }

  // Backup codes step
  if (step === "backup-codes") {
    return (
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <ShieldCheck className="h-5 w-5 text-green-600" />
            <CardTitle className="text-lg">2FA Enabled Successfully!</CardTitle>
          </div>
          <CardDescription>
            Save these backup codes in a secure location. You can use them to
            access your account if you lose your authenticator device.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <Alert className="border-yellow-200 bg-yellow-50">
            <AlertTriangle className="h-4 w-4 text-yellow-600" />
            <AlertTitle className="text-yellow-800">Important</AlertTitle>
            <AlertDescription className="text-yellow-700">
              Each backup code can only be used once. Keep them secure and do
              not share them with anyone.
            </AlertDescription>
          </Alert>

          {isFetchingBackupCodes ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-6 w-6 animate-spin" />
            </div>
          ) : (
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label>Backup Codes</Label>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() =>
                    copyToClipboard(backupCodes?.join("\n") || "", "codes")
                  }
                >
                  {copiedCodes ? (
                    <>
                      <Check className="h-4 w-4 mr-1" />
                      Copied
                    </>
                  ) : (
                    <>
                      <Copy className="h-4 w-4 mr-1" />
                      Copy All
                    </>
                  )}
                </Button>
              </div>
              <div className="grid grid-cols-2 gap-2 p-4 bg-muted rounded-lg">
                {backupCodes?.map((code, index) => (
                  <code
                    key={index}
                    className="font-mono text-sm p-2 bg-background rounded"
                  >
                    {code}
                  </code>
                ))}
              </div>
            </div>
          )}

          <Button onClick={handleComplete} className="w-full">
            Done
          </Button>
        </CardContent>
      </Card>
    );
  }

  return null;
}

export default TwoFactorSetup;
