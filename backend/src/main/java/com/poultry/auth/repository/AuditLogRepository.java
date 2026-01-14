package com.poultry.auth.repository;

import com.poultry.auth.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByUserTypeAndUserId(String userType, UUID userId, Pageable pageable);

    Page<AuditLog> findByAction(AuditLog.AuditAction action, Pageable pageable);

    Page<AuditLog> findByResourceTypeAndResourceId(String resourceType, UUID resourceId, Pageable pageable);

    List<AuditLog> findByUserTypeAndUserIdAndActionAndCreatedAtAfter(
            String userType, UUID userId, AuditLog.AuditAction action, Instant since);

    long countByUserTypeAndUserIdAndActionAndOutcomeAndCreatedAtAfter(
            String userType, UUID userId, AuditLog.AuditAction action, String outcome, Instant since);
}
