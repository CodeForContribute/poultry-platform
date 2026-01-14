package com.poultry.delivery.repository;

import com.poultry.delivery.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByOrderId(UUID orderId);

    List<Delivery> findByAgentIdAndStatusIn(UUID agentId, List<Delivery.DeliveryStatus> statuses);

    List<Delivery> findByStatus(Delivery.DeliveryStatus status);

    @Query("SELECT d FROM Delivery d WHERE d.status = :status AND d.slaDeadline < :now")
    List<Delivery> findOverdueDeliveries(@Param("status") Delivery.DeliveryStatus status, @Param("now") Instant now);

    @Query("SELECT d FROM Delivery d WHERE d.status = 'FAILED' AND d.retryCount < d.maxRetries")
    List<Delivery> findRetryableDeliveries();

    long countByStatus(Delivery.DeliveryStatus status);

    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.status = 'FAILED'")
    long countFailedDeliveries();
}
