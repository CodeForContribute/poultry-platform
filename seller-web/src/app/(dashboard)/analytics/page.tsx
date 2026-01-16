"use client";

import { useMemo } from "react";
import {
  TrendingUp,
  TrendingDown,
  ShoppingCart,
  IndianRupee,
  Users,
  BarChart3,
  Loader2,
  Calendar,
} from "lucide-react";
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
  PieChart,
  Pie,
  Cell,
} from "recharts";
import { format } from "date-fns";
import { Header } from "@/components/layout/header";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { formatCurrency } from "@/lib/utils";
import { useAnalytics } from "@/lib/hooks/useAnalytics";
import type { DateRange } from "@/types";

const dateRangeOptions: { label: string; value: DateRange }[] = [
  { label: "7 Days", value: "7d" },
  { label: "30 Days", value: "30d" },
  { label: "90 Days", value: "90d" },
  { label: "12 Months", value: "12m" },
];

const CHART_COLORS = ["#0ea5e9", "#22c55e", "#f59e0b", "#ef4444", "#8b5cf6"];

export default function AnalyticsPage() {
  const vm = useAnalytics();

  // Format chart data for revenue
  const revenueChartData = useMemo(() => {
    if (!vm.data?.revenueData) return [];
    return vm.data.revenueData.map((d) => ({
      ...d,
      date: format(new Date(d.date), "MMM d"),
      formattedRevenue: formatCurrency(d.revenue),
    }));
  }, [vm.data?.revenueData]);

  // Format chart data for order trends
  const orderTrendChartData = useMemo(() => {
    if (!vm.data?.orderTrends) return [];
    return vm.data.orderTrends.map((d) => ({
      ...d,
      date: format(new Date(d.date), "MMM d"),
    }));
  }, [vm.data?.orderTrends]);

  // Top products for pie chart
  const topProductsPieData = useMemo(() => {
    if (!vm.data?.topProducts) return [];
    return vm.data.topProducts.slice(0, 5).map((p, i) => ({
      name: p.name.length > 20 ? p.name.substring(0, 20) + "..." : p.name,
      value: p.revenue,
      color: CHART_COLORS[i],
    }));
  }, [vm.data?.topProducts]);

  const stats = [
    {
      title: "Total Revenue",
      value: formatCurrency(vm.data?.summary.totalRevenue ?? 0),
      change: vm.data?.summary.revenueGrowth ?? 0,
      changeLabel: "vs previous period",
      icon: IndianRupee,
      positive: (vm.data?.summary.revenueGrowth ?? 0) >= 0,
    },
    {
      title: "Total Orders",
      value: vm.data?.summary.totalOrders ?? 0,
      change: vm.data?.summary.orderGrowth ?? 0,
      changeLabel: "vs previous period",
      icon: ShoppingCart,
      positive: (vm.data?.summary.orderGrowth ?? 0) >= 0,
    },
    {
      title: "Average Order Value",
      value: formatCurrency(vm.data?.summary.averageOrderValue ?? 0),
      change: null,
      changeLabel: "Per order",
      icon: BarChart3,
      positive: true,
    },
    {
      title: "Total Customers",
      value: vm.data?.customerInsights.totalCustomers ?? 0,
      change: vm.data?.customerInsights.newCustomers ?? 0,
      changeLabel: "new this period",
      icon: Users,
      positive: true,
      isCount: true,
    },
  ];

  return (
    <div>
      <Header title="Analytics" />

      <div className="p-6 space-y-6">
        {/* Date Range Selector */}
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Calendar className="h-4 w-4" />
            <span>{vm.formattedPeriod}</span>
          </div>
          <div className="flex flex-wrap gap-2">
            {dateRangeOptions.map((option) => (
              <Button
                key={option.value}
                variant={vm.dateRange === option.value ? "default" : "outline"}
                size="sm"
                onClick={() => vm.setDateRange(option.value)}
              >
                {option.label}
              </Button>
            ))}
          </div>
        </div>

        {/* Loading state */}
        {vm.isLoading && (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {/* Error state */}
        {vm.error && (
          <Card>
            <CardContent className="p-8 text-center text-destructive">
              {vm.error.message || "Failed to load analytics"}
            </CardContent>
          </Card>
        )}

        {/* Analytics Content */}
        {!vm.isLoading && !vm.error && vm.data && (
          <>
            {/* Stats Grid */}
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
                    <div className="flex items-center gap-1 text-xs">
                      {stat.change !== null && !stat.isCount && (
                        <>
                          {stat.positive ? (
                            <TrendingUp className="h-3 w-3 text-green-500" />
                          ) : (
                            <TrendingDown className="h-3 w-3 text-red-500" />
                          )}
                          <span
                            className={
                              stat.positive ? "text-green-600" : "text-red-600"
                            }
                          >
                            {stat.change > 0 ? "+" : ""}
                            {stat.change}%
                          </span>
                        </>
                      )}
                      {stat.isCount && stat.change !== null && (
                        <span className="text-green-600">+{stat.change}</span>
                      )}
                      <span className="text-muted-foreground">
                        {stat.changeLabel}
                      </span>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>

            {/* Revenue Chart */}
            <Card>
              <CardHeader>
                <CardTitle>Revenue Overview</CardTitle>
                <CardDescription>Daily revenue for the selected period</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="h-[300px] sm:h-[350px]">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={revenueChartData}>
                      <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                      <XAxis
                        dataKey="date"
                        tick={{ fontSize: 12 }}
                        tickLine={false}
                        axisLine={false}
                      />
                      <YAxis
                        tick={{ fontSize: 12 }}
                        tickLine={false}
                        axisLine={false}
                        tickFormatter={(value) => `${(value / 1000).toFixed(0)}k`}
                      />
                      <Tooltip
                        content={({ active, payload, label }) => {
                          if (active && payload && payload.length) {
                            return (
                              <div className="rounded-lg border bg-background p-3 shadow-md">
                                <p className="font-medium">{label}</p>
                                <p className="text-sm text-muted-foreground">
                                  Revenue: {formatCurrency(payload[0].value as number)}
                                </p>
                                <p className="text-sm text-muted-foreground">
                                  Orders: {payload[1]?.value}
                                </p>
                              </div>
                            );
                          }
                          return null;
                        }}
                      />
                      <Legend />
                      <Line
                        type="monotone"
                        dataKey="revenue"
                        name="Revenue"
                        stroke="#0ea5e9"
                        strokeWidth={2}
                        dot={false}
                        activeDot={{ r: 4 }}
                      />
                      <Line
                        type="monotone"
                        dataKey="orders"
                        name="Orders"
                        stroke="#22c55e"
                        strokeWidth={2}
                        dot={false}
                        activeDot={{ r: 4 }}
                        yAxisId={0}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              </CardContent>
            </Card>

            {/* Order Trends Chart */}
            <Card>
              <CardHeader>
                <CardTitle>Order Trends</CardTitle>
                <CardDescription>Order status breakdown over time</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="h-[300px] sm:h-[350px]">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={orderTrendChartData}>
                      <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                      <XAxis
                        dataKey="date"
                        tick={{ fontSize: 12 }}
                        tickLine={false}
                        axisLine={false}
                      />
                      <YAxis
                        tick={{ fontSize: 12 }}
                        tickLine={false}
                        axisLine={false}
                      />
                      <Tooltip
                        content={({ active, payload, label }) => {
                          if (active && payload && payload.length) {
                            return (
                              <div className="rounded-lg border bg-background p-3 shadow-md">
                                <p className="font-medium mb-2">{label}</p>
                                {payload.map((item, index) => (
                                  <p
                                    key={index}
                                    className="text-sm"
                                    style={{ color: item.color }}
                                  >
                                    {item.name}: {item.value}
                                  </p>
                                ))}
                              </div>
                            );
                          }
                          return null;
                        }}
                      />
                      <Legend />
                      <Bar dataKey="placed" name="Placed" fill="#f59e0b" radius={[4, 4, 0, 0]} />
                      <Bar dataKey="confirmed" name="Confirmed" fill="#0ea5e9" radius={[4, 4, 0, 0]} />
                      <Bar dataKey="delivered" name="Delivered" fill="#22c55e" radius={[4, 4, 0, 0]} />
                      <Bar dataKey="cancelled" name="Cancelled" fill="#ef4444" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </CardContent>
            </Card>

            {/* Two Column Grid for Products and Customers */}
            <div className="grid gap-6 lg:grid-cols-2">
              {/* Top Products */}
              <Card>
                <CardHeader>
                  <CardTitle>Top Products by Sales</CardTitle>
                  <CardDescription>Best performing products this period</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="flex flex-col lg:flex-row gap-6">
                    {/* Pie Chart */}
                    <div className="h-[200px] w-full lg:w-1/2">
                      <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                          <Pie
                            data={topProductsPieData}
                            cx="50%"
                            cy="50%"
                            innerRadius={40}
                            outerRadius={80}
                            paddingAngle={2}
                            dataKey="value"
                          >
                            {topProductsPieData.map((entry, index) => (
                              <Cell key={`cell-${index}`} fill={entry.color} />
                            ))}
                          </Pie>
                          <Tooltip
                            formatter={(value: number) => formatCurrency(value)}
                          />
                        </PieChart>
                      </ResponsiveContainer>
                    </div>
                    {/* Product List */}
                    <div className="flex-1 space-y-3">
                      {vm.data.topProducts.slice(0, 5).map((product, index) => (
                        <div
                          key={product.id}
                          className="flex items-center justify-between"
                        >
                          <div className="flex items-center gap-3">
                            <div
                              className="h-3 w-3 rounded-full"
                              style={{ backgroundColor: CHART_COLORS[index] }}
                            />
                            <div>
                              <p className="text-sm font-medium truncate max-w-[150px]">
                                {product.name}
                              </p>
                              <p className="text-xs text-muted-foreground">
                                {product.quantitySold} sold
                              </p>
                            </div>
                          </div>
                          <p className="text-sm font-medium">
                            {formatCurrency(product.revenue)}
                          </p>
                        </div>
                      ))}
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* Customer Insights */}
              <Card>
                <CardHeader>
                  <CardTitle>Customer Insights</CardTitle>
                  <CardDescription>Customer activity and top buyers</CardDescription>
                </CardHeader>
                <CardContent>
                  {/* Customer Stats */}
                  <div className="grid grid-cols-3 gap-4 mb-6">
                    <div className="text-center p-3 bg-muted/50 rounded-lg">
                      <p className="text-2xl font-bold">
                        {vm.data.customerInsights.totalCustomers}
                      </p>
                      <p className="text-xs text-muted-foreground">Total</p>
                    </div>
                    <div className="text-center p-3 bg-muted/50 rounded-lg">
                      <p className="text-2xl font-bold text-green-600">
                        +{vm.data.customerInsights.newCustomers}
                      </p>
                      <p className="text-xs text-muted-foreground">New</p>
                    </div>
                    <div className="text-center p-3 bg-muted/50 rounded-lg">
                      <p className="text-2xl font-bold">
                        {vm.data.customerInsights.repeatCustomers}
                      </p>
                      <p className="text-xs text-muted-foreground">Repeat</p>
                    </div>
                  </div>

                  {/* Top Buyers */}
                  <div className="space-y-3">
                    <p className="text-sm font-medium text-muted-foreground">
                      Top Buyers
                    </p>
                    {vm.data.customerInsights.topBuyers.slice(0, 5).map((buyer) => (
                      <div
                        key={buyer.id}
                        className="flex items-center justify-between py-2 border-b last:border-0"
                      >
                        <div>
                          <p className="text-sm font-medium">{buyer.name}</p>
                          <p className="text-xs text-muted-foreground">
                            {buyer.totalOrders} orders
                          </p>
                        </div>
                        <p className="text-sm font-medium">
                          {formatCurrency(buyer.totalSpent)}
                        </p>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            </div>
          </>
        )}

        {/* Empty state */}
        {!vm.isLoading && !vm.error && !vm.data && (
          <Card>
            <CardContent className="p-8 text-center text-muted-foreground">
              No analytics data available for the selected period.
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
