package com.poultry.reconciliation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_runs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationRun
{

  public enum RunType
  {
    DAILY,
    WEEKLY,
    MANUAL
  }

  public enum SourceType
  {
    RAZORPAY,
    BANK
  }

  public enum RunStatus
  {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
  }

  public void addMismatch(ReconMismatch mismatch)
  {
    mismatches.add(mismatch);
    mismatch.setReconciliationRun(this);
  }

  public void removeMismatch(ReconMismatch mismatch)
  {
    mismatches.remove(mismatch);
    mismatch.setReconciliationRun(null);
  }
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column(name = "run_date", nullable = false, unique = true)
  private LocalDate runDate;
  @Enumerated(EnumType.STRING)
  @Column(name = "run_type", nullable = false)
  @Builder.Default
  private RunType runType = RunType.MANUAL;
  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false)
  @Builder.Default
  private SourceType sourceType = SourceType.RAZORPAY;
  @Column(name = "razorpay_file_url", length = 500)
  private String razorpayFileUrl;
  @Column(name = "bank_file_url", length = 500)
  private String bankFileUrl;
  @Column(name = "total_transactions")
  @Builder.Default
  private Integer totalRecords = 0;
  @Column(name = "matched_count")
  @Builder.Default
  private Integer matchedCount = 0;
  @Column(name = "unmatched_count")
  @Builder.Default
  private Integer mismatchedCount = 0;
  @Column(name = "pending_count")
  @Builder.Default
  private Integer pendingCount = 0;
  @Column(name = "total_expected_amount", precision = 14, scale = 2)
  @Builder.Default
  private BigDecimal totalExpectedAmount = BigDecimal.ZERO;
  @Column(name = "total_actual_amount", precision = 14, scale = 2)
  @Builder.Default
  private BigDecimal totalActualAmount = BigDecimal.ZERO;
  @Column(name = "discrepancy_amount", precision = 14, scale = 2)
  @Builder.Default
  private BigDecimal discrepancyAmount = BigDecimal.ZERO;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private RunStatus status = RunStatus.PENDING;
  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;
  @Column(name = "started_at")
  private Instant startedAt;
  @Column(name = "completed_at")
  private Instant completedAt;
  @Column(name = "report_url", length = 500)
  private String reportUrl;
  @OneToMany(mappedBy = "reconciliationRun", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<ReconMismatch> mismatches = new ArrayList<>();
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
