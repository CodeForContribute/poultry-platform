package com.poultry.ledger.service;

import com.poultry.common.exception.BusinessException;
import com.poultry.ledger.entity.LedgerEntry;
import com.poultry.ledger.entity.LedgerEntry.AccountType;
import com.poultry.ledger.entity.LedgerEntry.Direction;
import com.poultry.ledger.entity.LedgerEntry.ReferenceType;
import com.poultry.ledger.repository.LedgerEntryRepository;
import com.poultry.order.entity.Order;
import com.poultry.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final OrderRepository orderRepository;

    /**
     * Record payment received from buyer
     * DR BUYER_WALLET (buyer pays)
     * CR PLATFORM_ESCROW (platform holds)
     */
    @Transactional
    public void recordPaymentReceived(UUID orderId, UUID paymentId, BigDecimal amount, UUID buyerId) {
        UUID txnId = UUID.randomUUID();

        List<LedgerEntry> entries = new ArrayList<>();

        // Debit buyer wallet
        entries.add(createEntry(txnId, AccountType.BUYER_WALLET, buyerId, amount, Direction.DR,
                ReferenceType.PAYMENT, paymentId, "Payment for order"));

        // Credit escrow
        entries.add(createEntry(txnId, AccountType.PLATFORM_ESCROW, null, amount, Direction.CR,
                ReferenceType.PAYMENT, paymentId, "Payment received into escrow"));

        validateAndSave(entries);

        log.info("Recorded payment received: txnId={}, orderId={}, amount={}", txnId, orderId, amount);
    }

    /**
     * Release funds to seller after delivery
     * DR PLATFORM_ESCROW (release from escrow)
     * CR PLATFORM_FEE (platform commission)
     * CR SELLER_RECEIVABLE (seller's share)
     */
    @Transactional
    public void recordOrderDelivered(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("Order", orderId));

        UUID txnId = UUID.randomUUID();
        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal platformFee = order.getPlatformFee();
        BigDecimal sellerAmount = totalAmount.subtract(platformFee);

        List<LedgerEntry> entries = new ArrayList<>();

        // Debit escrow
        entries.add(createEntry(txnId, AccountType.PLATFORM_ESCROW, null, totalAmount, Direction.DR,
                ReferenceType.ORDER, orderId, "Release from escrow for delivered order"));

        // Credit platform fee
        entries.add(createEntry(txnId, AccountType.PLATFORM_FEE, null, platformFee, Direction.CR,
                ReferenceType.ORDER, orderId, "Platform commission"));

        // Credit seller receivable
        entries.add(createEntry(txnId, AccountType.SELLER_RECEIVABLE, order.getSellerId(),
                sellerAmount, Direction.CR, ReferenceType.ORDER, orderId, "Seller earnings for order"));

        validateAndSave(entries);

        log.info("Recorded order delivered: txnId={}, orderId={}, sellerAmount={}", txnId, orderId, sellerAmount);
    }

    /**
     * Record settlement to seller bank account
     * DR SELLER_RECEIVABLE (deduct from receivable)
     * CR SELLER_SETTLED (mark as settled)
     */
    @Transactional
    public void recordSettlement(UUID settlementId, UUID sellerId, BigDecimal amount) {
        UUID txnId = UUID.randomUUID();

        List<LedgerEntry> entries = new ArrayList<>();

        // Debit seller receivable
        entries.add(createEntry(txnId, AccountType.SELLER_RECEIVABLE, sellerId, amount, Direction.DR,
                ReferenceType.SETTLEMENT, settlementId, "Settlement payout"));

        // Credit seller settled
        entries.add(createEntry(txnId, AccountType.SELLER_SETTLED, sellerId, amount, Direction.CR,
                ReferenceType.SETTLEMENT, settlementId, "Settlement completed"));

        validateAndSave(entries);

        log.info("Recorded settlement: txnId={}, settlementId={}, amount={}", txnId, settlementId, amount);
    }

    /**
     * Record refund to buyer
     * DR PLATFORM_ESCROW or SELLER_RECEIVABLE
     * CR REFUND (buyer refund)
     */
    @Transactional
    public void recordRefund(UUID orderId, UUID paymentId, BigDecimal amount, UUID buyerId, boolean fromEscrow) {
        UUID txnId = UUID.randomUUID();

        List<LedgerEntry> entries = new ArrayList<>();

        if (fromEscrow) {
            // Debit escrow
            entries.add(createEntry(txnId, AccountType.PLATFORM_ESCROW, null, amount, Direction.DR,
                    ReferenceType.REFUND, paymentId, "Refund from escrow"));
        } else {
            // Debit platform (absorb the cost if already settled)
            entries.add(createEntry(txnId, AccountType.PLATFORM_FEE, null, amount, Direction.DR,
                    ReferenceType.REFUND, paymentId, "Refund from platform"));
        }

        // Credit refund
        entries.add(createEntry(txnId, AccountType.REFUND, buyerId, amount, Direction.CR,
                ReferenceType.REFUND, paymentId, "Refund to buyer"));

        validateAndSave(entries);

        log.info("Recorded refund: txnId={}, orderId={}, amount={}", txnId, orderId, amount);
    }

    @Transactional(readOnly = true)
    public BigDecimal getSellerReceivableBalance(UUID sellerId) {
        BigDecimal balance = ledgerEntryRepository.calculateBalance(AccountType.SELLER_RECEIVABLE, sellerId);
        return balance != null ? balance : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getPlatformEscrowBalance() {
        BigDecimal balance = ledgerEntryRepository.calculatePlatformBalance(AccountType.PLATFORM_ESCROW);
        return balance != null ? balance : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> getTransactionEntries(UUID txnId) {
        return ledgerEntryRepository.findByTxnId(txnId);
    }

    @Transactional(readOnly = true)
    public List<Object[]> checkLedgerIntegrity() {
        return ledgerEntryRepository.findUnbalancedTransactions();
    }

    private LedgerEntry createEntry(UUID txnId, AccountType accountType, UUID entityId,
                                    BigDecimal amount, Direction direction,
                                    ReferenceType referenceType, UUID referenceId,
                                    String description) {
        return LedgerEntry.builder()
                .txnId(txnId)
                .accountType(accountType)
                .entityId(entityId)
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .direction(direction)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .description(description)
                .build();
    }

    private void validateAndSave(List<LedgerEntry> entries) {
        // Validate that debits equal credits
        BigDecimal totalDebits = entries.stream()
                .filter(e -> e.getDirection() == Direction.DR)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = entries.stream()
                .filter(e -> e.getDirection() == Direction.CR)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalStateException(
                    String.format("Ledger entries do not balance: DR=%s, CR=%s", totalDebits, totalCredits)
            );
        }

        ledgerEntryRepository.saveAll(entries);
    }
}
