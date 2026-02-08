import apiClient from "./client";
import type {
  ApiResponse,
  PaginatedResponse,
  Settlement,
  SettlementFilters,
  SettlementSummary,
  BankAccount,
} from "@/types";

export async function getSettlements(
  filters?: SettlementFilters
): Promise<ApiResponse<PaginatedResponse<Settlement>>> {
  const response = await apiClient.get<ApiResponse<PaginatedResponse<Settlement>>>(
    "/seller/settlements",
    { params: filters }
  );
  return response.data;
}

export async function getSettlement(settlementId: string): Promise<ApiResponse<Settlement>> {
  const response = await apiClient.get<ApiResponse<Settlement>>(
    `/seller/settlements/${settlementId}`
  );
  return response.data;
}

export async function getSettlementSummary(): Promise<ApiResponse<SettlementSummary>> {
  const response = await apiClient.get<ApiResponse<SettlementSummary>>(
    "/seller/settlements/summary"
  );
  return response.data;
}

// Wallet types
export interface WalletBalance {
  availableBalance: number;
  pendingBalance: number;
  totalEarnings: number;
  totalWithdrawn: number;
  lastUpdated: string;
}

export interface WithdrawalRequest {
  amount: number;
  bankAccountId: string;
}

export interface WithdrawalResponse {
  id: string;
  amount: number;
  status: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
  bankAccountId: string;
  bankName: string;
  accountLast4: string;
  requestedAt: string;
  estimatedCompletionDate: string;
}

export async function getWalletBalance(): Promise<ApiResponse<WalletBalance>> {
  const response = await apiClient.get<ApiResponse<WalletBalance>>(
    "/seller/wallet"
  );
  return response.data;
}

export async function requestWithdrawal(
  request: WithdrawalRequest
): Promise<ApiResponse<WithdrawalResponse>> {
  const response = await apiClient.post<ApiResponse<WithdrawalResponse>>(
    "/seller/settlements/withdraw",
    request
  );
  return response.data;
}

export async function getBankAccounts(): Promise<ApiResponse<BankAccount[]>> {
  const response = await apiClient.get<ApiResponse<BankAccount[]>>(
    "/seller/bank-accounts"
  );
  return response.data;
}

export async function exportSettlement(
  settlementId: string,
  format: "pdf" | "csv"
): Promise<Blob> {
  const response = await apiClient.get(
    `/seller/settlements/${settlementId}/export`,
    {
      params: { format },
      responseType: "blob",
    }
  );
  return response.data;
}

