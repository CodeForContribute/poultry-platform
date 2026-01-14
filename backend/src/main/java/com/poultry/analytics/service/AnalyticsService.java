package com.poultry.analytics.service;

import com.poultry.analytics.dto.*;
import com.poultry.analytics.repository.AnalyticsRepository;
import com.poultry.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardStats", key = "#range.startDate.toString() + '-' + #range.endDate.toString()")
    public DashboardStatsDto getDashboardStats(DateRangeRequest range) {
        log.info("Fetching dashboard stats for: {} to {}", range.getStartDate(), range.getEndDate());

        Instant start = range.getStartInstant();
        Instant end = range.getEndInstant();

        Long totalOrders = analyticsRepository.countOrdersInPeriod(start, end);
        Long pendingOrders = countOrdersInStatuses(start, end,
                Order.OrderStatus.PLACED, Order.OrderStatus.SELLER_CONFIRMED,
                Order.OrderStatus.PAYMENT_PENDING, Order.OrderStatus.PAID, Order.OrderStatus.DISPATCHED);
        Long completedOrders = countOrdersInStatuses(start, end, Order.OrderStatus.DELIVERED, Order.OrderStatus.SETTLED);
        Long cancelledOrders = countOrdersInStatuses(start, end,
                Order.OrderStatus.CANCELLED_BY_BUYER, Order.OrderStatus.SELLER_REJECTED, Order.OrderStatus.REFUNDED);

        BigDecimal totalRevenue = analyticsRepository.sumRevenueInPeriod(start, end);
        BigDecimal totalPlatformFees = analyticsRepository.sumPlatformFeesInPeriod(start, end);
        BigDecimal averageOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Long totalActiveSellers = analyticsRepository.countActiveSellers();
        Long totalActiveBuyers = analyticsRepository.countActiveBuyers();
        Long newSellersInPeriod = analyticsRepository.countNewSellersInPeriod(start, end);
        Long newBuyersInPeriod = analyticsRepository.countNewBuyersInPeriod(start, end);

        // Calculate growth
        DateRangeRequest previousRange = getPreviousPeriod(range);
        BigDecimal previousRevenue = analyticsRepository.sumRevenueInPeriod(
                previousRange.getStartInstant(), previousRange.getEndInstant());
        Long previousOrders = analyticsRepository.countOrdersInPeriod(
                previousRange.getStartInstant(), previousRange.getEndInstant());

        BigDecimal revenueGrowth = calculateGrowthPercent(previousRevenue, totalRevenue);
        BigDecimal ordersGrowth = calculateGrowthPercent(BigDecimal.valueOf(previousOrders), BigDecimal.valueOf(totalOrders));

        return DashboardStatsDto.builder()
                .startDate(range.getStartDate())
                .endDate(range.getEndDate())
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .totalRevenue(totalRevenue)
                .totalPlatformFees(totalPlatformFees)
                .averageOrderValue(averageOrderValue)
                .totalActiveSellers(totalActiveSellers)
                .totalActiveBuyers(totalActiveBuyers)
                .newSellersInPeriod(newSellersInPeriod)
                .newBuyersInPeriod(newBuyersInPeriod)
                .revenueGrowthPercent(revenueGrowth)
                .ordersGrowthPercent(ordersGrowth)
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "orderAnalytics", key = "#range.startDate.toString() + '-' + #range.endDate.toString()")
    public OrderAnalyticsDto getOrderAnalytics(DateRangeRequest range) {
        log.info("Fetching order analytics for: {} to {}", range.getStartDate(), range.getEndDate());

        Instant start = range.getStartInstant();
        Instant end = range.getEndInstant();

        Long totalOrders = analyticsRepository.countOrdersInPeriod(start, end);
        BigDecimal totalOrderValue = analyticsRepository.sumRevenueInPeriod(start, end);

        Map<String, Long> ordersByStatus = analyticsRepository.countOrdersByStatus(start, end).stream()
                .collect(Collectors.toMap(row -> ((Order.OrderStatus) row[0]).name(), row -> (Long) row[1]));

        Map<String, Long> ordersByType = analyticsRepository.countOrdersByType(start, end).stream()
                .collect(Collectors.toMap(row -> ((Order.OrderType) row[0]).name(), row -> (Long) row[1]));

        Long completedOrders = ordersByStatus.getOrDefault("DELIVERED", 0L) + ordersByStatus.getOrDefault("SETTLED", 0L);
        Long cancelledOrders = ordersByStatus.getOrDefault("CANCELLED_BY_BUYER", 0L) +
                ordersByStatus.getOrDefault("SELLER_REJECTED", 0L) + ordersByStatus.getOrDefault("REFUNDED", 0L);

        BigDecimal completionRate = totalOrders > 0
                ? BigDecimal.valueOf(completedOrders * 100).divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal cancellationRate = totalOrders > 0
                ? BigDecimal.valueOf(cancelledOrders * 100).divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return OrderAnalyticsDto.builder()
                .startDate(range.getStartDate())
                .endDate(range.getEndDate())
                .totalOrders(totalOrders)
                .totalOrderValue(totalOrderValue)
                .ordersByStatus(ordersByStatus)
                .ordersByType(ordersByType)
                .orderCompletionRate(completionRate)
                .cancellationRate(cancellationRate)
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "topSellers", key = "#range.startDate.toString() + '-' + #range.endDate.toString() + '-' + #limit")
    public List<TopSellerDto> getTopSellers(DateRangeRequest range, int limit) {
        log.info("Fetching top {} sellers for: {} to {}", limit, range.getStartDate(), range.getEndDate());

        List<Object[]> results = analyticsRepository.getTopSellersByRevenue(
                range.getStartInstant(), range.getEndInstant(), limit);

        List<TopSellerDto> topSellers = new ArrayList<>();
        int rank = 1;

        for (Object[] row : results) {
            Long totalOrders = ((Number) row[2]).longValue();
            Long completedOrders = ((Number) row[3]).longValue();
            BigDecimal totalRevenue = (BigDecimal) row[4];

            BigDecimal completionRate = totalOrders > 0
                    ? BigDecimal.valueOf(completedOrders * 100).divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal avgOrderValue = totalOrders > 0
                    ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            topSellers.add(TopSellerDto.builder()
                    .sellerId((UUID) row[0])
                    .businessName((String) row[1])
                    .totalOrders(totalOrders)
                    .completedOrders(completedOrders)
                    .totalRevenue(totalRevenue)
                    .platformFees((BigDecimal) row[5])
                    .uniqueCustomers(((Number) row[6]).longValue())
                    .averageOrderValue(avgOrderValue)
                    .orderCompletionRate(completionRate)
                    .rank(rank++)
                    .build());
        }

        return topSellers;
    }

    private Long countOrdersInStatuses(Instant start, Instant end, Order.OrderStatus... statuses) {
        long count = 0;
        for (Order.OrderStatus status : statuses) {
            count += analyticsRepository.countOrdersByStatusInPeriod(status, start, end);
        }
        return count;
    }

    private DateRangeRequest getPreviousPeriod(DateRangeRequest currentRange) {
        long daysBetween = ChronoUnit.DAYS.between(currentRange.getStartDate(), currentRange.getEndDate()) + 1;
        return DateRangeRequest.builder()
                .startDate(currentRange.getStartDate().minusDays(daysBetween))
                .endDate(currentRange.getStartDate().minusDays(1))
                .build();
    }

    private BigDecimal calculateGrowthPercent(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }
}
