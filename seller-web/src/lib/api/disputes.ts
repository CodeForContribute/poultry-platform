import apiClient from "./client";
import type {
  ApiResponse,
  PaginatedResponse,
  Dispute,
  DisputeFilters,
  CreateDisputeResponseRequest,
  DisputeMessage,
} from "@/types";

export async function getDisputes(
  filters?: DisputeFilters
): Promise<ApiResponse<PaginatedResponse<Dispute>>> {
  const response = await apiClient.get<ApiResponse<PaginatedResponse<Dispute>>>(
    "/seller/disputes",
    { params: filters }
  );
  return response.data;
}

export async function getDispute(disputeId: string): Promise<ApiResponse<Dispute>> {
  const response = await apiClient.get<ApiResponse<Dispute>>(
    `/seller/disputes/${disputeId}`
  );
  return response.data;
}

export async function respondToDispute(
  disputeId: string,
  data: CreateDisputeResponseRequest
): Promise<ApiResponse<DisputeMessage>> {
  const response = await apiClient.post<ApiResponse<DisputeMessage>>(
    `/seller/disputes/${disputeId}/respond`,
    data
  );
  return response.data;
}

export async function acceptResolution(
  disputeId: string
): Promise<ApiResponse<Dispute>> {
  const response = await apiClient.post<ApiResponse<Dispute>>(
    `/seller/disputes/${disputeId}/accept`
  );
  return response.data;
}

export async function proposeResolution(
  disputeId: string,
  resolution: string,
  refundAmount?: number
): Promise<ApiResponse<Dispute>> {
  const response = await apiClient.post<ApiResponse<Dispute>>(
    `/seller/disputes/${disputeId}/propose-resolution`,
    { resolution, refundAmount }
  );
  return response.data;
}

// Mock data for development
export function getMockDisputes(): Dispute[] {
  return [
    {
      id: "1",
      disputeNumber: "DSP-2024-001",
      orderId: "ord-1",
      orderNumber: "ORD-2024-0145",
      buyerId: "buyer-1",
      buyerName: "Ram Kumar Restaurant",
      sellerId: "seller-1",
      type: "QUALITY_ISSUE",
      status: "WAITING_SELLER_RESPONSE",
      priority: "HIGH",
      reason: "Quality Issue",
      description: "The chicken delivered was not fresh. It had an unusual smell and color.",
      requestedResolution: "Full refund or replacement",
      attachments: [],
      messages: [
        {
          id: "msg-1",
          disputeId: "1",
          senderId: "buyer-1",
          senderName: "Ram Kumar",
          senderType: "BUYER",
          message: "The chicken delivered was not fresh. It had an unusual smell and color. Please help resolve this issue.",
          attachments: [],
          createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
          isInternal: false,
        },
      ],
      createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
      updatedAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
    },
    {
      id: "2",
      disputeNumber: "DSP-2024-002",
      orderId: "ord-2",
      orderNumber: "ORD-2024-0142",
      buyerId: "buyer-2",
      buyerName: "Sharma Dhaba",
      sellerId: "seller-1",
      type: "QUANTITY_MISMATCH",
      status: "UNDER_REVIEW",
      priority: "MEDIUM",
      reason: "Quantity Mismatch",
      description: "Ordered 10kg but received only 8kg of chicken.",
      requestedResolution: "Deliver remaining 2kg or refund",
      attachments: [],
      messages: [
        {
          id: "msg-2",
          disputeId: "2",
          senderId: "buyer-2",
          senderName: "Rahul Sharma",
          senderType: "BUYER",
          message: "I ordered 10kg chicken but received only 8kg. The weighing slip shows 8kg. Please check.",
          attachments: [],
          createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
          isInternal: false,
        },
        {
          id: "msg-3",
          disputeId: "2",
          senderId: "seller-1",
          senderName: "Seller",
          senderType: "SELLER",
          message: "We apologize for the inconvenience. We are checking our dispatch records.",
          attachments: [],
          createdAt: new Date(Date.now() - 20 * 60 * 60 * 1000).toISOString(),
          isInternal: false,
        },
      ],
      createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
      updatedAt: new Date(Date.now() - 20 * 60 * 60 * 1000).toISOString(),
    },
    {
      id: "3",
      disputeNumber: "DSP-2024-003",
      orderId: "ord-3",
      orderNumber: "ORD-2024-0138",
      buyerId: "buyer-3",
      buyerName: "Krishna Hotel",
      sellerId: "seller-1",
      type: "LATE_DELIVERY",
      status: "RESOLVED",
      priority: "LOW",
      reason: "Late Delivery",
      description: "Order was delivered 3 hours late.",
      requestedResolution: "Compensation for late delivery",
      resolution: "10% discount on next order",
      attachments: [],
      messages: [],
      createdAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString(),
      updatedAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
      resolvedAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
      resolvedBy: "Admin",
    },
  ];
}
