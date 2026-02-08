package com.poultry.seller.service;

import com.poultry.analytics.dto.DateRangeRequest;
import com.poultry.order.entity.Order;
import com.poultry.order.repository.OrderRepository;
import com.poultry.product.entity.Product;
import com.poultry.product.repository.ProductRepository;
import com.poultry.seller.dto.SellerAnalyticsDto;
import com.poultry.seller.dto.SellerAnalyticsDto.DailyRevenueDto;
import com.poultry.seller.dto.SellerAnalyticsDto.TopProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerAnalyticsService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public SellerAnalyticsDto getAnalytics(UUID sellerId, DateRangeRequest range) {
        log.info("Fetching analytics for seller {} from {} to {}", sellerId, range.getStartDate(), range.getEndDate());

        // Fetch all orders for the seller in the date range
        Page<Order> ordersPage = orderRepository.findBySellerId(sellerId, Pageable.unpaged());
        List<Order> allOrders = ordersPage.getContent();

        // Filter by date range
        List<Order> ordersInRange = allOrders.stream()
                .filter(o -> !o.getCreatedAt().isBefore(range.getStartInstant())
                        && o.getCreatedAt().isBefore(range.getEndInstant()))
                .collect(Collectors.toList());

        // Order metrics
        long totalOrders = ordersInRange.size();
        long pendingOrders = ordersInRange.stream()
                .filter(o -> List.of(Order.OrderStatus.PLACED, Order.OrderStatus.SELLER_CONFIRMED,
                        Order.OrderStatus.PAYMENT_PENDING, Order.OrderStatus.PAID,
                        Order.OrderStatus.DISPATCHED).contains(o.getStatus()))
                .count();
        long completedOrders = ordersInRange.stream()
                .filter(o -> List.of(Order.OrderStatus.DELIVERED, Order.OrderStatus.SETTLED).contains(o.getStatus()))
                .count();
        long cancelledOrders = ordersInRange.stream()
                .filter(o -> List.of(Order.OrderStatus.CANCELLED_BY_BUYER, Order.OrderStatus.SELLER_REJECTED,
                        Order.OrderStatus.REFUNDED).contains(o.getStatus()))
                .count();

        BigDecimal completionRate = totalOrders > 0
                ? BigDecimal.valueOf(completedOrders * 100).divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Revenue metrics
        List<Order.OrderStatus> revenueStatuses = List.of(
                Order.OrderStatus.PAID, Order.OrderStatus.DISPATCHED,
                Order.OrderStatus.DELIVERED, Order.OrderStatus.SETTLED);

        BigDecimal totalRevenue = ordersInRange.stream()
                .filter(o -> revenueStatuses.contains(o.getStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal platformFeesTotal = ordersInRange.stream()
                .filter(o -> revenueStatuses.contains(o.getStatus()))
                .map(o -> o.getPlatformFee() != null ? o.getPlatformFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netEarnings = totalRevenue.subtract(platformFeesTotal);

        long paidOrdersCount = ordersInRange.stream()
                .filter(o -> revenueStatuses.contains(o.getStatus()))
                .count();
        BigDecimal averageOrderValue = paidOrdersCount > 0
                ? totalRevenue.divide(BigDecimal.valueOf(paidOrdersCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Calculate growth (compare with previous period)
        DateRangeRequest previousRange = getPreviousPeriod(range);
        List<Order> previousOrders = allOrders.stream()
                .filter(o -> !o.getCreatedAt().isBefore(previousRange.getStartInstant())
                        && o.getCreatedAt().isBefore(previousRange.getEndInstant()))
                .collect(Collectors.toList());

        BigDecimal previousRevenue = previousOrders.stream()
                .filter(o -> revenueStatuses.contains(o.getStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal revenueGrowth = calculateGrowthPercent(previousRevenue, totalRevenue);
        BigDecimal ordersGrowth = calculateGrowthPercent(
                BigDecimal.valueOf(previousOrders.size()),
                BigDecimal.valueOf(totalOrders));

        // Product metrics
        long totalProducts = productRepository.countBySellerId(sellerId);
        long activeProducts = productRepository.countBySellerIdAndStatus(sellerId, Product.ProductStatus.ACTIVE);

        // Top products (simplified - would need order items for accurate data)
        List<TopProductDto> topProducts = new ArrayList<>();

        // Customer metrics
        Set<UUID> uniqueCustomers = ordersInRange.stream()
                .map(Order::getBuyerId)
                .collect(Collectors.toSet());

        Set<UUID> previousCustomers = allOrders.stream()
                .filter(o -> o.getCreatedAt().isBefore(range.getStartInstant()))
                .map(Order::getBuyerId)
                .collect(Collectors.toSet());

        long newCustomers = uniqueCustomers.stream()
                .filter(c -> !previousCustomers.contains(c))
                .count();
        long repeatCustomers = uniqueCustomers.size() - newCustomers;

        // Daily breakdown
        List<DailyRevenueDto> dailyRevenue = buildDailyRevenue(ordersInRange, range, revenueStatuses);

        // Orders by status
        Map<String, Long> ordersByStatus = ordersInRange.stream()
                .collect(Collectors.groupingBy(o -> o.getStatus().name(), Collectors.counting()));

        return SellerAnalyticsDto.builder()
                .startDate(range.getStartDate())
                .endDate(range.getEndDate())
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .orderCompletionRate(completionRate)
                .totalRevenue(totalRevenue)
                .averageOrderValue(averageOrderValue)
                .platformFeesTotal(platformFeesTotal)
                .netEarnings(netEarnings)
                .revenueGrowthPercent(revenueGrowth)
                .ordersGrowthPercent(ordersGrowth)
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .topProducts(topProducts)
                .totalUniqueCustomers(uniqueCustomers.size())
                .newCustomers(newCustomers)
                .repeatCustomers(repeatCustomers)
                .dailyRevenue(dailyRevenue)
                .ordersByStatus(ordersByStatus)
                .build();
    }

    private List<DailyRevenueDto> buildDailyRevenue(List<Order> orders, DateRangeRequest range,
                                                     List<Order.OrderStatus> revenueStatuses) {
        Map<LocalDate, DailyRevenueDto> dailyMap = new TreeMap<>();

        // Initialize all dates in range
        LocalDate current = range.getStartDate();
        while (!current.isAfter(range.getEndDate())) {
            dailyMap.put(current, DailyRevenueDto.builder()
                    .date(current)
                    .revenue(BigDecimal.ZERO)
                    .orderCount(0L)
                    .build());
            current = current.plusDays(1);
        }

        // Aggregate orders by date
        for (Order order : orders) {
            if (revenueStatuses.contains(order.getStatus())) {
                LocalDate orderDate = order.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
                DailyRevenueDto daily = dailyMap.get(orderDate);
                if (daily != null) {
                    daily.setRevenue(daily.getRevenue().add(order.getTotalAmount()));
                    daily.setOrderCount(daily.getOrderCount() + 1);
                }
            }
        }

        return new ArrayList<>(dailyMap.values());
    }

    private DateRangeRequest getPreviousPeriod(DateRangeRequest currentRange) {
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                currentRange.getStartDate(), currentRange.getEndDate()) + 1;
        return DateRangeRequest.builder()
                .startDate(currentRange.getStartDate().minusDays(daysBetween))
                .endDate(currentRange.getStartDate().minusDays(1))
                .build();
    }

    private BigDecimal calculateGrowthPercent(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }
}
