"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import type {
  BusinessProfile,
  BankAccountDetails,
  NotificationPreferences,
  SecuritySettings,
  UpdateBusinessProfileRequest,
  AddBankAccountRequest,
  ChangePasswordRequest,
} from "@/types";
import * as settingsApi from "@/lib/api/settings";

export function useBusinessProfile() {
  const queryClient = useQueryClient();

  const {
    data: profile,
    isLoading,
    error,
  } = useQuery<BusinessProfile, Error>({
    queryKey: ["seller-profile"],
    queryFn: async () => {
      // Use mock data for now
      await new Promise((resolve) => setTimeout(resolve, 300));
      return settingsApi.getMockBusinessProfile();
    },
  });

  const updateMutation = useMutation({
    mutationFn: (data: UpdateBusinessProfileRequest) =>
      settingsApi.updateBusinessProfile(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-profile"] });
      toast.success("Business profile updated successfully");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to update profile");
    },
  });

  return {
    profile,
    isLoading,
    error,
    updateProfile: updateMutation.mutate,
    isUpdating: updateMutation.isPending,
  };
}

export function useBankAccounts() {
  const queryClient = useQueryClient();

  const {
    data: accounts,
    isLoading,
    error,
  } = useQuery<BankAccountDetails[], Error>({
    queryKey: ["seller-bank-accounts"],
    queryFn: async () => {
      await new Promise((resolve) => setTimeout(resolve, 300));
      return settingsApi.getMockBankAccounts();
    },
  });

  const addMutation = useMutation({
    mutationFn: (data: AddBankAccountRequest) => settingsApi.addBankAccount(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-bank-accounts"] });
      toast.success("Bank account added successfully");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to add bank account");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (accountId: string) => settingsApi.deleteBankAccount(accountId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-bank-accounts"] });
      toast.success("Bank account removed");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to remove bank account");
    },
  });

  const setPrimaryMutation = useMutation({
    mutationFn: (accountId: string) => settingsApi.setPrimaryBankAccount(accountId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-bank-accounts"] });
      toast.success("Primary account updated");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to set primary account");
    },
  });

  return {
    accounts,
    isLoading,
    error,
    addAccount: addMutation.mutate,
    isAdding: addMutation.isPending,
    deleteAccount: deleteMutation.mutate,
    isDeleting: deleteMutation.isPending,
    setPrimaryAccount: setPrimaryMutation.mutate,
    isSettingPrimary: setPrimaryMutation.isPending,
  };
}

export function useNotificationPreferences() {
  const queryClient = useQueryClient();

  const {
    data: preferences,
    isLoading,
    error,
  } = useQuery<NotificationPreferences, Error>({
    queryKey: ["seller-notification-preferences"],
    queryFn: async () => {
      await new Promise((resolve) => setTimeout(resolve, 300));
      return settingsApi.getMockNotificationPreferences();
    },
  });

  const updateMutation = useMutation({
    mutationFn: (data: NotificationPreferences) =>
      settingsApi.updateNotificationPreferences(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-notification-preferences"] });
      toast.success("Notification preferences updated");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to update preferences");
    },
  });

  return {
    preferences,
    isLoading,
    error,
    updatePreferences: updateMutation.mutate,
    isUpdating: updateMutation.isPending,
  };
}

export function useSecuritySettings() {
  const queryClient = useQueryClient();

  const {
    data: settings,
    isLoading,
    error,
  } = useQuery<SecuritySettings, Error>({
    queryKey: ["seller-security-settings"],
    queryFn: async () => {
      await new Promise((resolve) => setTimeout(resolve, 300));
      return settingsApi.getMockSecuritySettings();
    },
  });

  const changePasswordMutation = useMutation({
    mutationFn: (data: ChangePasswordRequest) => settingsApi.changePassword(data),
    onSuccess: () => {
      toast.success("Password changed successfully");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to change password");
    },
  });

  const logoutAllMutation = useMutation({
    mutationFn: () => settingsApi.logoutAllSessions(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-security-settings"] });
      toast.success("All sessions have been logged out");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to logout sessions");
    },
  });

  return {
    settings,
    isLoading,
    error,
    changePassword: changePasswordMutation.mutate,
    isChangingPassword: changePasswordMutation.isPending,
    logoutAllSessions: logoutAllMutation.mutate,
    isLoggingOutAll: logoutAllMutation.isPending,
  };
}
