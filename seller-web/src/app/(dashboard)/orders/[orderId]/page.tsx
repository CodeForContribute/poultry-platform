"use client";

import { use } from "react";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  ArrowLeft,
  Loader2,
  Check,
  X,
  Truck,
  Package,
  Clock,
  User,
  Phone,
  MapPin,
  Calendar,
  CheckCircle,
  XCircle,
  PackageCheck,
} from "lucide-react";
import { Header } from "@/components/layout/header";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { formatCurrency, formatDateTime, formatRelativeTime, formatPhoneNumber } from "@/lib/utils";
import * as ordersApi from "@/lib/api/orders";
import type { OrderStatus } from "@/types";

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

const statusLabels: Record<OrderStatus, string> = {
  DRAFT: "Draft",
  PLACED: "Placed",
  SELLER_CONFIRMED: "Confirmed",
  SELLER_REJECTED: "Rejected",
  PAYMENT_PENDING: "Payment Pending",
  PAYMENT_FAILED: "Payment Failed",
  PAID: "Paid",
  DISPATCHED: "Dispatched",
  DELIVERED: "Delivered",
  SETTLED: "Settled",
  CANCELLED_BY_BUYER: "Cancelled",
  REFUND_INITIATED: "Refund Initiated",
  REFUNDED: "Refunded",
};

interface OrderTimelineEvent {
  status: OrderStatus;
  label: string;
  icon: typeof Clock;
  completed: boolean;
  current: boolean;
}

function getOrderTimeline(currentStatus: OrderStatus): OrderTimelineEvent[] {
  const statusOrder: OrderStatus[] = ["PLACED", "SELLER_CONFIRMED", "PAID", "DISPATCHED", "DELIVERED"];

  if (currentStatus === "SELLER_REJECTED") {
    return [
      { status: "PLACED", label: "Order Placed", icon: Package, completed: true, current: false },
      { status: "SELLER_REJECTED", label: "Rejected by Seller", icon: XCircle, completed: true, current: true },
    ];
  }

  if (currentStatus === "CANCELLED_BY_BUYER") {
    return [
      { status: "PLACED", label: "Order Placed", icon: Package, completed: true, current: false },
      { status: "CANCELLED_BY_BUYER", label: "Cancelled by Buyer", icon: XCircle, completed: true, current: true },
    ];
  }

  if (currentStatus === "PAYMENT_FAILED") {
    return [
      { status: "PLACED", label: "Order Placed", icon: Package, completed: true, current: false },
      { status: "SELLER_CONFIRMED", label: "Confirmed", icon: CheckCircle, completed: true, current: false },
      { status: "PAYMENT_FAILED", label: "Payment Failed", icon: XCircle, completed: true, current: true },
    ];
  }

  const currentIndex = statusOrder.indexOf(currentStatus);
  const icons: Record<string, typeof Clock> = {
    PLACED: Package, SELLER_CONFIRMED: CheckCircle, PAID: CheckCircle, DISPATCHED: Truck, DELIVERED: PackageCheck,
  };
  const labels: Record<string, string> = {
    PLACED: "Order Placed", SELLER_CONFIRMED: "Confirmed", PAID: "Payment Received", DISPATCHED: "Dispatched", DELIVERED: "Delivered",
  };

  return statusOrder.map((status, index) => ({
    status, label: labels[status], icon: icons[status], completed: index <= currentIndex, current: index === currentIndex,
  }));
}

