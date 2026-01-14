package com.poultry.payment.repository;

import com.poultry.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);

    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    List<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByOrderIdAndStatus(UUID orderId, Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.gatewayPaymentId IS NULL " +
            "AND p.createdAt < :cutoff")
    List<Payment> findPendingPaymentsForPolling(Instant cutoff);

    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId AND p.status = 'SUCCESS' " +
            "ORDER BY p.createdAt DESC LIMIT 1")
    Optional<Payment> findSuccessfulPaymentForOrder(UUID orderId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.orderId = :orderId AND p.status = 'SUCCESS'")
    long countSuccessfulPaymentsForOrder(UUID orderId);

    boolean existsByGatewayPaymentId(String gatewayPaymentId);

  List<Payment> findByCreatedAtBetween(Instant start, Instant end);
}
