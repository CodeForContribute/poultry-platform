import apiClient from "./client";
import type {
  ApiResponse,
  PaginatedResponse,
  Order,
  OrderStatus,
} from "@/types";

interface GetOrdersParams {
  status?: OrderStatus;
  page?: number;
  size?: number;
}

export async function getOrders(
  params?: GetOrdersParams
): Promise<ApiResponse<PaginatedResponse<Order>>> {
  const response = await apiClient.get<ApiResponse<PaginatedResponse<Order>>>(
    "/seller/orders",
    { params }
  );
  return response.data;
}

export async function getOrder(orderId: string): Promise<ApiResponse<Order>> {
  const response = await apiClient.get<ApiResponse<Order>>(
    `/seller/orders/${orderId}`
  );
  return response.data;
}

export async function confirmOrder(orderId: string): Promise<ApiResponse<Order>> {
  const response = await apiClient.post<ApiResponse<Order>>(
    `/seller/orders/${orderId}/confirm`
  );
  return response.data;
}

export async function rejectOrder(
  orderId: string,
  reason: string
): Promise<ApiResponse<Order>> {
  const response = await apiClient.post<ApiResponse<Order>>(
    `/seller/orders/${orderId}/reject`,
    { status: "SELLER_REJECTED", reason }
  );
  return response.data;
}

export async function dispatchOrder(orderId: string): Promise<ApiResponse<Order>> {
  const response = await apiClient.post<ApiResponse<Order>>(
    `/seller/orders/${orderId}/dispatch`,
    { status: "DISPATCHED" }
  );
  return response.data;
}

export async function deliverOrder(orderId: string): Promise<ApiResponse<Order>> {
  const response = await apiClient.post<ApiResponse<Order>>(
    `/seller/orders/${orderId}/deliver`,
    { status: "DELIVERED" }
  );
  return response.data;
}
