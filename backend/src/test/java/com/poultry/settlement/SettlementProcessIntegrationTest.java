package com.poultry.settlement;

import com.poultry.admin.service.AdminSettlementService;
import com.poultry.common.exception.BusinessException;
import com.poultry.config.BaseIntegrationTest;
import com.poultry.product.entity.Seller;
import com.poultry.product.repository.SellerRepository;
import com.poultry.settlement.entity.Settlement;
import com.poultry.settlement.entity.Settlement.SettlementStatus;
import com.poultry.settlement.repository.SettlementRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Settlement Process Integration Tests")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SettlementProcessIntegrationTest extends BaseIntegrationTest
{

  @Autowired
  private AdminSettlementService adminSettlementService;

  @Autowired
  private SettlementRepository settlementRepository;

  @Autowired
  private SellerRepository sellerRepository;

  private Seller testSeller;
  private UUID adminId;

  @BeforeEach
  void setUp()
  {
    adminId = UUID.randomUUID();

    // Create test seller
    testSeller = Seller.builder()
        .businessName("Settlement Test Farm")
        .phoneEncrypted("settlement-seller-phone".getBytes())
        .email("settlement@test.com")
        .gstin("29SETTLE1234Z1X5")
        .pan("SETTLE1234")
        .status(Seller.SellerStatus.ACTIVE)
        .platformFeePercent(BigDecimal.valueOf(2))
        .settlementCycle(Seller.SettlementCycle.T_PLUS_1)
        .bankIfsc("HDFC0001234")
        .build();
    testSeller = sellerRepository.save(testSeller);
  }

  @AfterEach
  void tearDown()
  {
    settlementRepository.deleteAll();
    sellerRepository.deleteAll();
  }

  @Nested
  @DisplayName("Settlement Approval Workflow")
  class SettlementApprovalTests
  {

    @Test
    @DisplayName("should approve pending settlement")
    void shouldApprovePendingSettlement()
    {
      // Create pending settlement
      Settlement settlement = createPendingSettlement();

      // Approve settlement
      Settlement approved = adminSettlementService.approveSettlement(
          settlement.getId(), adminId, "Verified and approved");

      assertThat(approved.getStatus()).isEqualTo(SettlementStatus.APPROVED);
      assertThat(approved.getApprovedBy()).isEqualTo(adminId);
      assertThat(approved.getApprovedAt()).isNotNull();
      assertThat(approved.getApprovalRemarks()).isEqualTo("Verified and approved");
    }

    @Test
    @DisplayName("should reject pending settlement")
    void shouldRejectPendingSettlement()
    {
      // Create pending settlement
      Settlement settlement = createPendingSettlement();

      // Reject settlement
      Settlement rejected = adminSettlementService.rejectSettlement(
          settlement.getId(), adminId, "Insufficient documentation");

      assertThat(rejected.getStatus()).isEqualTo(SettlementStatus.CANCELLED);
      assertThat(rejected.getApprovedBy()).isEqualTo(adminId);
      assertThat(rejected.getApprovalRemarks()).contains("Rejected:");
    }

    @Test
    @DisplayName("should not approve non-pending settlement")
    void shouldNotApproveNonPendingSettlement()
    {
      // Create already approved settlement
      Settlement settlement = createPendingSettlement();
      settlement.setStatus(SettlementStatus.APPROVED);
      settlement = settlementRepository.save(settlement);

      UUID settlementId = settlement.getId();

      assertThatThrownBy(() ->
          adminSettlementService.approveSettlement(settlementId, adminId, "Trying again"))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("not in pending status");
    }
  }

  @Nested
  @DisplayName("Settlement Processing")
  class SettlementProcessingTests
  {

    @Test
    @DisplayName("should initiate approved settlement for processing")
    void shouldInitiateApprovedSettlement()
    {
      // Create and approve settlement
      Settlement settlement = createPendingSettlement();
      settlement.setStatus(SettlementStatus.APPROVED);
      settlement = settlementRepository.save(settlement);

      // Initiate processing
      Settlement processing = adminSettlementService.initiateSettlement(settlement.getId(), adminId);

      assertThat(processing.getStatus()).isEqualTo(SettlementStatus.PROCESSING);
      assertThat(processing.getInitiatedAt()).isNotNull();
    }

    @Test
    @DisplayName("should not initiate non-approved settlement")
    void shouldNotInitiateNonApprovedSettlement()
    {
      // Create pending (not approved) settlement
      Settlement settlement = createPendingSettlement();
      UUID settlementId = settlement.getId();

      assertThatThrownBy(() ->
          adminSettlementService.initiateSettlement(settlementId, adminId))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("must be approved");
    }

    @Test
    @DisplayName("should mark settlement as success with bank reference")
    void shouldMarkSettlementSuccess()
    {
      // Create processing settlement
      Settlement settlement = createPendingSettlement();
      settlement.setStatus(SettlementStatus.PROCESSING);
      settlement = settlementRepository.save(settlement);

      // Mark as success
      String bankRef = "UTR123456789";
      Settlement success = adminSettlementService.markSettlementSuccess(
          settlement.getId(), bankRef, adminId);

      assertThat(success.getStatus()).isEqualTo(SettlementStatus.SUCCESS);
      assertThat(success.getBankReference()).isEqualTo(bankRef);
      assertThat(success.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("should mark settlement as failed and increment failure count")
    void shouldMarkSettlementFailed()
    {
      // Create processing settlement
      Settlement settlement = createPendingSettlement();
      settlement.setStatus(SettlementStatus.PROCESSING);
      settlement.setFailureCount(0);
      settlement = settlementRepository.save(settlement);

      // Mark as failed
      Settlement failed = adminSettlementService.markSettlementFailed(
          settlement.getId(), "Bank rejected transaction", adminId);

      assertThat(failed.getStatus()).isEqualTo(SettlementStatus.FAILED);
      assertThat(failed.getFailureCount()).isEqualTo(1);
      assertThat(failed.getFailureReason()).isEqualTo("Bank rejected transaction");
      assertThat(failed.getLastFailureAt()).isNotNull();
    }
  }

  @Nested
  @DisplayName("Settlement Retry")
  class SettlementRetryTests
  {

    @Test
    @DisplayName("should allow retry of failed settlement")
    void shouldAllowRetryOfFailedSettlement()
    {
      // Create failed settlement
      Settlement settlement = createPendingSettlement();
      settlement.setStatus(SettlementStatus.FAILED);
      settlement.setFailureCount(1);
      settlement = settlementRepository.save(settlement);

      // Retry
      Settlement retried = adminSettlementService.retrySettlement(settlement.getId(), adminId);

      assertThat(retried.getStatus()).isEqualTo(SettlementStatus.APPROVED);
    }

    @Test
    @DisplayName("should not allow retry if max attempts exceeded")
    void shouldNotAllowRetryIfMaxAttemptsExceeded()
    {
      // Create failed settlement with max retries
      Settlement settlement = createPendingSettlement();
      settlement.setStatus(SettlementStatus.FAILED);
      settlement.setFailureCount(3); // Max retries
      settlement = settlementRepository.save(settlement);

      UUID settlementId = settlement.getId();

      assertThatThrownBy(() ->
          adminSettlementService.retrySettlement(settlementId, adminId))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("max retry");
    }

    @Test
    @DisplayName("should only retry failed settlements")
    void shouldOnlyRetryFailedSettlements()
    {
      // Create pending settlement (not failed)
      Settlement settlement = createPendingSettlement();
      UUID settlementId = settlement.getId();

      assertThatThrownBy(() ->
          adminSettlementService.retrySettlement(settlementId, adminId))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("Only failed settlements");
    }
  }

  @Nested
  @DisplayName("Settlement Queries")
  class SettlementQueryTests
  {

    @Test
    @DisplayName("should get pending settlements for approval")
    void shouldGetPendingSettlements()
    {
      // Create multiple settlements
      createPendingSettlement();
      createPendingSettlement();

      Settlement approved = createPendingSettlement();
      approved.setStatus(SettlementStatus.APPROVED);
      settlementRepository.save(approved);

      // Get pending settlements
      Page<com.poultry.admin.dto.SettlementListDto> pending =
          adminSettlementService.getPendingSettlements(PageRequest.of(0, 10));

      assertThat(pending.getContent()).hasSize(2);
      assertThat(pending.getContent())
          .allMatch(s -> s.getStatus().equals(SettlementStatus.PENDING.name()));
    }

    @Test
    @DisplayName("should get settlement statistics")
    void shouldGetSettlementStats()
    {
      // Create settlements with different statuses
      createPendingSettlement();
      createPendingSettlement();

      Settlement failed = createPendingSettlement();
      failed.setStatus(SettlementStatus.FAILED);
      failed.setFailureCount(1); // Retryable
      settlementRepository.save(failed);

      // Get stats
      var stats = adminSettlementService.getSettlementStats();

      assertThat(stats).containsKey("pendingCount");
      assertThat(stats).containsKey("pendingAmount");
      assertThat(stats).containsKey("failedRetryableCount");
      assertThat((Long) stats.get("pendingCount")).isEqualTo(2);
      assertThat((Long) stats.get("failedRetryableCount")).isEqualTo(1);
    }

    @Test
    @DisplayName("should filter settlements by seller and status")
    void shouldFilterSettlements()
    {
      // Create settlements for test seller
      createPendingSettlement();

      Settlement approved = createPendingSettlement();
      approved.setStatus(SettlementStatus.APPROVED);
      settlementRepository.save(approved);

      // Create settlement for different seller
      Seller otherSeller = Seller.builder()
          .businessName("Other Farm")
          .phoneEncrypted("other-seller-phone".getBytes())
          .email("other@test.com")
          .gstin("29OTHER1234Z1X5")
          .pan("OTHER1234")
          .status(Seller.SellerStatus.ACTIVE)
          .platformFeePercent(BigDecimal.valueOf(2))
          .settlementCycle(Seller.SettlementCycle.T_PLUS_1)
          .build();
      otherSeller = sellerRepository.save(otherSeller);

      Settlement otherSettlement = Settlement.builder()
          .sellerId(otherSeller.getId())
          .periodStart(LocalDate.now().minusDays(7))
          .periodEnd(LocalDate.now())
          .grossAmount(BigDecimal.valueOf(5000))
          .platformFeeTotal(BigDecimal.valueOf(100))
          .netAmount(BigDecimal.valueOf(4900))
          .orderCount(5)
          .status(SettlementStatus.PENDING)
          .build();
      settlementRepository.save(otherSettlement);

      // Filter by test seller
      Page<com.poultry.admin.dto.SettlementListDto> sellerSettlements =
          adminSettlementService.getSettlements(
              testSeller.getId(), null, null, null, PageRequest.of(0, 10));

      assertThat(sellerSettlements.getContent()).hasSize(2);

      // Filter by status
      Page<com.poultry.admin.dto.SettlementListDto> approvedSettlements =
          adminSettlementService.getSettlements(
              null, "APPROVED", null, null, PageRequest.of(0, 10));

      assertThat(approvedSettlements.getContent()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("Settlement Full Workflow")
  class SettlementFullWorkflowTests
  {

    @Test
    @DisplayName("should complete full settlement workflow: pending -> approved -> processing -> success")
    void shouldCompleteFullSettlementWorkflow()
    {
      // Step 1: Create pending settlement
      Settlement settlement = createPendingSettlement();
      assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PENDING);

      // Step 2: Approve
      settlement = adminSettlementService.approveSettlement(
          settlement.getId(), adminId, "Approved for processing");
      assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.APPROVED);

      // Step 3: Initiate processing (triggers payout)
      settlement = adminSettlementService.initiateSettlement(settlement.getId(), adminId);
      assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PROCESSING);

      // Step 4: Mark as success (simulating webhook/callback from payment gateway)
      settlement = adminSettlementService.markSettlementSuccess(
          settlement.getId(), "UTR987654321", adminId);
      assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.SUCCESS);
      assertThat(settlement.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("should handle settlement failure and retry")
    void shouldHandleSettlementFailureAndRetry()
    {
      // Create and approve settlement
      Settlement settlement = createPendingSettlement();
      settlement = adminSettlementService.approveSettlement(
          settlement.getId(), adminId, "Approved");

      // Initiate processing
      settlement = adminSettlementService.initiateSettlement(settlement.getId(), adminId);

      // Mark as failed
      settlement = adminSettlementService.markSettlementFailed(
          settlement.getId(), "Bank system down", adminId);
      assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.FAILED);
      assertThat(settlement.getFailureCount()).isEqualTo(1);

      // Retry
      settlement = adminSettlementService.retrySettlement(settlement.getId(), adminId);
      assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.APPROVED);

      // Re-initiate
      settlement = adminSettlementService.initiateSettlement(settlement.getId(), adminId);
      assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PROCESSING);

      // This time success
      settlement = adminSettlementService.markSettlementSuccess(
          settlement.getId(), "UTR123123123", adminId);
      assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.SUCCESS);
    }
  }

  private Settlement createPendingSettlement()
  {
    Settlement settlement = Settlement.builder()
        .sellerId(testSeller.getId())
        .periodStart(LocalDate.now().minusDays(7))
        .periodEnd(LocalDate.now())
        .grossAmount(BigDecimal.valueOf(10000))
        .platformFeeTotal(BigDecimal.valueOf(200))
        .tdsAmount(BigDecimal.valueOf(100))
        .netAmount(BigDecimal.valueOf(9700))
        .orderCount(10)
        .status(SettlementStatus.PENDING)
        .build();
    return settlementRepository.save(settlement);
  }
}
