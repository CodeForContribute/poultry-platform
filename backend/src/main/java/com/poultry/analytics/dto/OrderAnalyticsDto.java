package com.poultry.analytics.dto;

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
public class OrderAnalyticsDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long totalOrders;
    private BigDecimal totalOrderValue;
    private Map<String, Long> ordersByStatus;
    private Map<String, Long> ordersByType;
    private List<DailyTrend> dailyTrends;
    private BigDecimal orderCompletionRate;
    private BigDecimal cancellationRate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTrend {
        private LocalDate date;
        private Long orderCount;
        private BigDecimal orderValue;
    }
}
