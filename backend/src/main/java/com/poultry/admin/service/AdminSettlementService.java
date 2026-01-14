package com.poultry.admin.service;

import com.poultry.admin.dto.SettlementListDto;
import com.poultry.common.exception.BusinessException;
import com.poultry.ledger.service.LedgerService;
import com.poultry.product.repository.SellerRepository;
import com.poultry.settlement.entity.Settlement;
import com.poultry.settlement.entity.Settlement.SettlementStatus;
import com.poultry.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSettlementService
{

  @Transactional(readOnly = true)
  public Page<SettlementListDto> getSettlements(UUID sellerId, String status, LocalDate startDate,
                                                LocalDate endDate, Pageable pageable)
  {
    log.info("Fetching settlements with filters - sellerId: {}, status: {}", sellerId, status);

    SettlementStatus settlementStatus = status != null ? SettlementStatus.valueOf(status) : null;

    Page<Settlement> settlements = settlementRepository.findWithFilters(
        sellerId, settlementStatus, startDate, endDate, pageable);

    return settlements.map(this::toSettlementListDto);
  }

  @Transactional(readOnly = true)
  public Page<SettlementListDto> getPendingSettlements(Pageable pageable)
  {
    log.info("Fetching pending settlements for approval");
    return settlementRepository.findPendingSettlements(pageable).map(this::toSettlementListDto);
  }

  @Transactional(readOnly = true)
  public Settlement getSettlementById(UUID settlementId)
  {
    return settlementRepository.findById(settlementId)
                               .orElseThrow(() -> new BusinessException("Settlement not found", "SETTLEMENT_NOT_FOUND", HttpStatus.NOT_FOUND));
  }

  @Transactional
  public Settlement approveSettlement(UUID settlementId, UUID adminId, String remarks)
  {
    log.info("Admin {} approving settlement {}", adminId, settlementId);

    Settlement settlement = getSettlementById(settlementId);

    if (settlement.getStatus() != SettlementStatus.PENDING)
    {
      throw new BusinessException("Settlement is not in pending status", "INVALID_STATUS", HttpStatus.BAD_REQUEST);
    }

    settlement.setStatus(SettlementStatus.APPROVED);
    settlement.setApprovedBy(adminId);
    settlement.setApprovedAt(Instant.now());
    settlement.setApprovalRemarks(remarks);

    settlement = settlementRepository.save(settlement);

    log.info("Settlement {} approved by admin {}", settlementId, adminId);
    return settlement;
  }

  @Transactional
  public Settlement rejectSettlement(UUID settlementId, UUID adminId, String reason)
  {
    log.info("Admin {} rejecting settlement {} with reason: {}", adminId, settlementId, reason);

    Settlement settlement = getSettlementById(settlementId);

    if (settlement.getStatus() != SettlementStatus.PENDING)
    {
      throw new BusinessException("Settlement is not in pending status", "INVALID_STATUS", HttpStatus.BAD_REQUEST);
    }

    settlement.setStatus(SettlementStatus.CANCELLED);
    settlement.setApprovedBy(adminId);
    settlement.setApprovedAt(Instant.now());
    settlement.setApprovalRemarks("Rejected: " + reason);

    settlement = settlementRepository.save(settlement);

    log.info("Settlement {} rejected by admin {}", settlementId, adminId);
    return settlement;
  }

  @Transactional
  public Settlement initiateSettlement(UUID settlementId, UUID adminId)
  {
    log.info("Admin {} initiating settlement {}", adminId, settlementId);

    Settlement settlement = getSettlementById(settlementId);

    if (settlement.getStatus() != SettlementStatus.APPROVED)
    {
      throw new BusinessException("Settlement must be approved before initiation", "INVALID_STATUS", HttpStatus.BAD_REQUEST);
    }

    settlement.setStatus(SettlementStatus.PROCESSING);
    settlement.setInitiatedAt(Instant.now());

    settlement = settlementRepository.save(settlement);

    // TODO: Trigger actual payout via Razorpay/bank API

    log.info("Settlement {} processing initiated by admin {}", settlementId, adminId);
    return settlement;
  }

  @Transactional
  public Settlement markSettlementSuccess(UUID settlementId, String bankReference, UUID adminId)
  {
    log.info("Admin {} marking settlement {} as success with reference: {}", adminId, settlementId, bankReference);

    Settlement settlement = getSettlementById(settlementId);

    if (settlement.getStatus() != SettlementStatus.PROCESSING)
    {
      throw new BusinessException("Settlement is not in processing status", "INVALID_STATUS", HttpStatus.BAD_REQUEST);
    }

    settlement.setStatus(SettlementStatus.SUCCESS);
    settlement.setBankReference(bankReference);
    settlement.setCompletedAt(Instant.now());

    settlement = settlementRepository.save(settlement);

    // Record in ledger
    ledgerService.recordSettlement(settlementId, settlement.getSellerId(), settlement.getNetAmount());

    log.info("Settlement {} marked as success", settlementId);
    return settlement;
  }

  @Transactional
  public Settlement markSettlementFailed(UUID settlementId, String reason, UUID adminId)
  {
    log.info("Admin {} marking settlement {} as failed: {}", adminId, settlementId, reason);

    Settlement settlement = getSettlementById(settlementId);

    settlement.setStatus(SettlementStatus.FAILED);
    settlement.setFailureCount(settlement.getFailureCount() + 1);
    settlement.setFailureReason(reason);
    settlement.setLastFailureAt(Instant.now());

    settlement = settlementRepository.save(settlement);

    log.info("Settlement {} marked as failed", settlementId);
    return settlement;
  }

  @Transactional
  public Settlement retrySettlement(UUID settlementId, UUID adminId)
  {
    log.info("Admin {} retrying settlement {}", adminId, settlementId);

    Settlement settlement = getSettlementById(settlementId);

    if (settlement.getStatus() != SettlementStatus.FAILED)
    {
      throw new BusinessException("Only failed settlements can be retried", "INVALID_STATUS", HttpStatus.BAD_REQUEST);
    }

    if (settlement.getFailureCount() >= 3)
    {
      throw new BusinessException("Settlement has exceeded max retry attempts", "MAX_RETRIES_EXCEEDED", HttpStatus.BAD_REQUEST);
    }

    settlement.setStatus(SettlementStatus.APPROVED);

    settlement = settlementRepository.save(settlement);

    log.info("Settlement {} reset for retry by admin {}", settlementId, adminId);
    return settlement;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getSettlementStats()
  {
    Map<String, Object> stats = new HashMap<>();

    stats.put("pendingCount", settlementRepository.countPendingSettlements());
    stats.put("pendingAmount", settlementRepository.getTotalPendingAmount());
    stats.put("failedRetryableCount", settlementRepository.countFailedRetryable());

    return stats;
  }

  private SettlementListDto toSettlementListDto(Settlement settlement)
  {
    SettlementListDto dto = SettlementListDto.fromEntity(settlement);

    // Enrich with seller info
    sellerRepository.findById(settlement.getSellerId()).ifPresent(seller ->
                                                                  {
                                                                    dto.setSellerName(seller.getBusinessName());
                                                                    dto.setSellerBusinessName(seller.getBusinessName());
                                                                  });

    return dto;
  }

  private final SettlementRepository settlementRepository;
  private final SellerRepository sellerRepository;
  private final LedgerService ledgerService;
}
