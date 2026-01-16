import apiClient from "./client";
import type { ApiResponse, AnalyticsData, AnalyticsFilters } from "@/types";

export async function getAnalytics(
  filters: AnalyticsFilters
): Promise<ApiResponse<AnalyticsData>> {
  const response = await apiClient.get<ApiResponse<AnalyticsData>>(
    "/seller/analytics",
    { params: filters }
  );
  return response.data;
}

// Mock data generator for development
export function getMockAnalyticsData(dateRange: string): AnalyticsData {
  const days = dateRange === "7d" ? 7 : dateRange === "30d" ? 30 : dateRange === "90d" ? 90 : 365;

  const generateDates = (count: number) => {
    const dates = [];
    const today = new Date();
    for (let i = count - 1; i >= 0; i--) {
      const date = new Date(today);
      date.setDate(date.getDate() - i);
      dates.push(date.toISOString().split("T")[0]);
    }
    return dates;
  };

  const dates = generateDates(Math.min(days, 30));

  const revenueData = dates.map((date) => ({
    date,
    revenue: Math.floor(Math.random() * 50000) + 10000,
    orders: Math.floor(Math.random() * 30) + 5,
  }));

  const orderTrends = dates.map((date) => ({
    date,
    placed: Math.floor(Math.random() * 20) + 5,
    confirmed: Math.floor(Math.random() * 18) + 4,
    delivered: Math.floor(Math.random() * 15) + 3,
    cancelled: Math.floor(Math.random() * 3),
  }));

  const topProducts = [
    { id: "1", name: "Fresh Chicken - Whole", sku: "CHK-001", quantitySold: 450, revenue: 112500, orderCount: 180 },
    { id: "2", name: "Chicken Breast - Boneless", sku: "CHK-002", quantitySold: 380, revenue: 95000, orderCount: 152 },
    { id: "3", name: "Chicken Drumsticks", sku: "CHK-003", quantitySold: 320, revenue: 64000, orderCount: 128 },
    { id: "4", name: "Chicken Wings", sku: "CHK-004", quantitySold: 280, revenue: 42000, orderCount: 112 },
    { id: "5", name: "Chicken Liver", sku: "CHK-005", quantitySold: 150, revenue: 18750, orderCount: 60 },
  ];

  const customerInsights = {
    totalCustomers: 245,
    newCustomers: 32,
    repeatCustomers: 156,
    averageOrderValue: 1850,
    topBuyers: [
      { id: "1", name: "Ram Kumar Restaurant", totalOrders: 45, totalSpent: 125000, lastOrderDate: new Date().toISOString() },
      { id: "2", name: "Sharma Dhaba", totalOrders: 38, totalSpent: 98000, lastOrderDate: new Date().toISOString() },
      { id: "3", name: "Krishna Hotel", totalOrders: 32, totalSpent: 87500, lastOrderDate: new Date().toISOString() },
      { id: "4", name: "Gupta Caterers", totalOrders: 28, totalSpent: 72000, lastOrderDate: new Date().toISOString() },
      { id: "5", name: "Singh Meat Shop", totalOrders: 25, totalSpent: 65000, lastOrderDate: new Date().toISOString() },
    ],
  };

  const totalRevenue = revenueData.reduce((sum, d) => sum + d.revenue, 0);
  const totalOrders = revenueData.reduce((sum, d) => sum + d.orders, 0);

  return {
    summary: {
      totalRevenue,
      totalOrders,
      averageOrderValue: Math.floor(totalRevenue / totalOrders),
      revenueGrowth: 12.5,
      orderGrowth: 8.3,
      conversionRate: 68.5,
    },
    revenueData,
    orderTrends,
    topProducts,
    customerInsights,
  };
}
