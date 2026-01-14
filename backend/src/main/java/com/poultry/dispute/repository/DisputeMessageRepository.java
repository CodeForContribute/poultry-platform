package com.poultry.dispute.repository;

import com.poultry.dispute.entity.DisputeMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisputeMessageRepository
    extends JpaRepository<DisputeMessage, UUID>
{

  @Query(
      "SELECT m FROM DisputeMessage m WHERE m.dispute.id = :disputeId AND m.isInternal = false ORDER BY m.createdAt ASC")
  List<DisputeMessage> findByDisputeIdPublic(
      @Param("disputeId")
      UUID disputeId);

  @Query("SELECT m FROM DisputeMessage m WHERE m.dispute.id = :disputeId ORDER BY m.createdAt ASC")
  List<DisputeMessage> findByDisputeIdAll(
      @Param("disputeId")
      UUID disputeId);

  @Query("SELECT m FROM DisputeMessage m WHERE m.dispute.id = :disputeId ORDER BY m.createdAt DESC")
  Page<DisputeMessage> findByDisputeIdPaged(
      @Param("disputeId")
      UUID disputeId, Pageable pageable);

  long countByDisputeId(UUID disputeId);
}
