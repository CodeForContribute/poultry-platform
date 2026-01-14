package com.poultry.verification.repository;

import com.poultry.verification.entity.SellerVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerVerificationRepository extends JpaRepository<SellerVerification, UUID> {

    List<SellerVerification> findBySellerId(UUID sellerId);

    Optional<SellerVerification> findBySellerIdAndVerificationType(UUID sellerId, SellerVerification.VerificationType type);

    List<SellerVerification> findByStatus(SellerVerification.VerificationStatus status);

    List<SellerVerification> findBySellerIdAndStatus(UUID sellerId, SellerVerification.VerificationStatus status);
}
