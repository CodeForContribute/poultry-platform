package com.poultry.analytics.repository;

import com.poultry.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AnalyticsRepository extends JpaRepository<Order, UUID> {

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :start AND :end")
    Long countOrdersInPeriod(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.createdAt BETWEEN :start AND :end")
    Long countOrdersByStatusInPeriod(@Param("status") Order.OrderStatus status,
                                     @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
            "WHERE o.status IN ('PAID', 'DISPATCHED', 'DELIVERED', 'SETTLED') " +
            "AND o.createdAt BETWEEN :start AND :end")
    BigDecimal sumRevenueInPeriod(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(o.platformFee), 0) FROM Order o " +
            "WHERE o.status IN ('PAID', 'DISPATCHED', 'DELIVERED', 'SETTLED') " +
            "AND o.createdAt BETWEEN :start AND :end")
    BigDecimal sumPlatformFeesInPeriod(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT o.status, COUNT(o) FROM Order o " +
            "WHERE o.createdAt BETWEEN :start AND :end GROUP BY o.status")
    List<Object[]> countOrdersByStatus(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT o.type, COUNT(o) FROM Order o " +
            "WHERE o.createdAt BETWEEN :start AND :end GROUP BY o.type")
    List<Object[]> countOrdersByType(@Param("start") Instant start, @Param("end") Instant end);

    @Query(value = "SELECT COUNT(*) FROM sellers WHERE status = 'ACTIVE'", nativeQuery = true)
    Long countActiveSellers();

    @Query(value = "SELECT COUNT(*) FROM buyers WHERE status = 'ACTIVE'", nativeQuery = true)
    Long countActiveBuyers();

    @Query(value = "SELECT COUNT(*) FROM sellers WHERE created_at BETWEEN :start AND :end", nativeQuery = true)
    Long countNewSellersInPeriod(@Param("start") Instant start, @Param("end") Instant end);

    @Query(value = "SELECT COUNT(*) FROM buyers WHERE created_at BETWEEN :start AND :end", nativeQuery = true)
    Long countNewBuyersInPeriod(@Param("start") Instant start, @Param("end") Instant end);

    @Query(value = "SELECT o.seller_id, s.business_name, COUNT(*) as total_orders, " +
            "COUNT(*) FILTER (WHERE o.status IN ('DELIVERED', 'SETTLED')) as completed_orders, " +
            "COALESCE(SUM(o.total_amount) FILTER (WHERE o.status IN ('PAID', 'DISPATCHED', 'DELIVERED', 'SETTLED')), 0) as total_revenue, " +
            "COALESCE(SUM(o.platform_fee) FILTER (WHERE o.status IN ('PAID', 'DISPATCHED', 'DELIVERED', 'SETTLED')), 0) as platform_fees, " +
            "COUNT(DISTINCT o.buyer_id) as unique_customers " +
            "FROM orders o JOIN sellers s ON o.seller_id = s.id " +
            "WHERE o.created_at BETWEEN :start AND :end " +
            "GROUP BY o.seller_id, s.business_name " +
            "ORDER BY total_revenue DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> getTopSellersByRevenue(@Param("start") Instant start, @Param("end") Instant end, @Param("limit") int limit);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.sellerId = :sellerId AND o.createdAt BETWEEN :start AND :end")
    Long countSellerOrdersInPeriod(@Param("sellerId") UUID sellerId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
            "WHERE o.sellerId = :sellerId AND o.status IN ('PAID', 'DISPATCHED', 'DELIVERED', 'SETTLED') " +
            "AND o.createdAt BETWEEN :start AND :end")
    BigDecimal sumSellerRevenueInPeriod(@Param("sellerId") UUID sellerId, @Param("start") Instant start, @Param("end") Instant end);
}
