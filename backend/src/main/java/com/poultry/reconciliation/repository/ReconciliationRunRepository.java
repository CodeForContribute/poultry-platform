package com.poultry.reconciliation.repository;

import com.poultry.reconciliation.entity.ReconciliationRun;
import com.poultry.reconciliation.entity.ReconciliationRun.RunStatus;
import com.poultry.reconciliation.entity.ReconciliationRun.SourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReconciliationRunRepository
    extends JpaRepository<ReconciliationRun, UUID>
{

  Optional<ReconciliationRun> findByRunDate(LocalDate runDate);

  Optional<ReconciliationRun> findByRunDateAndSourceType(LocalDate runDate, SourceType sourceType);

  Page<ReconciliationRun> findByStatus(RunStatus status, Pageable pageable);

  @Query("SELECT r FROM ReconciliationRun r ORDER BY r.runDate DESC")
  Page<ReconciliationRun> findRecentRuns(Pageable pageable);

  @Query("SELECT r FROM ReconciliationRun r WHERE r.status = :status ORDER BY r.runDate DESC LIMIT 1")
  Optional<ReconciliationRun> findLatestByStatus(
      @Param("status")
      RunStatus status);

  @Query("SELECT r FROM ReconciliationRun r ORDER BY r.runDate DESC LIMIT 1")
  Optional<ReconciliationRun> findLatestRun();

  @Query("SELECT COUNT(r) FROM ReconciliationRun r WHERE r.status = :status")
  long countByStatus(
      @Param("status")
      RunStatus status);

  boolean existsByRunDateAndSourceType(LocalDate runDate, SourceType sourceType);
}
