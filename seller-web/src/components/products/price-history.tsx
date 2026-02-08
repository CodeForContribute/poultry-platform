"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { format } from "date-fns";
import { History, Clock, TrendingUp, TrendingDown, Minus, Loader2 } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { formatCurrency } from "@/lib/utils";
import * as productsApi from "@/lib/api/products";

interface PriceHistoryProps {
  productId: string;
}

interface PriceRecord {
  id: string;
  basePrice: number;
  effectiveFrom: string;
  effectiveTo?: string;
  bulkDiscountSlabs?: {
    minQty: number;
    maxQty?: number;
    discountPercent: number;
  }[];
  createdBy?: string;
  createdAt: string;
  status?: "ACTIVE" | "SCHEDULED" | "EXPIRED";
}

function getPriceChangeIcon(current: number, previous: number) {
  if (current > previous) {
    return <TrendingUp className="h-4 w-4 text-green-600" />;
  } else if (current < previous) {
    return <TrendingDown className="h-4 w-4 text-red-600" />;
  }
  return <Minus className="h-4 w-4 text-muted-foreground" />;
}

function formatPriceChange(current: number, previous: number): string {
  const change = current - previous;
  const percentChange = previous > 0 ? ((change / previous) * 100).toFixed(1) : "0";
  const sign = change > 0 ? "+" : "";
  return `${sign}${formatCurrency(change)} (${sign}${percentChange}%)`;
}

export function PriceHistory({ productId }: PriceHistoryProps) {
  const [activeTab, setActiveTab] = useState("history");

  // Fetch price history
  const {
    data: priceHistory,
    isLoading: isLoadingHistory,
  } = useQuery({
    queryKey: ["price-history", productId],
    queryFn: async () => {
      const response = await productsApi.getPriceHistory(productId);
      if (response.success && response.data) {
        return response.data as PriceRecord[];
      }
      return [];
    },
    enabled: !!productId,
  });

  // Fetch scheduled prices
  const {
    data: scheduledPrices,
    isLoading: isLoadingScheduled,
  } = useQuery({
    queryKey: ["scheduled-prices", productId],
    queryFn: async () => {
      const response = await productsApi.getScheduledPrices(productId);
      if (response.success && response.data) {
        return response.data as PriceRecord[];
      }
      return [];
    },
    enabled: !!productId,
  });

  const isLoading = isLoadingHistory || isLoadingScheduled;

  if (isLoading) {
    return (
      <Card>
        <CardContent className="flex items-center justify-center py-8">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <History className="h-5 w-5" />
          Price History
        </CardTitle>
      </CardHeader>
      <CardContent>
        <Tabs value={activeTab} onValueChange={setActiveTab}>
          <TabsList className="mb-4">
            <TabsTrigger value="history" className="gap-2">
              <History className="h-4 w-4" />
              History ({priceHistory?.length || 0})
            </TabsTrigger>
            <TabsTrigger value="scheduled" className="gap-2">
              <Clock className="h-4 w-4" />
              Scheduled ({scheduledPrices?.length || 0})
            </TabsTrigger>
          </TabsList>

          <TabsContent value="history">
            {!priceHistory || priceHistory.length === 0 ? (
              <div className="text-center py-8 text-muted-foreground">
                No price history available.
              </div>
            ) : (
              <div className="space-y-3">
                {priceHistory.map((price, index) => {
                  const previousPrice = priceHistory[index + 1];
                  const isActive = !price.effectiveTo || new Date(price.effectiveTo) > new Date();

                  return (
                    <div
                      key={price.id}
                      className={`rounded-lg border p-4 ${
                        isActive ? "border-primary bg-primary/5" : ""
                      }`}
                    >
                      <div className="flex items-start justify-between">
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="text-lg font-bold">
                              {formatCurrency(price.basePrice)}
                            </span>
                            {isActive && (
                              <Badge variant="default" className="text-xs">
                                Current
                              </Badge>
                            )}
                          </div>
                          {previousPrice && (
                            <div className="flex items-center gap-1 mt-1 text-sm">
                              {getPriceChangeIcon(price.basePrice, previousPrice.basePrice)}
                              <span className={
                                price.basePrice > previousPrice.basePrice
                                  ? "text-green-600"
                                  : price.basePrice < previousPrice.basePrice
                                  ? "text-red-600"
                                  : "text-muted-foreground"
                              }>
                                {formatPriceChange(price.basePrice, previousPrice.basePrice)}
                              </span>
                            </div>
                          )}
                        </div>
                        <div className="text-right text-sm text-muted-foreground">
                          <div>
                            {format(new Date(price.effectiveFrom), "MMM d, yyyy")}
                          </div>
                          {price.effectiveTo && (
                            <div>
                              to {format(new Date(price.effectiveTo), "MMM d, yyyy")}
                            </div>
                          )}
                        </div>
                      </div>

                      {/* Bulk Discounts */}
                      {price.bulkDiscountSlabs && price.bulkDiscountSlabs.length > 0 && (
                        <div className="mt-3 pt-3 border-t">
                          <div className="text-xs font-medium text-muted-foreground mb-2">
                            Bulk Discounts
                          </div>
                          <div className="flex flex-wrap gap-2">
                            {price.bulkDiscountSlabs.map((slab, i) => (
                              <Badge key={i} variant="outline" className="text-xs">
                                {slab.minQty}+ units: {slab.discountPercent}% off
                              </Badge>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </TabsContent>

          <TabsContent value="scheduled">
            {!scheduledPrices || scheduledPrices.length === 0 ? (
              <div className="text-center py-8 text-muted-foreground">
                No scheduled price changes.
              </div>
            ) : (
              <div className="space-y-3">
                {scheduledPrices.map((price) => (
                  <div
                    key={price.id}
                    className="rounded-lg border border-dashed border-yellow-400 bg-yellow-50 dark:bg-yellow-950/20 p-4"
                  >
                    <div className="flex items-start justify-between">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-lg font-bold">
                            {formatCurrency(price.basePrice)}
                          </span>
                          <Badge variant="secondary" className="text-xs">
                            <Clock className="mr-1 h-3 w-3" />
                            Scheduled
                          </Badge>
                        </div>
                      </div>
                      <div className="text-right text-sm">
                        <div className="font-medium text-yellow-700 dark:text-yellow-400">
                          Effective {format(new Date(price.effectiveFrom), "MMM d, yyyy")}
                        </div>
                        {price.effectiveTo && (
                          <div className="text-muted-foreground">
                            until {format(new Date(price.effectiveTo), "MMM d, yyyy")}
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Bulk Discounts */}
                    {price.bulkDiscountSlabs && price.bulkDiscountSlabs.length > 0 && (
                      <div className="mt-3 pt-3 border-t border-yellow-200">
                        <div className="text-xs font-medium text-muted-foreground mb-2">
                          Bulk Discounts
                        </div>
                        <div className="flex flex-wrap gap-2">
                          {price.bulkDiscountSlabs.map((slab, i) => (
                            <Badge key={i} variant="outline" className="text-xs">
                              {slab.minQty}+ units: {slab.discountPercent}% off
                            </Badge>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  );
}
