"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Package, ShoppingCart, IndianRupee, TrendingUp, Loader2 } from "lucide-react";
import { Header } from "@/components/layout/header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { formatCurrency } from "@/lib/utils";
import * as dashboardApi from "@/lib/api/dashboard";
import * as ordersApi from "@/lib/api/orders";
import type { Order } from "@/types";

export default function DashboardPage() {
  // Fetch dashboard stats
  const {
    data: dashboardData,
    isLoading: isDashboardLoading,
    error: dashboardError,
  } = useQuery({
    queryKey: ["seller-dashboard"],
    queryFn: async () => {
      const response = await dashboardApi.getSellerDashboard();
      if (response.success && response.data) {
        return response.data;
      }
      throw new Error(response.message || "Failed to load dashboard");
    },
  });

  // Fetch recent orders
  const {
    data: recentOrdersData,
    isLoading: isOrdersLoading,
  } = useQuery({
    queryKey: ["seller-orders", "recent"],
    queryFn: async () => {
      const response = await ordersApi.getOrders({ size: 5 });
      if (response.success && response.data) {
        return response.data.content;
      }
      return [];
    },
  });

  const stats = [
    {
      title: "Total Products",
      value: dashboardData?.totalProducts ?? 0,
      change: `${dashboardData?.activeProducts ?? 0} active`,
      icon: Package,
    },
    {
      title: "Pending Orders",
      value: dashboardData?.pendingOrders ?? 0,
      change: "Need confirmation",
      icon: ShoppingCart,
    },
    {
      title: "Today's Revenue",
      value: formatCurrency(dashboardData?.todayRevenue ?? 0),
      change: `${dashboardData?.todayOrders ?? 0} orders today`,
      icon: IndianRupee,
    },
    {
      title: "Total Revenue",
      value: formatCurrency(dashboardData?.totalRevenue ?? 0),
      change: "All time",
      icon: TrendingUp,
    },
  ];

  const isLoading = isDashboardLoading;
  const recentOrders = recentOrdersData ?? [];

  return (
    <div>
      <Header title="Dashboard" />

      <div className="p-6 space-y-6">
        {/* Loading state */}
        {isLoading && (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {/* Error state */}
        {dashboardError && (
          <Card>
            <CardContent className="p-8 text-center text-destructive">
              {dashboardError instanceof Error
                ? dashboardError.message
                : "Failed to load dashboard"}
            </CardContent>
          </Card>
        )}

        {/* Stats Grid */}
        {!isLoading && !dashboardError && (
          <>
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
              {stats.map((stat) => (
                <Card key={stat.title}>
                  <CardHeader className="flex flex-row items-center justify-between pb-2">
                    <CardTitle className="text-sm font-medium text-muted-foreground">
                      {stat.title}
                    </CardTitle>
                    <stat.icon className="h-4 w-4 text-muted-foreground" />
                  </CardHeader>
                  <CardContent>
                    <div className="text-2xl font-bold">{stat.value}</div>
                    <p className="text-xs text-muted-foreground">{stat.change}</p>
                  </CardContent>
                </Card>
              ))}
            </div>

            {/* Recent Orders */}
            <Card>
              <CardHeader className="flex flex-row items-center justify-between">
                <CardTitle>Recent Orders</CardTitle>
                <Link href="/orders">
                  <Button variant="outline" size="sm">
                    View All
                  </Button>
                </Link>
              </CardHeader>
              <CardContent>
                {isOrdersLoading ? (
                  <div className="flex items-center justify-center py-8">
                    <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                  </div>
                ) : recentOrders.length === 0 ? (
                  <div className="text-sm text-muted-foreground">
                    No recent orders to display. Orders will appear here once buyers
                    place them.
                  </div>
                ) : (
                  <div className="space-y-4">
                    {recentOrders.map((order: Order) => (
                      <div
                        key={order.id}
                        className="flex items-center justify-between border-b pb-4 last:border-0 last:pb-0"
                      >
                        <div>
                          <p className="font-medium">{order.orderNumber}</p>
                          <p className="text-sm text-muted-foreground">
                            {order.buyerName} - {order.items?.length || 0} items
                          </p>
                        </div>
                        <div className="text-right">
                          <p className="font-medium">
                            {formatCurrency(order.totalAmount)}
                          </p>
                          <p className="text-xs text-muted-foreground">
                            {order.status.replace(/_/g, " ")}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </>
        )}
      </div>
    </div>
  );
}
