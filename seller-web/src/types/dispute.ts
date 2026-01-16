// Dispute Types
export interface Dispute {
  id: string;
  disputeNumber: string;
  orderId: string;
  orderNumber: string;
  buyerId: string;
  buyerName: string;
  sellerId: string;
  type: DisputeType;
  status: DisputeStatus;
  priority: DisputePriority;
  reason: string;
  description: string;
  requestedResolution: string;
  resolution?: string;
  refundAmount?: number;
  attachments: DisputeAttachment[];
  messages: DisputeMessage[];
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
  resolvedBy?: string;
}

export type DisputeType =
  | "QUALITY_ISSUE"
  | "QUANTITY_MISMATCH"
  | "WRONG_PRODUCT"
  | "DAMAGED_PRODUCT"
  | "LATE_DELIVERY"
  | "NON_DELIVERY"
  | "PAYMENT_ISSUE"
  | "OTHER";

export type DisputeStatus =
  | "OPEN"
  | "UNDER_REVIEW"
  | "WAITING_SELLER_RESPONSE"
  | "WAITING_BUYER_RESPONSE"
  | "ESCALATED"
  | "RESOLVED"
  | "CLOSED";

export type DisputePriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

export interface DisputeAttachment {
  id: string;
  fileName: string;
  fileUrl: string;
  fileType: string;
  fileSize: number;
  uploadedAt: string;
  uploadedBy: string;
}

export interface DisputeMessage {
  id: string;
  disputeId: string;
  senderId: string;
  senderName: string;
  senderType: "BUYER" | "SELLER" | "ADMIN";
  message: string;
  attachments: DisputeAttachment[];
  createdAt: string;
  isInternal: boolean;
}

export interface CreateDisputeResponseRequest {
  message: string;
  proposedResolution?: string;
  refundAmount?: number;
  attachments?: File[];
}

export interface DisputeFilters {
  status?: DisputeStatus | "ALL";
  type?: DisputeType | "ALL";
  priority?: DisputePriority | "ALL";
  page?: number;
  size?: number;
}
