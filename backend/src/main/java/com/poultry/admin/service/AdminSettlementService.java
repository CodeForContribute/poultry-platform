package com.poultry.admin.service;

import com.poultry.admin.dto.SettlementListDto;
import com.poultry.common.exception.BusinessException;
import com.poultry.common.service.EncryptionService;
import com.poultry.ledger.service.LedgerService;
import com.poultry.payment.service.RazorpayService;
import com.poultry.product.entity.Seller;
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

    // Get seller details for payout
    Seller seller = sellerRepository.findById(settlement.getSellerId())
        .orElseThrow(() -> new BusinessException("Seller not found", "SELLER_NOT_FOUND", HttpStatus.NOT_FOUND));

    settlement.setStatus(SettlementStatus.PROCESSING);
    settlement.setInitiatedAt(Instant.now());
    settlement = settlementRepository.save(settlement);

    // Trigger actual payout via Razorpay
    try {
      // Create or get existing fund account for seller
      String fundAccountId = getOrCreateFundAccount(seller);

      // Create payout
      Map<String, Object> payoutResponse = razorpayService.createPayout(
          fundAccountId,
          settlement.getNetAmount(),
          "INR",
          "NEFT", // or IMPS, RTGS based on amount/urgency
          "payout",
          "SETT_" + settlementId.toString().substring(0, 8),
          "Settlement payout for period " + settlement.getPeriodStart() + " to " + settlement.getPeriodEnd()
      );

      // Store payout reference
      String payoutId = (String) payoutResponse.get("id");
      String utr = (String) payoutResponse.get("utr");
      settlement.setBankReference(payoutId);
      if (utr != null) {
        settlement.setBankReference(payoutId + "/" + utr);
      }
      settlement = settlementRepository.save(settlement);

      log.info("Payout created for settlement {}: payoutId={}", settlementId, payoutId);
    } catch (Exception e) {
      log.error("Failed to create payout for settlement {}: {}", settlementId, e.getMessage());
      // Don't fail the settlement - it's in PROCESSING state and can be retried
      // The failure will be detected by a scheduled job or webhook
    }

    log.info("Settlement {} processing initiated by admin {}", settlementId, adminId);
    return settlement;
  }

  private String getOrCreateFundAccount(Seller seller) {
    // Check if seller already has a stored fund account ID to avoid duplicates
    if (seller.getRazorpayFundAccountId() != null) {
      log.debug("Using existing Razorpay fund account {} for seller {}",
          seller.getRazorpayFundAccountId(), seller.getId());
      return seller.getRazorpayFundAccountId();
    }

    String contactId = seller.getRazorpayContactId();

    // Create contact if not exists
    if (contactId == null) {
      log.info("Creating new Razorpay contact for seller {}", seller.getId());

      // Decrypt phone number if available
      String phone = null;
      if (seller.getPhoneEncrypted() != null) {
        try {
          phone = encryptionService.decrypt(seller.getPhoneEncrypted());
        } catch (Exception e) {
          log.warn("Failed to decrypt phone for seller {}: {}", seller.getId(), e.getMessage());
        }
      }

      Map<String, Object> contact = razorpayService.createContact(
          seller.getBusinessName(),
          seller.getEmail(),
          phone,
          "vendor",
          "SELLER_" + seller.getId().toString().substring(0, 8)
      );
      contactId = (String) contact.get("id");

      // Store contact ID on seller for future use
      seller.setRazorpayContactId(contactId);
      sellerRepository.save(seller);
      log.info("Created and stored Razorpay contact {} for seller {}", contactId, seller.getId());
    }

    // Decrypt bank account number
    String bankAccountNumber;
    if (seller.getBankAccountNumberEncrypted() == null) {
      throw new BusinessException(
          "Bank account number not configured for seller",
          "BANK_ACCOUNT_MISSING",
          HttpStatus.BAD_REQUEST
      );
    }

    try {
      bankAccountNumber = encryptionService.decrypt(seller.getBankAccountNumberEncrypted());
    } catch (Exception e) {
      log.error("Failed to decrypt bank account number for seller {}: {}", seller.getId(), e.getMessage());
      throw new BusinessException(
          "Failed to decrypt bank account details",
          "DECRYPTION_ERROR",
          HttpStatus.INTERNAL_SERVER_ERROR
      );
    }

    // Create fund account
    log.info("Creating new Razorpay fund account for seller {}", seller.getId());
    Map<String, Object> fundAccount = razorpayService.createFundAccount(
        contactId,
        bankAccountNumber,
        seller.getBankIfsc(),
        seller.getBusinessName()
    );

    String fundAccountId = (String) fundAccount.get("id");

    // Store fund account ID on seller for future use
    seller.setRazorpayFundAccountId(fundAccountId);
    sellerRepository.save(seller);
    log.info("Created and stored Razorpay fund account {} for seller {}", fundAccountId, seller.getId());

    return fundAccountId;
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
  private final RazorpayService razorpayService;
  private final EncryptionService encryptionService;
}