export default function OrderDetailPage({ params }: { params: Promise<{ orderId: string }> }) {
  const { orderId } = use(params);
  const queryClient = useQueryClient();

  const { data: order, isLoading, error } = useQuery({
    queryKey: ["seller-order", orderId],
    queryFn: async () => {
      const response = await ordersApi.getOrder(orderId);
      if (response.success && response.data) return response.data;
      throw new Error(response.message || "Failed to load order");
    },
  });

  const confirmMutation = useMutation({
    mutationFn: () => ordersApi.confirmOrder(orderId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-order", orderId] });
      queryClient.invalidateQueries({ queryKey: ["seller-orders"] });
      toast.success("Order confirmed successfully");
    },
    onError: (error: Error) => toast.error(error.message || "Failed to confirm order"),
  });

  const rejectMutation = useMutation({
    mutationFn: (reason: string) => ordersApi.rejectOrder(orderId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-order", orderId] });
      queryClient.invalidateQueries({ queryKey: ["seller-orders"] });
      toast.success("Order rejected");
    },
    onError: (error: Error) => toast.error(error.message || "Failed to reject order"),
  });

  const dispatchMutation = useMutation({
    mutationFn: () => ordersApi.dispatchOrder(orderId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-order", orderId] });
      queryClient.invalidateQueries({ queryKey: ["seller-orders"] });
      toast.success("Order dispatched");
    },
    onError: (error: Error) => toast.error(error.message || "Failed to dispatch order"),
  });

  const deliverMutation = useMutation({
    mutationFn: () => ordersApi.deliverOrder(orderId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-order", orderId] });
      queryClient.invalidateQueries({ queryKey: ["seller-orders"] });
      toast.success("Order marked as delivered");
    },
    onError: (error: Error) => toast.error(error.message || "Failed to mark order as delivered"),
  });

  const handleConfirm = () => confirmMutation.mutate();
  const handleReject = () => {
    const reason = prompt("Please enter rejection reason:");
    if (reason) rejectMutation.mutate(reason);
  };
  const handleDispatch = () => dispatchMutation.mutate();
  const handleDeliver = () => deliverMutation.mutate();

  const isPending = confirmMutation.isPending || rejectMutation.isPending || dispatchMutation.isPending || deliverMutation.isPending;

  if (isLoading) {
    return (
      <div>
        <Header title="Order Details" />
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div>
        <Header title="Order Details" />
        <div className="p-6">
          <Link href="/orders">
            <Button variant="ghost" size="sm" className="mb-6">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Back to Orders
            </Button>
          </Link>
          <Card>
            <CardContent className="p-8 text-center text-destructive">
              {error instanceof Error ? error.message : "Failed to load order"}
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  const timeline = getOrderTimeline(order.status);

  return (
    <div>
      <Header title="Order Details" />
      <div className="p-6 space-y-6">
        <Link href="/orders">
          <Button variant="ghost" size="sm">
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back to Orders
          </Button>
        </Link>

        <Card>
          <CardContent className="p-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <div className="space-y-1">
                <div className="flex flex-wrap items-center gap-3">
                  <h2 className="text-2xl font-bold">{order.orderNumber}</h2>
                  <Badge className={statusColors[order.status]}>{statusLabels[order.status]}</Badge>
                </div>
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <Calendar className="h-4 w-4" />
                  <span>Placed on {formatDateTime(order.createdAt)}</span>
                </div>
              </div>
              <div className="flex flex-wrap gap-2">
                {order.status === "PLACED" && (
                  <>
                    <Button variant="outline" onClick={handleReject} disabled={isPending}>
                      {rejectMutation.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <X className="mr-2 h-4 w-4" />}
                      Reject Order
                    </Button>
                    <Button onClick={handleConfirm} disabled={isPending}>
                      {confirmMutation.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Check className="mr-2 h-4 w-4" />}
                      Confirm Order
                    </Button>
                  </>
                )}
                {(order.status === "SELLER_CONFIRMED" || order.status === "PAID") && (
                  <Button onClick={handleDispatch} disabled={isPending}>
                    {dispatchMutation.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Truck className="mr-2 h-4 w-4" />}
                    Mark as Dispatched
                  </Button>
                )}
                {order.status === "DISPATCHED" && (
                  <Button onClick={handleDeliver} disabled={isPending}>
                    {deliverMutation.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <PackageCheck className="mr-2 h-4 w-4" />}
                    Mark as Delivered
                  </Button>
                )}
              </div>
            </div>
          </CardContent>
        </Card>

        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2 space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Package className="h-5 w-5" />
                  Order Items
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b">
                        <th className="py-3 px-2 text-left text-sm font-medium text-muted-foreground">Product</th>
                        <th className="py-3 px-2 text-right text-sm font-medium text-muted-foreground">Qty</th>
                        <th className="py-3 px-2 text-right text-sm font-medium text-muted-foreground">Unit Price</th>
                        <th className="py-3 px-2 text-right text-sm font-medium text-muted-foreground">Discount</th>
                        <th className="py-3 px-2 text-right text-sm font-medium text-muted-foreground">Total</th>
                      </tr>
                    </thead>
                    <tbody>
                      {order.items.map((item) => (
                        <tr key={item.id} className="border-b last:border-0">
                          <td className="py-3 px-2">
                            <div>
                              <p className="font-medium">{item.productName}</p>
                              <p className="text-xs text-muted-foreground">SKU: {item.productSku}</p>
                            </div>
                          </td>
                          <td className="py-3 px-2 text-right">{item.quantity}</td>
                          <td className="py-3 px-2 text-right">{formatCurrency(item.unitPrice)}</td>
                          <td className="py-3 px-2 text-right">
                            {item.discountAmount > 0 ? <span className="text-green-600">-{formatCurrency(item.discountAmount)}</span> : "-"}
                          </td>
                          <td className="py-3 px-2 text-right font-medium">{formatCurrency(item.lineTotal)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <Separator className="my-4" />
                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Subtotal</span>
                    <span>{formatCurrency(order.subtotal)}</span>
                  </div>
                  {order.discountAmount > 0 && (
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground">Discount</span>
                      <span className="text-green-600">-{formatCurrency(order.discountAmount)}</span>
                    </div>
                  )}
                  {order.gstAmount > 0 && (
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground">GST</span>
                      <span>{formatCurrency(order.gstAmount)}</span>
                    </div>
                  )}
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Delivery Fee</span>
                    <span>{order.deliveryCharge > 0 ? formatCurrency(order.deliveryCharge) : "Free"}</span>
                  </div>
                  <Separator />
                  <div className="flex justify-between font-semibold text-lg">
                    <span>Total</span>
                    <span>{formatCurrency(order.totalAmount)}</span>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Clock className="h-5 w-5" />
                  Order Timeline
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {timeline.map((event, index) => {
                    const Icon = event.icon;
                    const isLast = index === timeline.length - 1;
                    return (
                      <div key={event.status} className="flex gap-4">
                        <div className="flex flex-col items-center">
                          <div className={`flex h-8 w-8 items-center justify-center rounded-full ${event.completed ? (event.status === "SELLER_REJECTED" || event.status === "CANCELLED_BY_BUYER" || event.status === "PAYMENT_FAILED" ? "bg-red-100 text-red-600" : "bg-green-100 text-green-600") : "bg-gray-100 text-gray-400"}`}>
                            <Icon className="h-4 w-4" />
                          </div>
                          {!isLast && <div className={`w-0.5 flex-1 min-h-[24px] ${event.completed ? "bg-green-200" : "bg-gray-200"}`} />}
                        </div>
                        <div className="pb-4">
                          <p className={`font-medium ${event.current ? "text-primary" : ""}`}>{event.label}</p>
                          {event.current && <p className="text-sm text-muted-foreground">{formatRelativeTime(order.updatedAt)}</p>}
                        </div>
                      </div>
                    );
                  })}
                </div>
                {order.cancellationReason && (
                  <div className="mt-4 p-4 bg-red-50 rounded-lg border border-red-200">
                    <p className="text-sm font-medium text-red-800">Cancellation Reason</p>
                    <p className="text-sm text-red-700">{order.cancellationReason}</p>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          <div className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="text-base flex items-center gap-2">
                  <User className="h-4 w-4" />
                  Buyer Information
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <p className="font-medium">{order.buyerName}</p>
                </div>
                <div className="flex items-start gap-2 text-sm">
                  <Phone className="h-4 w-4 text-muted-foreground mt-0.5" />
                  <span>{formatPhoneNumber(order.deliveryAddress.contactPhone)}</span>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base flex items-center gap-2">
                  <MapPin className="h-4 w-4" />
                  Delivery Address
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-2 text-sm">
                {order.deliveryAddress.label && <Badge variant="secondary">{order.deliveryAddress.label}</Badge>}
                <p className="font-medium">{order.deliveryAddress.contactName}</p>
                <p>{order.deliveryAddress.line1}</p>
                {order.deliveryAddress.line2 && <p>{order.deliveryAddress.line2}</p>}
                <p>{order.deliveryAddress.city}, {order.deliveryAddress.state} - {order.deliveryAddress.pincode}</p>
                {order.deliveryAddress.landmark && <p className="text-muted-foreground">Landmark: {order.deliveryAddress.landmark}</p>}
                <Separator />
                <div className="flex items-center gap-2">
                  <Phone className="h-4 w-4 text-muted-foreground" />
                  <span>{formatPhoneNumber(order.deliveryAddress.contactPhone)}</span>
                </div>
              </CardContent>
            </Card>

            {(order.deliveryDate || order.deliverySlot || order.deliveryInstructions) && (
              <Card>
                <CardHeader>
                  <CardTitle className="text-base flex items-center gap-2">
                    <Truck className="h-4 w-4" />
                    Delivery Details
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-3 text-sm">
                  {order.deliveryDate && (
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Delivery Date</span>
                      <span className="font-medium">{order.deliveryDate}</span>
                    </div>
                  )}
                  {order.deliverySlot && (
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Time Slot</span>
                      <span className="font-medium">{order.deliverySlot}</span>
                    </div>
                  )}
                  {order.deliveryInstructions && (
                    <div>
                      <p className="text-muted-foreground mb-1">Instructions</p>
                      <p className="p-2 bg-muted rounded text-sm">{order.deliveryInstructions}</p>
                    </div>
                  )}
                </CardContent>
              </Card>
            )}

            <Card>
              <CardHeader>
                <CardTitle className="text-base">Order Details</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Order Type</span>
                  <Badge variant="outline">{order.type}</Badge>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Platform Fee</span>
                  <span>{formatCurrency(order.platformFee)}</span>
                </div>
                <Separator />
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Created</span>
                  <span>{formatDateTime(order.createdAt)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Last Updated</span>
                  <span>{formatRelativeTime(order.updatedAt)}</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
