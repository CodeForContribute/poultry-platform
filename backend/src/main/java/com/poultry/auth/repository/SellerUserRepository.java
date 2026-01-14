package com.poultry.auth.repository;

import com.poultry.auth.entity.SellerUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerUserRepository extends JpaRepository<SellerUser, UUID> {

    Optional<SellerUser> findByEmail(String email);

    Optional<SellerUser> findBySellerIdAndEmail(UUID sellerId, String email);

    List<SellerUser> findBySellerId(UUID sellerId);

    List<SellerUser> findBySellerIdAndStatus(UUID sellerId, SellerUser.SellerStatus status);

    boolean existsByEmail(String email);

    boolean existsBySellerIdAndEmail(UUID sellerId, String email);

    @Query("SELECT su FROM SellerUser su WHERE su.status = 'ACTIVE' " +
            "AND su.passwordChangedAt < :expiryDate")
    List<SellerUser> findUsersWithExpiredPasswords(Instant expiryDate);

    @Query("SELECT COUNT(su) FROM SellerUser su WHERE su.sellerId = :sellerId " +
            "AND su.status = 'ACTIVE'")
    long countActiveUsersBySeller(UUID sellerId);
}
