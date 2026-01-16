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

// Mock data for development
export function getMockSettlements(): Settlement[] {
  const today = new Date();

  return [
    {
      id: "1",
      settlementNumber: "STL-2024-0012",
      sellerId: "seller-1",
      status: "COMPLETED",
      periodStart: new Date(today.getFullYear(), today.getMonth(), 1).toISOString(),
      periodEnd: new Date(today.getFullYear(), today.getMonth(), 7).toISOString(),
      totalOrders: 45,
      grossAmount: 125000,
      platformFee: 6250,
      gstOnPlatformFee: 1125,
      tdsAmount: 1250,
      adjustments: 0,
      netAmount: 116375,
      bankAccountId: "bank-1",
      bankAccountLast4: "4523",
      bankName: "HDFC Bank",
      paymentReference: "UTR123456789",
      paidAt: new Date(today.getFullYear(), today.getMonth(), 9).toISOString(),
      orders: [
        { orderId: "1", orderNumber: "ORD-2024-0145", orderDate: new Date().toISOString(), deliveredAt: new Date().toISOString(), orderAmount: 4500, platformFee: 225, netAmount: 4275 },
        { orderId: "2", orderNumber: "ORD-2024-0144", orderDate: new Date().toISOString(), deliveredAt: new Date().toISOString(), orderAmount: 3200, platformFee: 160, netAmount: 3040 },
        { orderId: "3", orderNumber: "ORD-2024-0143", orderDate: new Date().toISOString(), deliveredAt: new Date().toISOString(), orderAmount: 5800, platformFee: 290, netAmount: 5510 },
      ],
      createdAt: new Date(today.getFullYear(), today.getMonth(), 8).toISOString(),
      updatedAt: new Date(today.getFullYear(), today.getMonth(), 9).toISOString(),
    },
    {
      id: "2",
      settlementNumber: "STL-2024-0011",
      sellerId: "seller-1",
      status: "COMPLETED",
      periodStart: new Date(today.getFullYear(), today.getMonth() - 1, 24).toISOString(),
      periodEnd: new Date(today.getFullYear(), today.getMonth() - 1, 30).toISOString(),
      totalOrders: 52,
      grossAmount: 142000,
      platformFee: 7100,
      gstOnPlatformFee: 1278,
      tdsAmount: 1420,
      adjustments: -500,
      netAmount: 131702,
      bankAccountId: "bank-1",
      bankAccountLast4: "4523",
      bankName: "HDFC Bank",
      paymentReference: "UTR123456788",
      paidAt: new Date(today.getFullYear(), today.getMonth(), 2).toISOString(),
      orders: [],
      createdAt: new Date(today.getFullYear(), today.getMonth(), 1).toISOString(),
      updatedAt: new Date(today.getFullYear(), today.getMonth(), 2).toISOString(),
    },
    {
      id: "3",
      settlementNumber: "STL-2024-0013",
      sellerId: "seller-1",
      status: "PENDING",
      periodStart: new Date(today.getFullYear(), today.getMonth(), 8).toISOString(),
      periodEnd: new Date(today.getFullYear(), today.getMonth(), 14).toISOString(),
      totalOrders: 38,
      grossAmount: 98500,
      platformFee: 4925,
      gstOnPlatformFee: 886,
      tdsAmount: 985,
      adjustments: 0,
      netAmount: 91704,
      bankAccountId: "bank-1",
      bankAccountLast4: "4523",
      bankName: "HDFC Bank",
      orders: [],
      createdAt: new Date(today.getFullYear(), today.getMonth(), 15).toISOString(),
      updatedAt: new Date(today.getFullYear(), today.getMonth(), 15).toISOString(),
    },
  ];
}

export function getMockSettlementSummary(): SettlementSummary {
  return {
    totalSettlements: 12,
    totalPaid: 1450000,
    pendingAmount: 91704,
    lastSettlementDate: new Date().toISOString(),
    nextSettlementDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
  };
}

export function getMockBankAccounts(): BankAccount[] {
  return [
    {
      id: "bank-1",
      accountHolderName: "Poultry Farm Pvt Ltd",
      bankName: "HDFC Bank",
      accountNumberLast4: "4523",
      ifscCode: "HDFC0001234",
      accountType: "CURRENT",
      isPrimary: true,
      isVerified: true,
      createdAt: new Date().toISOString(),
    },
  ];
}
