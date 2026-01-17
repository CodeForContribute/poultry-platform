"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  Building2,
  CreditCard,
  Bell,
  Shield,
  Loader2,
  Save,
  Plus,
  Trash2,
  Star,
  Eye,
  EyeOff,
  LogOut,
  Smartphone,
  Monitor,
} from "lucide-react";
import { Header } from "@/components/layout/header";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Separator } from "@/components/ui/separator";
import { Badge } from "@/components/ui/badge";
import { formatDate, formatRelativeTime } from "@/lib/utils";
import {
  useBusinessProfile,
  useBankAccounts,
  useNotificationPreferences,
  useSecuritySettings,
} from "@/lib/hooks/useSettings";
import { TwoFactorSetup } from "@/components/settings/two-factor-setup";
import type { NotificationPreferences } from "@/types";

// Validation schemas
const businessProfileSchema = z.object({
  businessName: z.string().min(2, "Business name is required"),
  businessNameHi: z.string().optional(),
  ownerName: z.string().min(2, "Owner name is required"),
  phone: z.string().regex(/^\d{10}$/, "Invalid phone number"),
  alternatePhone: z.string().regex(/^\d{10}$/, "Invalid phone number").optional().or(z.literal("")),
  gstin: z.string().regex(/^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/, "Invalid GSTIN").optional().or(z.literal("")),
  pan: z.string().regex(/^[A-Z]{5}[0-9]{4}[A-Z]{1}$/, "Invalid PAN").optional().or(z.literal("")),
  fssaiLicense: z.string().optional(),
  addressLine1: z.string().min(5, "Address is required"),
  addressLine2: z.string().optional(),
  city: z.string().min(2, "City is required"),
  state: z.string().min(2, "State is required"),
  pincode: z.string().regex(/^\d{6}$/, "Invalid pincode"),
  landmark: z.string().optional(),
  description: z.string().optional(),
});

const bankAccountSchema = z.object({
  accountHolderName: z.string().min(2, "Account holder name is required"),
  bankName: z.string().min(2, "Bank name is required"),
  accountNumber: z.string().min(9, "Invalid account number").max(18),
  confirmAccountNumber: z.string(),
  ifscCode: z.string().regex(/^[A-Z]{4}0[A-Z0-9]{6}$/, "Invalid IFSC code"),
  accountType: z.enum(["SAVINGS", "CURRENT"]),
  branch: z.string().optional(),
}).refine((data) => data.accountNumber === data.confirmAccountNumber, {
  message: "Account numbers don't match",
  path: ["confirmAccountNumber"],
});

const changePasswordSchema = z.object({
  currentPassword: z.string().min(1, "Current password is required"),
  newPassword: z.string().min(8, "Password must be at least 8 characters"),
  confirmPassword: z.string(),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: "Passwords don't match",
  path: ["confirmPassword"],
});

type BusinessProfileFormData = z.infer<typeof businessProfileSchema>;
type BankAccountFormData = z.infer<typeof bankAccountSchema>;
type ChangePasswordFormData = z.infer<typeof changePasswordSchema>;

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState("profile");

  return (
    <div>
      <Header title="Settings" />

      <div className="p-6">
        <Tabs value={activeTab} onValueChange={setActiveTab}>
          <TabsList className="grid w-full grid-cols-2 lg:grid-cols-4 mb-6">
            <TabsTrigger value="profile" className="gap-2">
              <Building2 className="h-4 w-4 hidden sm:inline" />
              Business
            </TabsTrigger>
            <TabsTrigger value="bank" className="gap-2">
              <CreditCard className="h-4 w-4 hidden sm:inline" />
              Bank
            </TabsTrigger>
            <TabsTrigger value="notifications" className="gap-2">
              <Bell className="h-4 w-4 hidden sm:inline" />
              Notifications
            </TabsTrigger>
            <TabsTrigger value="security" className="gap-2">
              <Shield className="h-4 w-4 hidden sm:inline" />
              Security
            </TabsTrigger>
          </TabsList>

          <TabsContent value="profile">
            <BusinessProfileTab />
          </TabsContent>

          <TabsContent value="bank">
            <BankAccountsTab />
          </TabsContent>

          <TabsContent value="notifications">
            <NotificationsTab />
          </TabsContent>

          <TabsContent value="security">
            <SecurityTab />
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}

