package com.poultry.dispute.repository;

import com.poultry.dispute.entity.Dispute;
import com.poultry.dispute.entity.Dispute.DisputeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DisputeRepository
    extends JpaRepository<Dispute, UUID>
{

  Optional<Dispute> findByDisputeNumber(String disputeNumber);

  Page<Dispute> findByBuyerId(UUID buyerId, Pageable pageable);

  Page<Dispute> findBySellerId(UUID sellerId, Pageable pageable);

  Page<Dispute> findByOrderId(UUID orderId, Pageable pageable);

  @Query("SELECT d FROM Dispute d WHERE d.status IN :statuses ORDER BY d.priority ASC, d.createdAt ASC")
  Page<Dispute> findByStatusIn(
      @Param("statuses")
      List<DisputeStatus> statuses, Pageable pageable);

  @Query(
      "SELECT d FROM Dispute d WHERE d.assignedTo = :adminId AND d.status NOT IN ('RESOLVED', 'CLOSED') ORDER BY d.priority ASC, d.createdAt ASC")
  Page<Dispute> findAssignedTo(
      @Param("adminId")
      UUID adminId, Pageable pageable);

  @Query("SELECT d FROM Dispute d WHERE " +
      "(:buyerId IS NULL OR d.buyerId = :buyerId) AND " +
      "(:sellerId IS NULL OR d.sellerId = :sellerId) AND " +
      "(:status IS NULL OR d.status = :status) AND " +
      "(:type IS NULL OR d.type = :type) " +
      "ORDER BY d.createdAt DESC")
  Page<Dispute> findWithFilters(
      @Param("buyerId")
      UUID buyerId,
      @Param("sellerId")
      UUID sellerId,
      @Param("status")
      DisputeStatus status,
      @Param("type")
      Dispute.DisputeType type,
      Pageable pageable);

  @Query("SELECT COUNT(d) FROM Dispute d WHERE d.status = :status")
  long countByStatus(
      @Param("status")
      DisputeStatus status);

  @Query("SELECT COUNT(d) FROM Dispute d WHERE d.status IN ('OPEN', 'UNDER_REVIEW', 'ESCALATED')")
  long countOpenDisputes();

  @Query("SELECT COUNT(d) FROM Dispute d WHERE d.assignedTo IS NULL AND d.status IN ('OPEN', 'UNDER_REVIEW')")
  long countUnassigned();

  @Query("SELECT d FROM Dispute d WHERE d.status = 'ESCALATED' ORDER BY d.escalatedAt ASC")
  Page<Dispute> findEscalated(Pageable pageable);

  @Query(value = "SELECT nextval('dispute_number_seq')", nativeQuery = true)
  Long getNextDisputeNumber();
}
