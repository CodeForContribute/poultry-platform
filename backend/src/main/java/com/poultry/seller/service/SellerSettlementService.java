package com.poultry.seller.service;

import com.poultry.common.exception.BusinessException;
import com.poultry.common.service.EncryptionService;
import com.poultry.order.entity.Order;
import com.poultry.order.repository.OrderRepository;
import com.poultry.product.entity.Seller;
import com.poultry.product.repository.SellerRepository;
import com.poultry.seller.dto.*;
import com.poultry.seller.entity.SellerBankAccount;
import com.poultry.seller.repository.SellerBankAccountRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerSettlementService {

    private final SettlementRepository settlementRepository;
    private final SellerRepository sellerRepository;
    private final SellerBankAccountRepository bankAccountRepository;
    private final OrderRepository orderRepository;
    private final EncryptionService encryptionService;

    @Transactional(readOnly = true)
    public Page<SettlementDto> getSettlements(UUID sellerId, String status, LocalDate startDate,
                                               LocalDate endDate, Pageable pageable) {
        log.info("Fetching settlements for seller: {}", sellerId);

        SettlementStatus settlementStatus = status != null && !status.equals("ALL")
                ? SettlementStatus.valueOf(status)
                : null;

        Page<Settlement> settlements = settlementRepository.findWithFilters(
                sellerId, settlementStatus, startDate, endDate, pageable);

        Seller seller = sellerRepository.findById(sellerId).orElse(null);

        return settlements.map(s -> {
            SettlementDto dto = SettlementDto.fromEntity(s);
            if (seller != null && seller.getBankAccountNumberEncrypted() != null) {
                try {
                    String accountNumber = encryptionService.decrypt(seller.getBankAccountNumberEncrypted());
                    dto.setBankAccountLast4(accountNumber.substring(accountNumber.length() - 4));
                    dto.setBankName(seller.getBankName());
                } catch (Exception e) {
                    log.warn("Failed to decrypt bank account for seller {}", sellerId);
                }
            }
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public SettlementDto getSettlementById(UUID sellerId, UUID settlementId) {
        log.info("Fetching settlement {} for seller: {}", settlementId, sellerId);

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> BusinessException.notFound("Settlement", settlementId));

        if (!settlement.getSellerId().equals(sellerId)) {
            throw BusinessException.forbidden("You don't have access to this settlement");
        }

        return SettlementDto.fromEntity(settlement);
    }

    @Transactional(readOnly = true)
    public SettlementSummaryDto getSettlementSummary(UUID sellerId) {
        log.info("Fetching settlement summary for seller: {}", sellerId);

        // Get settlement counts by status
        List<Settlement> allSettlements = settlementRepository.findBySellerId(sellerId, Pageable.unpaged()).getContent();

        long pendingCount = allSettlements.stream()
                .filter(s -> s.getStatus() == SettlementStatus.PENDING || s.getStatus() == SettlementStatus.APPROVED)
                .count();
        long completedCount = allSettlements.stream()
                .filter(s -> s.getStatus() == SettlementStatus.SUCCESS)
                .count();
        long failedCount = allSettlements.stream()
                .filter(s -> s.getStatus() == SettlementStatus.FAILED)
                .count();

        BigDecimal totalSettled = settlementRepository.getTotalSettledAmount(sellerId);
        if (totalSettled == null) totalSettled = BigDecimal.ZERO;

        BigDecimal pendingAmount = allSettlements.stream()
                .filter(s -> s.getStatus() == SettlementStatus.PENDING || s.getStatus() == SettlementStatus.APPROVED)
                .map(Settlement::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate this month and last month earnings
        YearMonth thisMonth = YearMonth.now();
        YearMonth lastMonth = thisMonth.minusMonths(1);

        BigDecimal thisMonthEarnings = allSettlements.stream()
                .filter(s -> s.getStatus() == SettlementStatus.SUCCESS)
                .filter(s -> YearMonth.from(s.getPeriodEnd()).equals(thisMonth))
                .map(Settlement::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal lastMonthEarnings = allSettlements.stream()
                .filter(s -> s.getStatus() == SettlementStatus.SUCCESS)
                .filter(s -> YearMonth.from(s.getPeriodEnd()).equals(lastMonth))
                .map(Settlement::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SettlementSummaryDto.builder()
                .totalSettlements(allSettlements.size())
                .pendingSettlements(pendingCount)
                .completedSettlements(completedCount)
                .failedSettlements(failedCount)
                .totalSettledAmount(totalSettled)
                .pendingAmount(pendingAmount)
                .thisMonthEarnings(thisMonthEarnings)
                .lastMonthEarnings(lastMonthEarnings)
                .build();
    }

    @Transactional(readOnly = true)
    public WalletDto getWallet(UUID sellerId) {
        log.info("Fetching wallet for seller: {}", sellerId);

        // Calculate available balance (completed settlements)
        BigDecimal totalSettled = settlementRepository.getTotalSettledAmount(sellerId);
        if (totalSettled == null) totalSettled = BigDecimal.ZERO;

        // Calculate pending balance (orders delivered but not yet settled)
        List<Order.OrderStatus> deliveredStatuses = List.of(Order.OrderStatus.DELIVERED);
        BigDecimal pendingAmount = orderRepository.sumTotalAmountBySellerIdAndStatusIn(sellerId, deliveredStatuses);
        if (pendingAmount == null) pendingAmount = BigDecimal.ZERO;

        // Calculate total earnings (all successful settlements + pending)
        BigDecimal totalEarnings = totalSettled.add(pendingAmount);

        // For now, withdrawn equals settled amount (in a real system, would track withdrawals separately)
        BigDecimal totalWithdrawn = totalSettled;

        return WalletDto.builder()
                .availableBalance(totalSettled.subtract(totalWithdrawn))
                .pendingBalance(pendingAmount)
                .totalEarnings(totalEarnings)
                .totalWithdrawn(totalWithdrawn)
                .lastUpdated(Instant.now())
                .build();
    }

    @Transactional
    public SettlementDto requestWithdrawal(UUID sellerId, WithdrawalRequest request) {
        log.info("Processing withdrawal request for seller: {}", sellerId);

        // Verify bank account exists and belongs to seller
        SellerBankAccount bankAccount = bankAccountRepository.findByIdAndSellerId(request.getBankAccountId(), sellerId)
                .orElseThrow(() -> BusinessException.notFound("Bank account", request.getBankAccountId()));

        // Check available balance
        WalletDto wallet = getWallet(sellerId);
        if (request.getAmount().compareTo(wallet.getAvailableBalance()) > 0) {
            throw new BusinessException(
                    "Insufficient balance. Available: ₹" + wallet.getAvailableBalance(),
                    "INSUFFICIENT_BALANCE",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Create a manual withdrawal settlement
        Settlement settlement = Settlement.builder()
                .sellerId(sellerId)
                .periodStart(LocalDate.now())
                .periodEnd(LocalDate.now())
                .grossAmount(request.getAmount())
                .platformFeeTotal(BigDecimal.ZERO)
                .tdsAmount(BigDecimal.ZERO)
                .otherDeductions(BigDecimal.ZERO)
                .netAmount(request.getAmount())
                .orderCount(0)
                .status(SettlementStatus.PENDING)
                .payoutMethod("MANUAL_WITHDRAWAL")
                .build();

        settlement = settlementRepository.save(settlement);
        log.info("Withdrawal request {} created for seller: {}", settlement.getId(), sellerId);

        SettlementDto dto = SettlementDto.fromEntity(settlement);
        dto.setBankAccountLast4(bankAccount.getAccountNumberLast4());
        dto.setBankName(bankAccount.getBankName());

        return dto;
    }

    @Transactional(readOnly = true)
    public byte[] exportSettlement(UUID sellerId, UUID settlementId, String format) {
        log.info("Exporting settlement {} as {} for seller: {}", settlementId, format, sellerId);

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> BusinessException.notFound("Settlement", settlementId));

        if (!settlement.getSellerId().equals(sellerId)) {
            throw BusinessException.forbidden("You don't have access to this settlement");
        }

        // Generate export content based on format
        if ("csv".equalsIgnoreCase(format)) {
            return generateCsvExport(settlement);
        } else if ("pdf".equalsIgnoreCase(format)) {
            return generatePdfExport(settlement);
        } else {
            throw new BusinessException("Unsupported export format: " + format, "INVALID_FORMAT", HttpStatus.BAD_REQUEST);
        }
    }

    private byte[] generateCsvExport(Settlement settlement) {
        StringBuilder csv = new StringBuilder();
        csv.append("Settlement Details\n");
        csv.append("Period,").append(settlement.getPeriodStart()).append(" to ").append(settlement.getPeriodEnd()).append("\n");
        csv.append("Order Count,").append(settlement.getOrderCount()).append("\n");
        csv.append("Gross Amount,").append(settlement.getGrossAmount()).append("\n");
        csv.append("Platform Fee,").append(settlement.getPlatformFeeTotal()).append("\n");
        csv.append("TDS Amount,").append(settlement.getTdsAmount()).append("\n");
        csv.append("Other Deductions,").append(settlement.getOtherDeductions()).append("\n");
        csv.append("Net Amount,").append(settlement.getNetAmount()).append("\n");
        csv.append("Status,").append(settlement.getStatus()).append("\n");

        return csv.toString().getBytes();
    }

    private byte[] generatePdfExport(Settlement settlement) {
        // In a real implementation, would use iText or similar library
        // For now, return a simple text representation
        StringBuilder content = new StringBuilder();
        content.append("SETTLEMENT STATEMENT\n");
        content.append("=====================\n\n");
        content.append("Period: ").append(settlement.getPeriodStart()).append(" to ").append(settlement.getPeriodEnd()).append("\n");
        content.append("Orders: ").append(settlement.getOrderCount()).append("\n\n");
        content.append("FINANCIAL SUMMARY\n");
        content.append("-----------------\n");
        content.append("Gross Amount:      Rs ").append(settlement.getGrossAmount()).append("\n");
        content.append("Platform Fee:      Rs ").append(settlement.getPlatformFeeTotal()).append("\n");
        content.append("TDS:               Rs ").append(settlement.getTdsAmount()).append("\n");
        content.append("Other Deductions:  Rs ").append(settlement.getOtherDeductions()).append("\n");
        content.append("-----------------\n");
        content.append("Net Amount:        Rs ").append(settlement.getNetAmount()).append("\n\n");
        content.append("Status: ").append(settlement.getStatus()).append("\n");

        return content.toString().getBytes();
    }
}
