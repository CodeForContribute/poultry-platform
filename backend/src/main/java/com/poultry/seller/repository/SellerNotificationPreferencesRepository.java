package com.poultry.seller.repository;

import com.poultry.seller.entity.SellerNotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerNotificationPreferencesRepository extends JpaRepository<SellerNotificationPreferences, UUID> {

    Optional<SellerNotificationPreferences> findBySellerId(UUID sellerId);
}
