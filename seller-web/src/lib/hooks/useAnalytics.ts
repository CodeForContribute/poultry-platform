"use client";

import { useState, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { subDays, format } from "date-fns";
import type { DateRange, AnalyticsData } from "@/types";
import * as analyticsApi from "@/lib/api/analytics";

export interface AnalyticsViewModel {
  // State
  dateRange: DateRange;
  startDate: Date | undefined;
  endDate: Date | undefined;

  // Data
  data: AnalyticsData | undefined;
  isLoading: boolean;
  error: Error | null;

  // Computed
  dateRangeLabel: string;
  formattedPeriod: string;

  // Actions
  setDateRange: (range: DateRange) => void;
  setCustomDateRange: (start: Date, end: Date) => void;
  refetch: () => void;
}

export function useAnalytics(): AnalyticsViewModel {
  const [dateRange, setDateRange] = useState<DateRange>("30d");
  const [customStart, setCustomStart] = useState<Date | undefined>();
  const [customEnd, setCustomEnd] = useState<Date | undefined>();

  // Calculate actual date range
  const { startDate, endDate } = useMemo(() => {
    if (dateRange === "custom" && customStart && customEnd) {
      return { startDate: customStart, endDate: customEnd };
    }

    const end = new Date();
    let start: Date;

    switch (dateRange) {
      case "7d":
        start = subDays(end, 7);
        break;
      case "30d":
        start = subDays(end, 30);
        break;
      case "90d":
        start = subDays(end, 90);
        break;
      case "12m":
        start = subDays(end, 365);
        break;
      default:
        start = subDays(end, 30);
    }

    return { startDate: start, endDate: end };
  }, [dateRange, customStart, customEnd]);

  // Fetch analytics data
  const {
    data,
    isLoading,
    error,
    refetch,
  } = useQuery<AnalyticsData, Error>({
    queryKey: ["seller-analytics", dateRange, startDate?.toISOString(), endDate?.toISOString()],
    queryFn: async () => {
      // Use mock data for now - replace with API call when backend is ready
      // const response = await analyticsApi.getAnalytics({
      //   dateRange,
      //   startDate: startDate?.toISOString(),
      //   endDate: endDate?.toISOString(),
      // });
      // if (response.success && response.data) {
      //   return response.data;
      // }
      // throw new Error(response.message || "Failed to load analytics");

      // Simulate API delay
      await new Promise((resolve) => setTimeout(resolve, 500));
      return analyticsApi.getMockAnalyticsData(dateRange);
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  // Computed values
  const dateRangeLabel = useMemo(() => {
    switch (dateRange) {
      case "7d":
        return "Last 7 days";
      case "30d":
        return "Last 30 days";
      case "90d":
        return "Last 90 days";
      case "12m":
        return "Last 12 months";
      case "custom":
        return "Custom range";
      default:
        return "Last 30 days";
    }
  }, [dateRange]);

  const formattedPeriod = useMemo(() => {
    if (!startDate || !endDate) return "";
    return `${format(startDate, "MMM d, yyyy")} - ${format(endDate, "MMM d, yyyy")}`;
  }, [startDate, endDate]);

  // Actions
  const handleSetDateRange = (range: DateRange) => {
    setDateRange(range);
    if (range !== "custom") {
      setCustomStart(undefined);
      setCustomEnd(undefined);
    }
  };

  const handleSetCustomDateRange = (start: Date, end: Date) => {
    setCustomStart(start);
    setCustomEnd(end);
    setDateRange("custom");
  };

  return {
    dateRange,
    startDate,
    endDate,
    data,
    isLoading,
    error: error as Error | null,
    dateRangeLabel,
    formattedPeriod,
    setDateRange: handleSetDateRange,
    setCustomDateRange: handleSetCustomDateRange,
    refetch,
  };
}
