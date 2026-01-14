package com.poultry.auth.repository;

import com.poultry.auth.entity.BuyerSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BuyerSessionRepository extends JpaRepository<BuyerSession, UUID> {

    @Query("SELECT bs FROM BuyerSession bs WHERE bs.buyerId = :buyerId " +
            "AND bs.revoked = false ORDER BY bs.createdAt DESC")
    List<BuyerSession> findActiveSessionsByBuyer(UUID buyerId);

    @Query("SELECT COUNT(bs) FROM BuyerSession bs WHERE bs.buyerId = :buyerId " +
            "AND bs.revoked = false AND bs.expiresAt > :now")
    long countActiveSessions(UUID buyerId, Instant now);

    Optional<BuyerSession> findByRefreshTokenHash(String refreshTokenHash);

    @Query("SELECT bs FROM BuyerSession bs WHERE bs.buyerId = :buyerId " +
            "AND bs.deviceId = :deviceId AND bs.revoked = false AND bs.expiresAt > :now")
    Optional<BuyerSession> findActiveSessionByDevice(UUID buyerId, String deviceId, Instant now);

    @Modifying
    @Query("UPDATE BuyerSession bs SET bs.revoked = true, bs.revokedAt = :now, " +
            "bs.revokeReason = :reason WHERE bs.buyerId = :buyerId AND bs.revoked = false")
    int revokeAllBuyerSessions(UUID buyerId, Instant now, String reason);

    @Query("SELECT bs FROM BuyerSession bs WHERE bs.buyerId = :buyerId " +
            "AND bs.revoked = false AND bs.expiresAt > :now ORDER BY bs.lastActiveAt ASC")
    List<BuyerSession> findOldestActiveSessions(UUID buyerId, Instant now);
}
