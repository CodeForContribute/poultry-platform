"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { subDays } from "date-fns";
import type { Settlement, SettlementStatus, BankAccount } from "@/types";
import * as settlementsApi from "@/lib/api/settlements";
import type { WalletBalance, WithdrawalRequest } from "@/lib/api/settlements";

export interface SettlementsViewModel {
  // State
  statusFilter: SettlementStatus | "ALL";
  startDate: Date | undefined;
  endDate: Date | undefined;

  // Data
  settlements: Settlement[];
  walletBalance: WalletBalance | undefined;
  bankAccounts: BankAccount[];
  isLoading: boolean;
  isLoadingWallet: boolean;
  isLoadingBankAccounts: boolean;
  error: Error | null;

  // Actions
  setStatusFilter: (status: SettlementStatus | "ALL") => void;
  setDateRange: (start: Date | undefined, end: Date | undefined) => void;
  requestWithdrawal: (request: WithdrawalRequest) => void;
  isWithdrawing: boolean;
  refetch: () => void;
}

export function useSettlements(): SettlementsViewModel {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState<SettlementStatus | "ALL">("ALL");
  const [startDate, setStartDate] = useState<Date | undefined>(subDays(new Date(), 30));
  const [endDate, setEndDate] = useState<Date | undefined>(new Date());

  // Fetch settlements
  const {
    data: settlements,
    isLoading,
    error,
    refetch,
  } = useQuery<Settlement[], Error>({
    queryKey: ["seller-settlements", statusFilter, startDate?.toISOString(), endDate?.toISOString()],
    queryFn: async () => {
      // Use mock data for now - replace with real API when backend ready
      await new Promise((resolve) => setTimeout(resolve, 300));
      let data = settlementsApi.getMockSettlements();

      // Apply filters
      if (statusFilter !== "ALL") {
        data = data.filter((s) => s.status === statusFilter);
      }
      if (startDate) {
        data = data.filter((s) => new Date(s.createdAt) >= startDate);
      }
      if (endDate) {
        data = data.filter((s) => new Date(s.createdAt) <= endDate);
      }

      return data;
    },
  });

  // Fetch wallet balance
  const {
    data: walletBalance,
    isLoading: isLoadingWallet,
  } = useQuery<WalletBalance, Error>({
    queryKey: ["seller-wallet"],
    queryFn: async () => {
      await new Promise((resolve) => setTimeout(resolve, 200));
      return settlementsApi.getMockWalletBalance();
    },
  });

  // Fetch bank accounts
  const {
    data: bankAccounts,
    isLoading: isLoadingBankAccounts,
  } = useQuery<BankAccount[], Error>({
    queryKey: ["seller-bank-accounts-settlements"],
    queryFn: async () => {
      await new Promise((resolve) => setTimeout(resolve, 200));
      return settlementsApi.getMockBankAccounts();
    },
  });

  // Withdrawal mutation
  const withdrawMutation = useMutation({
    mutationFn: (request: WithdrawalRequest) => settlementsApi.requestWithdrawal(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-wallet"] });
      queryClient.invalidateQueries({ queryKey: ["seller-settlements"] });
      toast.success("Withdrawal request submitted successfully");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to submit withdrawal request");
    },
  });

  const handleSetDateRange = (start: Date | undefined, end: Date | undefined) => {
    setStartDate(start);
    setEndDate(end);
  };

  return {
    statusFilter,
    startDate,
    endDate,
    settlements: settlements ?? [],
    walletBalance,
    bankAccounts: bankAccounts ?? [],
    isLoading,
    isLoadingWallet,
    isLoadingBankAccounts,
    error: error as Error | null,
    setStatusFilter,
    setDateRange: handleSetDateRange,
    requestWithdrawal: withdrawMutation.mutate,
    isWithdrawing: withdrawMutation.isPending,
    refetch,
  };
}
