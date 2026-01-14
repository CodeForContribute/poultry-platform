package com.poultry.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long totalOrders;
    private Long pendingOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private BigDecimal totalRevenue;
    private BigDecimal totalPlatformFees;
    private BigDecimal averageOrderValue;
    private Long totalActiveSellers;
    private Long totalActiveBuyers;
    private Long newSellersInPeriod;
    private Long newBuyersInPeriod;
    private BigDecimal revenueGrowthPercent;
    private BigDecimal ordersGrowthPercent;
}
