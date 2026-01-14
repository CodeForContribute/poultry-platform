package com.poultry.reconciliation.service;

import com.poultry.common.exception.BusinessException;
import com.poultry.ledger.repository.LedgerEntryRepository;
import com.poultry.payment.entity.Payment;
import com.poultry.payment.repository.PaymentRepository;
import com.poultry.reconciliation.dto.ReconMismatchDto;
import com.poultry.reconciliation.dto.ReconciliationRunDto;
import com.poultry.reconciliation.dto.ResolveMismatchRequest;
import com.poultry.reconciliation.dto.StartReconciliationRequest;
import com.poultry.reconciliation.entity.ReconMismatch;
import com.poultry.reconciliation.entity.ReconMismatch.MismatchType;
import com.poultry.reconciliation.entity.ReconciliationRun;
import com.poultry.reconciliation.entity.ReconciliationRun.RunStatus;
import com.poultry.reconciliation.entity.ReconciliationRun.RunType;
import com.poultry.reconciliation.entity.ReconciliationRun.SourceType;
import com.poultry.reconciliation.repository.ReconMismatchRepository;
import com.poultry.reconciliation.repository.ReconciliationRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService
{

  @Transactional
  public ReconciliationRunDto startReconciliation(StartReconciliationRequest request)
  {
    log.info("Starting reconciliation for date {} with source {}", request.getRunDate(), request.getSourceType());

    // Check if reconciliation already exists for this date/source
    if (runRepository.existsByRunDateAndSourceType(request.getRunDate(), request.getSourceType()))
    {
      throw new BusinessException("Reconciliation already exists for this date and source",
                                  "RECONCILIATION_EXISTS", HttpStatus.CONFLICT);
    }

    ReconciliationRun run = ReconciliationRun.builder()
                                             .runDate(request.getRunDate())
                                             .runType(request.getRunType() != null ? request.getRunType() : RunType.MANUAL)
                                             .sourceType(request.getSourceType())
                                             .status(RunStatus.PENDING)
                                             .build();

    run = runRepository.save(run);
    log.info("Reconciliation run {} created", run.getId());

    return ReconciliationRunDto.fromEntity(run);
  }

  @Transactional
  public ReconciliationRunDto processReconciliation(UUID runId, String fileContent)
  {
    log.info("Processing reconciliation for run {}", runId);

    ReconciliationRun run = runRepository.findById(runId)
                                         .orElseThrow(() -> new BusinessException("Reconciliation run not found", "RUN_NOT_FOUND", HttpStatus.NOT_FOUND));

    if (run.getStatus() != RunStatus.PENDING)
    {
      throw new BusinessException("Reconciliation is not in pending status", "INVALID_STATUS", HttpStatus.BAD_REQUEST);
    }

    run.setStatus(RunStatus.PROCESSING);
    run.setStartedAt(Instant.now());
    runRepository.save(run);

    try
    {
      // Parse file content and get transactions
      List<ExternalTransaction> externalTransactions = parseTransactions(fileContent, run.getSourceType());
      run.setTotalRecords(externalTransactions.size());

      // Get internal payments for the same date
      List<Payment> internalPayments = paymentRepository.findByCreatedAtBetween(
          run.getRunDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
          run.getRunDate().plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
      );

      // Compare and find mismatches
      compareTransactions(run, externalTransactions, internalPayments);

      // Calculate totals
      BigDecimal totalExpected = externalTransactions.stream()
                                                     .map(ExternalTransaction::amount)
                                                     .reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal totalActual = internalPayments.stream()
                                               .filter(p -> p.getStatus() == Payment.PaymentStatus.SUCCESS)
                                               .map(Payment::getAmount)
                                               .reduce(BigDecimal.ZERO, BigDecimal::add);

      run.setTotalExpectedAmount(totalExpected);
      run.setTotalActualAmount(totalActual);
      run.setDiscrepancyAmount(totalExpected.subtract(totalActual).abs());
      run.setStatus(RunStatus.COMPLETED);
      run.setCompletedAt(Instant.now());

      run = runRepository.save(run);
      log.info("Reconciliation {} completed - {} matched, {} mismatched",
               runId, run.getMatchedCount(), run.getMismatchedCount());

    }
    catch (Exception e)
    {
      log.error("Reconciliation {} failed: {}", runId, e.getMessage());
      run.setStatus(RunStatus.FAILED);
      run.setErrorMessage(e.getMessage());
      run.setCompletedAt(Instant.now());
      runRepository.save(run);
      throw new BusinessException("Reconciliation failed: " + e.getMessage(), "RECONCILIATION_FAILED", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    return ReconciliationRunDto.fromEntity(run);
  }

  @Transactional(readOnly = true)
  public ReconciliationRunDto getReconciliationRun(UUID runId)
  {
    return runRepository.findById(runId)
                        .map(ReconciliationRunDto::fromEntity)
                        .orElseThrow(() -> new BusinessException("Reconciliation run not found", "RUN_NOT_FOUND", HttpStatus.NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public Page<ReconciliationRunDto> getRecentRuns(Pageable pageable)
  {
    return runRepository.findRecentRuns(pageable)
                        .map(ReconciliationRunDto::fromEntity);
  }

  @Transactional(readOnly = true)
  public Page<ReconMismatchDto> getMismatches(UUID runId, Pageable pageable)
  {
    return mismatchRepository.findByReconciliationRunId(runId, pageable)
                             .map(ReconMismatchDto::fromEntity);
  }

  @Transactional(readOnly = true)
  public Page<ReconMismatchDto> getUnresolvedMismatches(Pageable pageable)
  {
    return mismatchRepository.findUnresolvedMismatches(pageable)
                             .map(ReconMismatchDto::fromEntity);
  }

  @Transactional
  public ReconMismatchDto resolveMismatch(UUID mismatchId, UUID adminId, ResolveMismatchRequest request)
  {
    log.info("Resolving mismatch {} by admin {}", mismatchId, adminId);

    ReconMismatch mismatch = mismatchRepository.findById(mismatchId)
                                               .orElseThrow(() -> new BusinessException("Mismatch not found", "MISMATCH_NOT_FOUND", HttpStatus.NOT_FOUND));

    if (mismatch.getResolved())
    {
      throw new BusinessException("Mismatch is already resolved", "ALREADY_RESOLVED", HttpStatus.BAD_REQUEST);
    }

    mismatch.setResolved(true);
    mismatch.setResolvedAt(Instant.now());
    mismatch.setResolvedBy(adminId);
    mismatch.setResolutionType(request.getResolutionType());
    mismatch.setNotes(request.getNotes());

    mismatch = mismatchRepository.save(mismatch);
    log.info("Mismatch {} resolved", mismatchId);

    return ReconMismatchDto.fromEntity(mismatch);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getReconciliationStats()
  {
    Map<String, Object> stats = new HashMap<>();
    stats.put("pendingRuns", runRepository.countByStatus(RunStatus.PENDING));
    stats.put("processingRuns", runRepository.countByStatus(RunStatus.PROCESSING));
    stats.put("completedRuns", runRepository.countByStatus(RunStatus.COMPLETED));
    stats.put("failedRuns", runRepository.countByStatus(RunStatus.FAILED));
    stats.put("unresolvedMismatches", mismatchRepository.countUnresolved());

    runRepository.findLatestRun().ifPresent(run ->
                                            {
                                              stats.put("lastRunDate", run.getRunDate());
                                              stats.put("lastRunStatus", run.getStatus().name());
                                            });

    return stats;
  }

  private List<ExternalTransaction> parseTransactions(String fileContent, SourceType sourceType)
  {
    // Simple CSV parsing - in production would use a proper CSV library
    List<ExternalTransaction> transactions = new ArrayList<>();

    if (fileContent == null || fileContent.isBlank())
    {
      return transactions;
    }

    String[] lines = fileContent.split("\n");
    boolean headerSkipped = false;

    for (String line : lines)
    {
      if (!headerSkipped)
      {
        headerSkipped = true;
        continue;
      }

      String[] parts = line.split(",");
      if (parts.length >= 3)
      {
        try
        {
          transactions.add(new ExternalTransaction(
              parts[0].trim(), // reference ID
              new BigDecimal(parts[1].trim()), // amount
              parts[2].trim() // status
          ));
        }
        catch (Exception e)
        {
          log.warn("Failed to parse transaction line: {}", line);
        }
      }
    }

    return transactions;
  }

  private void compareTransactions(ReconciliationRun run, List<ExternalTransaction> external, List<Payment> internal)
  {
    Map<String, Payment> internalByReference = internal.stream()
                                                       .filter(p -> p.getGatewayOrderId() != null)
                                                       .collect(Collectors.toMap(Payment::getGatewayOrderId, p -> p, (a, b) -> a));

    Set<String> matchedReferences = new HashSet<>();
    int matched = 0;
    int mismatched = 0;

    // Check each external transaction against internal records
    for (ExternalTransaction ext : external)
    {
      Payment payment = internalByReference.get(ext.referenceId());

      if (payment == null)
      {
        // Missing in internal system
        createMismatch(run, ext.referenceId(), MismatchType.MISSING_IN_LEDGER,
                       ext.amount(), null, ext.status(), null);
        mismatched++;
      }
      else
      {
        matchedReferences.add(ext.referenceId());

        // Check amount
        if (ext.amount().compareTo(payment.getAmount()) != 0)
        {
          createMismatch(run, ext.referenceId(), MismatchType.AMOUNT_MISMATCH,
                         ext.amount(), payment.getAmount(), ext.status(), payment.getStatus().name());
          mismatched++;
        }
        else if (!statusMatches(ext.status(), payment.getStatus()))
        {
          createMismatch(run, ext.referenceId(), MismatchType.STATUS_MISMATCH,
                         ext.amount(), payment.getAmount(), ext.status(), payment.getStatus().name());
          mismatched++;
        }
        else
        {
          matched++;
        }
      }
    }

    // Check for internal payments not in external source
    for (Payment payment : internal)
    {
      if (payment.getGatewayOrderId() != null && !matchedReferences.contains(payment.getGatewayOrderId()))
      {
        createMismatch(run, payment.getGatewayOrderId(), MismatchType.MISSING_IN_SOURCE,
                       null, payment.getAmount(), null, payment.getStatus().name());
        mismatched++;
      }
    }

    run.setMatchedCount(matched);
    run.setMismatchedCount(mismatched);
  }

  private boolean statusMatches(String externalStatus, Payment.PaymentStatus internalStatus)
  {
    // Normalize status comparison
    if (externalStatus == null || internalStatus == null) return false;
    String normalized = externalStatus.toUpperCase().replace(" ", "_");
    return normalized.equals(internalStatus.name()) ||
        (normalized.equals("CAPTURED") && internalStatus == Payment.PaymentStatus.SUCCESS) ||
        (normalized.equals("AUTHORIZED") && internalStatus == Payment.PaymentStatus.PENDING);
  }

  private void createMismatch(ReconciliationRun run, String referenceId, MismatchType type,
                              BigDecimal expected, BigDecimal actual, String expectedStatus, String actualStatus)
  {
    ReconMismatch mismatch = ReconMismatch.builder()
                                          .reconciliationRun(run)
                                          .source(run.getSourceType().name())
                                          .referenceId(referenceId)
                                          .mismatchType(type)
                                          .expectedAmount(expected)
                                          .actualAmount(actual)
                                          .variance(calculateVariance(expected, actual))
                                          .expectedStatus(expectedStatus)
                                          .actualStatus(actualStatus)
                                          .resolved(false)
                                          .build();

    run.addMismatch(mismatch);
  }

  private BigDecimal calculateVariance(BigDecimal expected, BigDecimal actual)
  {
    if (expected == null) expected = BigDecimal.ZERO;
    if (actual == null) actual = BigDecimal.ZERO;
    return expected.subtract(actual).abs();
  }

  // Internal record for external transactions
  private record ExternalTransaction(String referenceId, BigDecimal amount, String status)
  {
  }
  private final ReconciliationRunRepository runRepository;
  private final ReconMismatchRepository mismatchRepository;
  private final PaymentRepository paymentRepository;
  private final LedgerEntryRepository ledgerEntryRepository;
}
