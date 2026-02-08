package com.poultry.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerAnalyticsDto {
    private LocalDate startDate;
    private LocalDate endDate;

    // Order metrics
    private long totalOrders;
    private long pendingOrders;
    private long completedOrders;
    private long cancelledOrders;
    private BigDecimal orderCompletionRate;

    // Revenue metrics
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private BigDecimal platformFeesTotal;
    private BigDecimal netEarnings;

    // Growth metrics
    private BigDecimal revenueGrowthPercent;
    private BigDecimal ordersGrowthPercent;

    // Product metrics
    private long totalProducts;
    private long activeProducts;
    private List<TopProductDto> topProducts;

    // Customer metrics
    private long totalUniqueCustomers;
    private long newCustomers;
    private long repeatCustomers;

    // Daily breakdown for charts
    private List<DailyRevenueDto> dailyRevenue;
    private Map<String, Long> ordersByStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProductDto {
        private String productId;
        private String productName;
        private long orderCount;
        private BigDecimal revenue;
        private int quantitySold;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenueDto {
        private LocalDate date;
        private BigDecimal revenue;
        private long orderCount;
    }
}
