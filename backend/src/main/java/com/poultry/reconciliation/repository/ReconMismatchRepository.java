package com.poultry.reconciliation.repository;

import com.poultry.reconciliation.entity.ReconMismatch;
import com.poultry.reconciliation.entity.ReconMismatch.MismatchType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReconMismatchRepository
    extends JpaRepository<ReconMismatch, UUID>
{

  @Query("SELECT m FROM ReconMismatch m WHERE m.reconciliationRun.id = :runId ORDER BY m.createdAt DESC")
  Page<ReconMismatch> findByReconciliationRunId(
      @Param("runId")
      UUID runId, Pageable pageable);

  @Query("SELECT m FROM ReconMismatch m WHERE m.reconciliationRun.id = :runId")
  List<ReconMismatch> findAllByReconciliationRunId(
      @Param("runId")
      UUID runId);

  @Query("SELECT m FROM ReconMismatch m WHERE m.resolved = false ORDER BY m.createdAt DESC")
  Page<ReconMismatch> findUnresolvedMismatches(Pageable pageable);

  @Query("SELECT m FROM ReconMismatch m WHERE m.reconciliationRun.id = :runId AND m.resolved = false")
  List<ReconMismatch> findUnresolvedByRunId(
      @Param("runId")
      UUID runId);

  @Query("SELECT COUNT(m) FROM ReconMismatch m WHERE m.reconciliationRun.id = :runId")
  long countByReconciliationRunId(
      @Param("runId")
      UUID runId);

  @Query("SELECT COUNT(m) FROM ReconMismatch m WHERE m.reconciliationRun.id = :runId AND m.resolved = false")
  long countUnresolvedByRunId(
      @Param("runId")
      UUID runId);

  @Query("SELECT COUNT(m) FROM ReconMismatch m WHERE m.resolved = false")
  long countUnresolved();

  @Query("SELECT m FROM ReconMismatch m WHERE m.mismatchType = :type AND m.resolved = false")
  List<ReconMismatch> findUnresolvedByType(
      @Param("type")
      MismatchType type);
}
