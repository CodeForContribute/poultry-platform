// Settlement Types
export interface Settlement {
  id: string;
  settlementNumber: string;
  sellerId: string;
  status: SettlementStatus;
  periodStart: string;
  periodEnd: string;
  totalOrders: number;
  grossAmount: number;
  platformFee: number;
  gstOnPlatformFee: number;
  tdsAmount: number;
  adjustments: number;
  netAmount: number;
  bankAccountId: string;
  bankAccountLast4: string;
  bankName: string;
  paymentReference?: string;
  paidAt?: string;
  orders: SettlementOrderSummary[];
  createdAt: string;
  updatedAt: string;
}

export type SettlementStatus =
  | "PENDING"
  | "PROCESSING"
  | "COMPLETED"
  | "FAILED"
  | "ON_HOLD";

export interface SettlementOrderSummary {
  orderId: string;
  orderNumber: string;
  orderDate: string;
  deliveredAt: string;
  orderAmount: number;
  platformFee: number;
  netAmount: number;
}

export interface SettlementSummary {
  totalSettlements: number;
  totalPaid: number;
  pendingAmount: number;
  lastSettlementDate?: string;
  nextSettlementDate?: string;
}

export interface BankAccount {
  id: string;
  accountHolderName: string;
  bankName: string;
  accountNumberLast4: string;
  ifscCode: string;
  accountType: "SAVINGS" | "CURRENT";
  isPrimary: boolean;
  isVerified: boolean;
  createdAt: string;
}

export interface SettlementFilters {
  status?: SettlementStatus | "ALL";
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}
