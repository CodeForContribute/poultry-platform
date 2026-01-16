// Analytics Types
export interface RevenueData {
  date: string;
  revenue: number;
  orders: number;
}

export interface OrderTrendData {
  date: string;
  placed: number;
  confirmed: number;
  delivered: number;
  cancelled: number;
}

export interface TopProduct {
  id: string;
  name: string;
  sku: string;
  quantitySold: number;
  revenue: number;
  orderCount: number;
}

export interface CustomerInsight {
  totalCustomers: number;
  newCustomers: number;
  repeatCustomers: number;
  averageOrderValue: number;
  topBuyers: TopBuyer[];
}

export interface TopBuyer {
  id: string;
  name: string;
  totalOrders: number;
  totalSpent: number;
  lastOrderDate: string;
}

export interface AnalyticsSummary {
  totalRevenue: number;
  totalOrders: number;
  averageOrderValue: number;
  revenueGrowth: number;
  orderGrowth: number;
  conversionRate: number;
}

export interface AnalyticsData {
  summary: AnalyticsSummary;
  revenueData: RevenueData[];
  orderTrends: OrderTrendData[];
  topProducts: TopProduct[];
  customerInsights: CustomerInsight;
}

export type DateRange = "7d" | "30d" | "90d" | "12m" | "custom";

export interface AnalyticsFilters {
  dateRange: DateRange;
  startDate?: string;
  endDate?: string;
}
