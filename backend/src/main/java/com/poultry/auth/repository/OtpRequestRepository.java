package com.poultry.auth.repository;

import com.poultry.auth.entity.OtpRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRequestRepository extends JpaRepository<OtpRequest, UUID> {

    @Query("SELECT o FROM OtpRequest o WHERE o.phoneHash = :phoneHash " +
            "AND o.purpose = :purpose AND o.verified = false AND o.expiresAt > :now " +
            "ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpRequest> findLatestValidOtp(String phoneHash, OtpRequest.OtpPurpose purpose, Instant now);

    @Query("SELECT COUNT(o) FROM OtpRequest o WHERE o.phoneHash = :phoneHash " +
            "AND o.createdAt > :since")
    long countRecentRequests(String phoneHash, Instant since);

    @Query("SELECT COUNT(o) FROM OtpRequest o WHERE o.phoneHash = :phoneHash " +
            "AND o.purpose = :purpose AND o.createdAt > :since")
    long countRecentRequestsByPurpose(String phoneHash, OtpRequest.OtpPurpose purpose, Instant since);
}
