package com.poultry.reconciliation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recon_mismatches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconMismatch
{

  public enum MismatchType
  {
    AMOUNT_MISMATCH,
    MISSING_IN_SOURCE,
    MISSING_IN_LEDGER,
    STATUS_MISMATCH,
    DUPLICATE,
    TIMING
  }
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recon_run_id", nullable = false)
  private ReconciliationRun reconciliationRun;
  @Column(name = "source", nullable = false, length = 50)
  private String source;
  @Column(name = "source_reference", nullable = false, length = 100)
  private String referenceId;
  @Enumerated(EnumType.STRING)
  @Column(name = "mismatch_type", nullable = false, length = 50)
  private MismatchType mismatchType;
  @Column(name = "expected_amount", precision = 12, scale = 2)
  private BigDecimal expectedAmount;
  @Column(name = "actual_amount", precision = 12, scale = 2)
  private BigDecimal actualAmount;
  @Column(name = "variance", nullable = false, precision = 12, scale = 2)
  @Builder.Default
  private BigDecimal variance = BigDecimal.ZERO;
  @Column(name = "expected_status", length = 50)
  private String expectedStatus;
  @Column(name = "actual_status", length = 50)
  private String actualStatus;
  @Column(name = "order_id")
  private UUID orderId;
  @Column(name = "payment_id")
  private UUID paymentId;
  @Column(name = "settlement_id")
  private UUID settlementId;
  @Column(name = "resolved", nullable = false)
  @Builder.Default
  private Boolean resolved = false;
  @Column(name = "resolved_at")
  private Instant resolvedAt;
  @Column(name = "resolved_by")
  private UUID resolvedBy;
  @Column(name = "resolution_type", length = 50)
  private String resolutionType;
  @Column(name = "resolution_notes", columnDefinition = "TEXT")
  private String notes;
  @Column(name = "adjustment_txn_id")
  private UUID adjustmentTxnId;
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