function BusinessProfileTab() {
  const { profile, isLoading, error, updateProfile, isUpdating } = useBusinessProfile();

  const form = useForm<BusinessProfileFormData>({
    resolver: zodResolver(businessProfileSchema),
    values: profile ? {
      businessName: profile.businessName,
      businessNameHi: profile.businessNameHi || "",
      ownerName: profile.ownerName,
      phone: profile.phone,
      alternatePhone: profile.alternatePhone || "",
      gstin: profile.gstin || "",
      pan: profile.pan || "",
      fssaiLicense: profile.fssaiLicense || "",
      addressLine1: profile.address.line1,
      addressLine2: profile.address.line2 || "",
      city: profile.address.city,
      state: profile.address.state,
      pincode: profile.address.pincode,
      landmark: profile.address.landmark || "",
      description: profile.description || "",
    } : undefined,
  });

  const onSubmit = (data: BusinessProfileFormData) => {
    updateProfile({
      businessName: data.businessName,
      businessNameHi: data.businessNameHi,
      ownerName: data.ownerName,
      phone: data.phone,
      alternatePhone: data.alternatePhone,
      gstin: data.gstin,
      pan: data.pan,
      fssaiLicense: data.fssaiLicense,
      address: {
        line1: data.addressLine1,
        line2: data.addressLine2,
        city: data.city,
        state: data.state,
        pincode: data.pincode,
        landmark: data.landmark,
      },
      description: data.description,
    });
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (error) {
    return (
      <Card>
        <CardContent className="p-8 text-center text-destructive">
          {error.message || "Failed to load business profile"}
        </CardContent>
      </Card>
    );
  }

  return (
    <form onSubmit={form.handleSubmit(onSubmit)}>
      <div className="space-y-6">
        {/* Business Details */}
        <Card>
          <CardHeader>
            <CardTitle>Business Details</CardTitle>
            <CardDescription>
              Update your business information that will be shown to buyers
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="businessName">Business Name *</Label>
                <Input
                  id="businessName"
                  {...form.register("businessName")}
                  placeholder="Enter business name"
                />
                {form.formState.errors.businessName && (
                  <p className="text-sm text-destructive">
                    {form.formState.errors.businessName.message}
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="businessNameHi">Business Name (Hindi)</Label>
                <Input
                  id="businessNameHi"
                  {...form.register("businessNameHi")}
                  placeholder="व्यापार का नाम"
                />
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="ownerName">Owner Name *</Label>
                <Input
                  id="ownerName"
                  {...form.register("ownerName")}
                  placeholder="Enter owner name"
                />
                {form.formState.errors.ownerName && (
                  <p className="text-sm text-destructive">
                    {form.formState.errors.ownerName.message}
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="phone">Phone Number *</Label>
                <Input
                  id="phone"
                  {...form.register("phone")}
                  placeholder="10-digit mobile number"
                />
                {form.formState.errors.phone && (
                  <p className="text-sm text-destructive">
                    {form.formState.errors.phone.message}
                  </p>
                )}
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="alternatePhone">Alternate Phone</Label>
              <Input
                id="alternatePhone"
                {...form.register("alternatePhone")}
                placeholder="Alternate mobile number"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Business Description</Label>
              <Textarea
                id="description"
                {...form.register("description")}
                placeholder="Tell buyers about your business..."
                rows={3}
              />
            </div>
          </CardContent>
        </Card>

        {/* Tax Information */}
        <Card>
          <CardHeader>
            <CardTitle>Tax Information</CardTitle>
            <CardDescription>
              Required for GST invoicing and compliance
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="gstin">GSTIN</Label>
                <Input
                  id="gstin"
                  {...form.register("gstin")}
                  placeholder="29ABCDE1234F1Z5"
                  className="uppercase"
                />
                {form.formState.errors.gstin && (
                  <p className="text-sm text-destructive">
                    {form.formState.errors.gstin.message}
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="pan">PAN</Label>
                <Input
                  id="pan"
                  {...form.register("pan")}
                  placeholder="ABCDE1234F"
                  className="uppercase"
                />
                {form.formState.errors.pan && (
                  <p className="text-sm text-destructive">
                    {form.formState.errors.pan.message}
                  </p>
                )}
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="fssaiLicense">FSSAI License</Label>
              <Input
                id="fssaiLicense"
                {...form.register("fssaiLicense")}
                placeholder="14-digit license number"
              />
            </div>
          </CardContent>
        </Card>

        {/* Address */}
        <Card>
          <CardHeader>
            <CardTitle>Business Address</CardTitle>
            <CardDescription>
              Your business location for pickup and communication
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="addressLine1">Address Line 1 *</Label>
              <Input
                id="addressLine1"
                {...form.register("addressLine1")}
                placeholder="Street address, building name"
              />
              {form.formState.errors.addressLine1 && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.addressLine1.message}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="addressLine2">Address Line 2</Label>
              <Input
                id="addressLine2"
                {...form.register("addressLine2")}
                placeholder="Area, locality"
              />
            </div>

            <div className="grid gap-4 sm:grid-cols-3">
              <div className="space-y-2">
                <Label htmlFor="city">City *</Label>
                <Input
                  id="city"
                  {...form.register("city")}
                  placeholder="City"
                />
                {form.formState.errors.city && (
                  <p className="text-sm text-destructive">
                    {form.formState.errors.city.message}
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="state">State *</Label>
                <Input
                  id="state"
                  {...form.register("state")}
                  placeholder="State"
                />
                {form.formState.errors.state && (
                  <p className="text-sm text-destructive">
                    {form.formState.errors.state.message}
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="pincode">Pincode *</Label>
                <Input
                  id="pincode"
                  {...form.register("pincode")}
                  placeholder="6-digit pincode"
                />
                {form.formState.errors.pincode && (
                  <p className="text-sm text-destructive">
                    {form.formState.errors.pincode.message}
                  </p>
                )}
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="landmark">Landmark</Label>
              <Input
                id="landmark"
                {...form.register("landmark")}
                placeholder="Nearby landmark"
              />
            </div>
          </CardContent>
        </Card>

        {/* Submit Button */}
        <div className="flex justify-end">
          <Button type="submit" disabled={isUpdating}>
            {isUpdating ? (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            ) : (
              <Save className="mr-2 h-4 w-4" />
            )}
            Save Changes
          </Button>
        </div>
      </div>
    </form>
  );
}

function BankAccountsTab() {
  const { accounts, isLoading, error, addAccount, isAdding, deleteAccount, setPrimaryAccount } = useBankAccounts();
  const [showAddForm, setShowAddForm] = useState(false);
  const [showAccountNumber, setShowAccountNumber] = useState(false);

  const form = useForm<BankAccountFormData>({
    resolver: zodResolver(bankAccountSchema),
    defaultValues: {
      accountType: "CURRENT",
    },
  });

  const onSubmit = (data: BankAccountFormData) => {
    addAccount({
      accountHolderName: data.accountHolderName,
      bankName: data.bankName,
      accountNumber: data.accountNumber,
      confirmAccountNumber: data.confirmAccountNumber,
      ifscCode: data.ifscCode,
      accountType: data.accountType,
      branch: data.branch,
    });
    setShowAddForm(false);
    form.reset();
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (error) {
    return (
      <Card>
        <CardContent className="p-8 text-center text-destructive">
          {error.message || "Failed to load bank accounts"}
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>Bank Accounts</CardTitle>
            <CardDescription>
              Manage your bank accounts for receiving settlements
            </CardDescription>
          </div>
          {!showAddForm && (
            <Button onClick={() => setShowAddForm(true)}>
              <Plus className="mr-2 h-4 w-4" />
              Add Account
            </Button>
          )}
        </CardHeader>
        <CardContent>
          {/* Existing Accounts */}
          {accounts && accounts.length > 0 ? (
            <div className="space-y-4">
              {accounts.map((account) => (
                <div
                  key={account.id}
                  className="flex items-center justify-between p-4 border rounded-lg"
                >
                  <div className="flex items-center gap-4">
                    <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                      <CreditCard className="h-5 w-5 text-primary" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <p className="font-medium">{account.bankName}</p>
                        {account.isPrimary && (
                          <Badge variant="secondary" className="gap-1">
                            <Star className="h-3 w-3" />
                            Primary
                          </Badge>
                        )}
                        {account.isVerified && (
                          <Badge variant="success">Verified</Badge>
                        )}
                      </div>
                      <p className="text-sm text-muted-foreground">
                        {account.accountHolderName} - ****{account.accountNumberLast4}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        IFSC: {account.ifscCode}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {!account.isPrimary && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setPrimaryAccount(account.id)}
                      >
                        Set Primary
                      </Button>
                    )}
                    {!account.isPrimary && (
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => {
                          if (confirm("Are you sure you want to remove this account?")) {
                            deleteAccount(account.id);
                          }
                        }}
                      >
                        <Trash2 className="h-4 w-4 text-destructive" />
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8 text-muted-foreground">
              No bank accounts added yet. Add one to receive settlements.
            </div>
          )}
        </CardContent>
      </Card>

      {/* Add Account Form */}
      {showAddForm && (
        <Card>
          <CardHeader>
            <CardTitle>Add New Bank Account</CardTitle>
            <CardDescription>
              Enter your bank account details for receiving payments
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="accountHolderName">Account Holder Name *</Label>
                  <Input
                    id="accountHolderName"
                    {...form.register("accountHolderName")}
                    placeholder="As per bank records"
                  />
                  {form.formState.errors.accountHolderName && (
                    <p className="text-sm text-destructive">
                      {form.formState.errors.accountHolderName.message}
                    </p>
                  )}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="bankName">Bank Name *</Label>
                  <Input
                    id="bankName"
                    {...form.register("bankName")}
                    placeholder="Enter bank name"
                  />
                  {form.formState.errors.bankName && (
                    <p className="text-sm text-destructive">
                      {form.formState.errors.bankName.message}
                    </p>
                  )}
                </div>
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="accountNumber">Account Number *</Label>
                  <div className="relative">
                    <Input
                      id="accountNumber"
                      type={showAccountNumber ? "text" : "password"}
                      {...form.register("accountNumber")}
                      placeholder="Enter account number"
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="absolute right-0 top-0"
                      onClick={() => setShowAccountNumber(!showAccountNumber)}
                    >
                      {showAccountNumber ? (
                        <EyeOff className="h-4 w-4" />
                      ) : (
                        <Eye className="h-4 w-4" />
                      )}
                    </Button>
                  </div>
                  {form.formState.errors.accountNumber && (
                    <p className="text-sm text-destructive">
                      {form.formState.errors.accountNumber.message}
                    </p>
                  )}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="confirmAccountNumber">Confirm Account Number *</Label>
                  <Input
                    id="confirmAccountNumber"
                    type="password"
                    {...form.register("confirmAccountNumber")}
                    placeholder="Re-enter account number"
                  />
                  {form.formState.errors.confirmAccountNumber && (
                    <p className="text-sm text-destructive">
                      {form.formState.errors.confirmAccountNumber.message}
                    </p>
                  )}
                </div>
              </div>

              <div className="grid gap-4 sm:grid-cols-3">
                <div className="space-y-2">
                  <Label htmlFor="ifscCode">IFSC Code *</Label>
                  <Input
                    id="ifscCode"
                    {...form.register("ifscCode")}
                    placeholder="HDFC0001234"
                    className="uppercase"
                  />
                  {form.formState.errors.ifscCode && (
                    <p className="text-sm text-destructive">
                      {form.formState.errors.ifscCode.message}
                    </p>
                  )}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="accountType">Account Type *</Label>
                  <select
                    id="accountType"
                    {...form.register("accountType")}
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  >
                    <option value="CURRENT">Current</option>
                    <option value="SAVINGS">Savings</option>
                  </select>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="branch">Branch (Optional)</Label>
                  <Input
                    id="branch"
                    {...form.register("branch")}
                    placeholder="Branch name"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    setShowAddForm(false);
                    form.reset();
                  }}
                >
                  Cancel
                </Button>
                <Button type="submit" disabled={isAdding}>
                  {isAdding ? (
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  ) : (
                    <Plus className="mr-2 h-4 w-4" />
                  )}
                  Add Account
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

function NotificationsTab() {
  const { preferences, isLoading, error, updatePreferences, isUpdating } = useNotificationPreferences();
  const [localPrefs, setLocalPrefs] = useState<NotificationPreferences | null>(null);

  // Initialize local state when preferences load
  if (preferences && !localPrefs) {
    setLocalPrefs(preferences);
  }

  const handleToggle = (key: keyof NotificationPreferences) => {
    if (!localPrefs) return;
    setLocalPrefs({ ...localPrefs, [key]: !localPrefs[key] });
  };

  const handleSave = () => {
    if (localPrefs) {
      updatePreferences(localPrefs);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (error) {
    return (
      <Card>
        <CardContent className="p-8 text-center text-destructive">
          {error.message || "Failed to load notification preferences"}
        </CardContent>
      </Card>
    );
  }

  if (!localPrefs) return null;

  const notificationGroups = [
    {
      title: "Communication Channels",
      description: "Choose how you want to receive notifications",
      items: [
        { key: "emailNotifications" as const, label: "Email Notifications", description: "Receive notifications via email" },
        { key: "smsNotifications" as const, label: "SMS Notifications", description: "Receive notifications via SMS" },
        { key: "pushNotifications" as const, label: "Push Notifications", description: "Receive browser push notifications" },
      ],
    },
    {
      title: "Alert Types",
      description: "Select which alerts you want to receive",
      items: [
        { key: "orderAlerts" as const, label: "Order Alerts", description: "New orders, status updates, cancellations" },
        { key: "paymentAlerts" as const, label: "Payment Alerts", description: "Payment confirmations, settlement updates" },
        { key: "disputeAlerts" as const, label: "Dispute Alerts", description: "New disputes, resolution updates" },
      ],
    },
    {
      title: "Reports & Marketing",
      description: "Periodic reports and promotional content",
      items: [
        { key: "weeklyReport" as const, label: "Weekly Report", description: "Weekly sales and performance summary" },
        { key: "monthlyReport" as const, label: "Monthly Report", description: "Monthly business analytics" },
        { key: "promotionalEmails" as const, label: "Promotional Emails", description: "Tips, offers, and platform updates" },
      ],
    },
  ];

  return (
    <div className="space-y-6">
      {notificationGroups.map((group) => (
        <Card key={group.title}>
          <CardHeader>
            <CardTitle>{group.title}</CardTitle>
            <CardDescription>{group.description}</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {group.items.map((item) => (
              <div
                key={item.key}
                className="flex items-center justify-between py-2"
              >
                <div>
                  <p className="font-medium">{item.label}</p>
                  <p className="text-sm text-muted-foreground">{item.description}</p>
                </div>
                <Switch
                  checked={localPrefs[item.key]}
                  onCheckedChange={() => handleToggle(item.key)}
                />
              </div>
            ))}
          </CardContent>
        </Card>
      ))}

      <div className="flex justify-end">
        <Button onClick={handleSave} disabled={isUpdating}>
          {isUpdating ? (
            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
          ) : (
            <Save className="mr-2 h-4 w-4" />
          )}
          Save Preferences
        </Button>
      </div>
    </div>
  );
}

function SecurityTab() {
  const { settings, isLoading, error, changePassword, isChangingPassword, logoutAllSessions, isLoggingOutAll } = useSecuritySettings();
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);

  const form = useForm<ChangePasswordFormData>({
    resolver: zodResolver(changePasswordSchema),
  });

  const onSubmit = (data: ChangePasswordFormData) => {
    changePassword({
      currentPassword: data.currentPassword,
      newPassword: data.newPassword,
      confirmPassword: data.confirmPassword,
    });
    form.reset();
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (error) {
    return (
      <Card>
        <CardContent className="p-8 text-center text-destructive">
          {error.message || "Failed to load security settings"}
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      {/* Change Password */}
      <Card>
        <CardHeader>
          <CardTitle>Change Password</CardTitle>
          <CardDescription>
            {settings?.lastPasswordChange && (
              <>Last changed {formatRelativeTime(settings.lastPasswordChange)}</>
            )}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="currentPassword">Current Password</Label>
              <div className="relative">
                <Input
                  id="currentPassword"
                  type={showCurrentPassword ? "text" : "password"}
                  {...form.register("currentPassword")}
                  placeholder="Enter current password"
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="absolute right-0 top-0"
                  onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                >
                  {showCurrentPassword ? (
                    <EyeOff className="h-4 w-4" />
                  ) : (
                    <Eye className="h-4 w-4" />
                  )}
                </Button>
              </div>
              {form.formState.errors.currentPassword && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.currentPassword.message}
                </p>
              )}
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="newPassword">New Password</Label>
                <div className="relative">
                  <Input
                    id="newPassword"
                    type={showNewPassword ? "text" : "password"}
                    {...form.register("newPassword")}
                    placeholder="Enter new password"
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="absolute right-0 top-0"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                  >
                    {showNewPassword ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </Button>
                </div>
                {form.formState.errors.newPassword && (
                  <p className="text-sm text-destructive">
                    {form.formState.errors.newPassword.message}
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="confirmPassword">Confirm New Password</Label>
                <Input
                  id="confirmPassword"
                  type="password"
                  {...form.register("confirmPassword")}
                  placeholder="Confirm new password"
                />
                {form.formState.errors.confirmPassword && (
                  <p className="text-sm text-destructive">
                    {form.formState.errors.confirmPassword.message}
                  </p>
                )}
              </div>
            </div>

            <div className="flex justify-end">
              <Button type="submit" disabled={isChangingPassword}>
                {isChangingPassword ? (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                ) : (
                  <Shield className="mr-2 h-4 w-4" />
                )}
                Update Password
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {/* Two-Factor Authentication */}
      <TwoFactorSetup
        isEnabled={settings?.twoFactorEnabled || false}
        onSetupComplete={() => {
          // Settings will be refetched automatically by the hook
        }}
        onDisableComplete={() => {
          // Settings will be refetched automatically by the hook
        }}
      />

      {/* Active Sessions */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>Active Sessions</CardTitle>
            <CardDescription>
              Devices where you're currently logged in
            </CardDescription>
          </div>
          <Button
            variant="outline"
            onClick={() => {
              if (confirm("This will log you out from all devices. Continue?")) {
                logoutAllSessions();
              }
            }}
            disabled={isLoggingOutAll}
          >
            {isLoggingOutAll ? (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            ) : (
              <LogOut className="mr-2 h-4 w-4" />
            )}
            Logout All
          </Button>
        </CardHeader>
        <CardContent>
          {settings?.loginHistory && settings.loginHistory.length > 0 ? (
            <div className="space-y-4">
              {settings.loginHistory.map((session) => (
                <div
                  key={session.id}
                  className="flex items-center justify-between py-2 border-b last:border-0"
                >
                  <div className="flex items-center gap-3">
                    {session.deviceInfo.toLowerCase().includes("mobile") ||
                    session.deviceInfo.toLowerCase().includes("iphone") ? (
                      <Smartphone className="h-5 w-5 text-muted-foreground" />
                    ) : (
                      <Monitor className="h-5 w-5 text-muted-foreground" />
                    )}
                    <div>
                      <div className="flex items-center gap-2">
                        <p className="font-medium">{session.deviceInfo}</p>
                        {session.isCurrent && (
                          <Badge variant="success">Current</Badge>
                        )}
                      </div>
                      <p className="text-sm text-muted-foreground">
                        {session.location} - {session.ipAddress}
                      </p>
                    </div>
                  </div>
                  <div className="text-right text-sm text-muted-foreground">
                    {formatRelativeTime(session.loginAt)}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8 text-muted-foreground">
              No active sessions found
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
