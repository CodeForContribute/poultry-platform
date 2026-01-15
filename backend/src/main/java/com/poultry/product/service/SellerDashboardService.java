package com.poultry.product.service;

import com.poultry.order.entity.Order;
import com.poultry.order.repository.OrderRepository;
import com.poultry.product.dto.SellerDashboardDto;
import com.poultry.product.entity.Product;
import com.poultry.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerDashboardService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public SellerDashboardDto getDashboard(UUID sellerId) {
        log.info("Fetching dashboard data for seller: {}", sellerId);

        // Product counts
        long totalProducts = productRepository.countBySellerId(sellerId);
        long activeProducts = productRepository.countBySellerIdAndStatus(sellerId, Product.ProductStatus.ACTIVE);

        // Pending orders (PLACED status)
        List<Order.OrderStatus> pendingStatuses = List.of(Order.OrderStatus.PLACED);
        long pendingOrders = orderRepository.countBySellerIdAndStatusIn(sellerId, pendingStatuses);

        // Today's date range
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant todayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant todayEnd = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // Today's orders count
        long todayOrders = orderRepository.countBySellerIdAndCreatedAtBetween(sellerId, todayStart, todayEnd);

        // Revenue calculation (for orders that are PAID, DISPATCHED, DELIVERED, SETTLED)
        List<Order.OrderStatus> revenueStatuses = List.of(
                Order.OrderStatus.PAID,
                Order.OrderStatus.DISPATCHED,
                Order.OrderStatus.DELIVERED,
                Order.OrderStatus.SETTLED
        );

        // Today's revenue
        BigDecimal todayRevenue = orderRepository.sumTotalAmountBySellerIdAndStatusInAndCreatedAtBetween(
                sellerId, revenueStatuses, todayStart, todayEnd);

        // Total revenue (all time)
        BigDecimal totalRevenue = orderRepository.sumTotalAmountBySellerIdAndStatusIn(sellerId, revenueStatuses);

        return SellerDashboardDto.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .pendingOrders(pendingOrders)
                .todayOrders(todayOrders)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .build();
    }
}
