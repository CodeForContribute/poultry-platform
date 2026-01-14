package com.poultry.settlement.repository;

import com.poultry.settlement.entity.Settlement;
import com.poultry.settlement.entity.Settlement.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementRepository
    extends JpaRepository<Settlement, UUID>
{

  Page<Settlement> findByStatus(SettlementStatus status, Pageable pageable);

  Page<Settlement> findBySellerId(UUID sellerId, Pageable pageable);

  @Query("SELECT s FROM Settlement s WHERE s.status = :status AND s.scheduledFor <= :now")
  List<Settlement> findDueSettlements(
      @Param("status")
      SettlementStatus status,
      @Param("now")
      Instant now);

  @Query("SELECT s FROM Settlement s WHERE s.status = 'PENDING' ORDER BY s.createdAt DESC")
  Page<Settlement> findPendingSettlements(Pageable pageable);

  @Query("SELECT s FROM Settlement s WHERE " +
      "(:sellerId IS NULL OR s.sellerId = :sellerId) AND " +
      "(:status IS NULL OR s.status = :status) AND " +
      "(:startDate IS NULL OR s.periodStart >= :startDate) AND " +
      "(:endDate IS NULL OR s.periodEnd <= :endDate)")
  Page<Settlement> findWithFilters(
      @Param("sellerId")
      UUID sellerId,
      @Param("status")
      SettlementStatus status,
      @Param("startDate")
      LocalDate startDate,
      @Param("endDate")
      LocalDate endDate,
      Pageable pageable);

  @Query("SELECT SUM(s.netAmount) FROM Settlement s WHERE s.sellerId = :sellerId AND s.status = 'SUCCESS'")
  BigDecimal getTotalSettledAmount(
      @Param("sellerId")
      UUID sellerId);

  @Query("SELECT SUM(s.netAmount) FROM Settlement s WHERE s.status = 'PENDING'")
  BigDecimal getTotalPendingAmount();

  @Query("SELECT COUNT(s) FROM Settlement s WHERE s.status = 'PENDING'")
  Long countPendingSettlements();

  @Query("SELECT COUNT(s) FROM Settlement s WHERE s.status = 'FAILED' AND s.failureCount < 3")
  Long countFailedRetryable();
}
