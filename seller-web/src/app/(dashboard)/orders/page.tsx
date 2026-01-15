"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Search, Eye, Check, X, Truck, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { Header } from "@/components/layout/header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { formatCurrency, formatDateTime } from "@/lib/utils";
import * as ordersApi from "@/lib/api/orders";
import type { OrderStatus, Order } from "@/types";

const statusColors: Record<OrderStatus, string> = {
  DRAFT: "bg-gray-100 text-gray-800",
  PLACED: "bg-yellow-100 text-yellow-800",
  SELLER_CONFIRMED: "bg-blue-100 text-blue-800",
  SELLER_REJECTED: "bg-red-100 text-red-800",
  PAYMENT_PENDING: "bg-orange-100 text-orange-800",
  PAYMENT_FAILED: "bg-red-100 text-red-800",
  PAID: "bg-green-100 text-green-800",
  DISPATCHED: "bg-purple-100 text-purple-800",
  DELIVERED: "bg-green-100 text-green-800",
  SETTLED: "bg-green-100 text-green-800",
  CANCELLED_BY_BUYER: "bg-gray-100 text-gray-800",
  REFUND_INITIATED: "bg-orange-100 text-orange-800",
  REFUNDED: "bg-gray-100 text-gray-800",
};

const statusFilters: { label: string; value: OrderStatus | "ALL" }[] = [
  { label: "All", value: "ALL" },
  { label: "Pending", value: "PLACED" },
  { label: "Confirmed", value: "SELLER_CONFIRMED" },
  { label: "Paid", value: "PAID" },
  { label: "Dispatched", value: "DISPATCHED" },
  { label: "Delivered", value: "DELIVERED" },
];

export default function OrdersPage() {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<OrderStatus | "ALL">("ALL");
  const queryClient = useQueryClient();

  // Fetch orders
  const {
    data: ordersResponse,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["seller-orders", statusFilter],
    queryFn: async () => {
      const params = statusFilter !== "ALL" ? { status: statusFilter } : {};
      const response = await ordersApi.getOrders(params);
      if (response.success && response.data) {
        return response.data;
      }
      throw new Error(response.message || "Failed to load orders");
    },
  });

  const orders = ordersResponse?.content ?? [];

  // Confirm order mutation
  const confirmMutation = useMutation({
    mutationFn: (orderId: string) => ordersApi.confirmOrder(orderId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-orders"] });
      toast.success("Order confirmed successfully");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to confirm order");
    },
  });

  // Reject order mutation
  const rejectMutation = useMutation({
    mutationFn: ({ orderId, reason }: { orderId: string; reason: string }) =>
      ordersApi.rejectOrder(orderId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-orders"] });
      toast.success("Order rejected");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to reject order");
    },
  });

  // Dispatch order mutation
  const dispatchMutation = useMutation({
    mutationFn: (orderId: string) => ordersApi.dispatchOrder(orderId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-orders"] });
      toast.success("Order dispatched");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to dispatch order");
    },
  });

  const handleConfirm = (orderId: string) => {
    confirmMutation.mutate(orderId);
  };

  const handleReject = (orderId: string) => {
    const reason = prompt("Please enter rejection reason:");
    if (reason) {
      rejectMutation.mutate({ orderId, reason });
    }
  };

  const handleDispatch = (orderId: string) => {
    dispatchMutation.mutate(orderId);
  };

  const filteredOrders = orders.filter((order) => {
    const matchesSearch =
      order.orderNumber.toLowerCase().includes(search.toLowerCase()) ||
      order.buyerName.toLowerCase().includes(search.toLowerCase());
    return matchesSearch;
  });

  const isPending =
    confirmMutation.isPending ||
    rejectMutation.isPending ||
    dispatchMutation.isPending;

  return (
    <div>
      <Header title="Orders" />

      <div className="p-6 space-y-6">
        {/* Filters */}
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative max-w-sm">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search orders..."
              className="pl-10"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="flex flex-wrap gap-2">
            {statusFilters.map((filter) => (
              <Button
                key={filter.value}
                variant={statusFilter === filter.value ? "default" : "outline"}
                size="sm"
                onClick={() => setStatusFilter(filter.value)}
              >
                {filter.label}
              </Button>
            ))}
          </div>
        </div>

        {/* Loading state */}
        {isLoading && (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {/* Error state */}
        {error && (
          <Card>
            <CardContent className="p-8 text-center text-destructive">
              {error instanceof Error ? error.message : "Failed to load orders"}
            </CardContent>
          </Card>
        )}

        {/* Orders list */}
        {!isLoading && !error && (
          <div className="space-y-4">
            {filteredOrders.map((order) => (
              <Card key={order.id}>
                <CardContent className="p-4">
                  <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold">{order.orderNumber}</span>
                        <span
                          className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                            statusColors[order.status]
                          }`}
                        >
                          {order.status.replace(/_/g, " ")}
                        </span>
                      </div>
                      <p className="text-sm text-muted-foreground">
                        {order.buyerName} • {order.items?.length || 0} items
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {formatDateTime(order.createdAt)}
                      </p>
                    </div>

                    <div className="flex items-center gap-4">
                      <div className="text-right">
                        <p className="font-semibold">
                          {formatCurrency(order.totalAmount)}
                        </p>
                      </div>

                      <div className="flex gap-2">
                        {order.status === "PLACED" && (
                          <>
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => handleReject(order.id)}
                              disabled={isPending}
                            >
                              <X className="mr-1 h-4 w-4" />
                              Reject
                            </Button>
                            <Button
                              size="sm"
                              onClick={() => handleConfirm(order.id)}
                              disabled={isPending}
                            >
                              <Check className="mr-1 h-4 w-4" />
                              Confirm
                            </Button>
                          </>
                        )}
                        {(order.status === "SELLER_CONFIRMED" ||
                          order.status === "PAID") && (
                          <Button
                            size="sm"
                            onClick={() => handleDispatch(order.id)}
                            disabled={isPending}
                          >
                            <Truck className="mr-1 h-4 w-4" />
                            Dispatch
                          </Button>
                        )}
                        <Link href={`/orders/${order.id}`}>
                          <Button size="sm" variant="ghost">
                            <Eye className="h-4 w-4" />
                          </Button>
                        </Link>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}

            {filteredOrders.length === 0 && (
              <Card>
                <CardContent className="p-8 text-center text-muted-foreground">
                  {search
                    ? "No orders found matching your search."
                    : "No orders found."}
                </CardContent>
              </Card>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
