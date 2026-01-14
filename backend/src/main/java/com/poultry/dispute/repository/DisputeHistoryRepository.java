package com.poultry.dispute.repository;

import com.poultry.dispute.entity.DisputeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisputeHistoryRepository
    extends JpaRepository<DisputeHistory, UUID>
{

  List<DisputeHistory> findByDisputeIdOrderByCreatedAtAsc(UUID disputeId);

  List<DisputeHistory> findByDisputeIdOrderByCreatedAtDesc(UUID disputeId);
}
