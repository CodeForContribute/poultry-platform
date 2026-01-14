package com.poultry.order.repository;

import com.poultry.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithLock(UUID id);

    @Query("SELECT o FROM Order o WHERE o.buyerId = :buyerId ORDER BY o.createdAt DESC")
    Page<Order> findByBuyerId(UUID buyerId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.sellerId = :sellerId ORDER BY o.createdAt DESC")
    Page<Order> findBySellerId(UUID sellerId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.sellerId = :sellerId AND o.status = :status ORDER BY o.createdAt DESC")
    Page<Order> findBySellerIdAndStatus(UUID sellerId, Order.OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.buyerId = :buyerId AND o.status IN :statuses ORDER BY o.createdAt DESC")
    List<Order> findByBuyerIdAndStatusIn(UUID buyerId, List<Order.OrderStatus> statuses);

    @Query("SELECT o FROM Order o WHERE o.status = 'PAYMENT_PENDING' AND o.expiresAt < :now")
    List<Order> findExpiredPaymentPendingOrders(Instant now);

    @Query("SELECT o FROM Order o WHERE o.sellerId = :sellerId AND o.status = 'DELIVERED'")
    List<Order> findDeliveredOrdersForSettlement(UUID sellerId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.buyerId = :buyerId AND o.status NOT IN ('SETTLED', 'REFUNDED', 'SELLER_REJECTED')")
    long countActiveOrdersByBuyer(UUID buyerId);

    @Modifying
    @Query("UPDATE Order o SET o.status = 'SELLER_REJECTED', o.cancelledAt = :now, " +
            "o.cancelledBy = 'SYSTEM', o.cancellationReason = 'Payment timeout' " +
            "WHERE o.status = 'PAYMENT_PENDING' AND o.expiresAt < :now")
    int cancelExpiredOrders(Instant now);

    @Query("SELECT o.sellerId, COUNT(o), SUM(o.totalAmount) FROM Order o " +
            "WHERE o.status = 'DELIVERED' AND o.createdAt BETWEEN :start AND :end " +
            "GROUP BY o.sellerId")
    List<Object[]> getSellerOrderStats(Instant start, Instant end);
}
