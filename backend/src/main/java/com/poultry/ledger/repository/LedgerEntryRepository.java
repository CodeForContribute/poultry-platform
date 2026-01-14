package com.poultry.ledger.repository;

import com.poultry.ledger.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByTxnId(UUID txnId);

    List<LedgerEntry> findByReferenceTypeAndReferenceId(
            LedgerEntry.ReferenceType referenceType, UUID referenceId);

    @Query("SELECT SUM(CASE WHEN e.direction = 'CR' THEN e.amount ELSE -e.amount END) " +
            "FROM LedgerEntry e WHERE e.accountType = :accountType AND e.entityId = :entityId")
    BigDecimal calculateBalance(LedgerEntry.AccountType accountType, UUID entityId);

    @Query("SELECT SUM(CASE WHEN e.direction = 'CR' THEN e.amount ELSE -e.amount END) " +
            "FROM LedgerEntry e WHERE e.accountType = :accountType AND e.entityId IS NULL")
    BigDecimal calculatePlatformBalance(LedgerEntry.AccountType accountType);

    @Query("SELECT e FROM LedgerEntry e WHERE e.accountType = :accountType " +
            "AND e.entityId = :entityId ORDER BY e.createdAt DESC")
    List<LedgerEntry> findByAccountAndEntity(LedgerEntry.AccountType accountType, UUID entityId);

    @Query("SELECT e FROM LedgerEntry e WHERE e.createdAt BETWEEN :start AND :end " +
            "ORDER BY e.createdAt")
    List<LedgerEntry> findByDateRange(Instant start, Instant end);

    @Query("SELECT e.txnId, SUM(CASE WHEN e.direction = 'DR' THEN e.amount ELSE 0 END) as debits, " +
            "SUM(CASE WHEN e.direction = 'CR' THEN e.amount ELSE 0 END) as credits " +
            "FROM LedgerEntry e GROUP BY e.txnId " +
            "HAVING SUM(CASE WHEN e.direction = 'DR' THEN e.amount ELSE -e.amount END) != 0")
    List<Object[]> findUnbalancedTransactions();
}
