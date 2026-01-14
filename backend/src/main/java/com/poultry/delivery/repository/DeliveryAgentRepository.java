package com.poultry.delivery.repository;

import com.poultry.delivery.entity.DeliveryAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryAgentRepository extends JpaRepository<DeliveryAgent, UUID> {

    List<DeliveryAgent> findBySellerIdAndStatus(UUID sellerId, DeliveryAgent.AgentStatus status);

    List<DeliveryAgent> findBySellerIdAndIsAvailableTrue(UUID sellerId);

    Optional<DeliveryAgent> findByPhoneHash(String phoneHash);

    @Query("SELECT da FROM DeliveryAgent da WHERE da.sellerId = :sellerId " +
           "AND da.isAvailable = true AND da.status = 'ACTIVE' " +
           "ORDER BY da.successfulDeliveries DESC")
    List<DeliveryAgent> findAvailableAgentsForSeller(@Param("sellerId") UUID sellerId);
}
