package com.poultry.auth.repository;

import com.poultry.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash " +
            "AND rt.revoked = false AND rt.expiresAt > :now")
    Optional<RefreshToken> findValidToken(String tokenHash, Instant now);

    List<RefreshToken> findByUserTypeAndUserIdAndRevokedFalse(String userType, UUID userId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now, " +
            "rt.revokeReason = :reason WHERE rt.userType = :userType AND rt.userId = :userId " +
            "AND rt.revoked = false")
    int revokeAllUserTokens(String userType, UUID userId, Instant now, String reason);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoff")
    int deleteExpiredTokens(Instant cutoff);

    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.userId = :userId " +
            "AND rt.userType = :userType AND rt.revoked = false AND rt.expiresAt > :now")
    long countActiveTokens(UUID userId, String userType, Instant now);
}
