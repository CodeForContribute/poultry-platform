import apiClient from "./client";
import type { ApiResponse } from "@/types";

export interface SellerDashboard {
  totalProducts: number;
  activeProducts: number;
  pendingOrders: number;
  todayOrders: number;
  todayRevenue: number;
  totalRevenue: number;
}

export async function getSellerDashboard(): Promise<ApiResponse<SellerDashboard>> {
  const response = await apiClient.get<ApiResponse<SellerDashboard>>(
    "/seller/dashboard"
  );
  return response.data;
}
