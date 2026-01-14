package com.poultry.settlement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "settlements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement
{

  public enum SettlementStatus
  {
    PENDING,
    APPROVED,
    PROCESSING,
    SUCCESS,
    FAILED,
    CANCELLED
  }

  public void addItem(SettlementItem item)
  {
    items.add(item);
    item.setSettlement(this);
  }

  public void removeItem(SettlementItem item)
  {
    items.remove(item);
    item.setSettlement(null);
  }

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column(name = "seller_id", nullable = false)
  private UUID sellerId;
  @Column(name = "period_start", nullable = false)
  private LocalDate periodStart;
  @Column(name = "period_end", nullable = false)
  private LocalDate periodEnd;
  @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal grossAmount;
  @Column(name = "platform_fee_total", nullable = false, precision = 12, scale = 2)
  private BigDecimal platformFeeTotal;
  @Column(name = "tds_amount", precision = 12, scale = 2)
  @Builder.Default
  private BigDecimal tdsAmount = BigDecimal.ZERO;
  @Column(name = "other_deductions", precision = 12, scale = 2)
  @Builder.Default
  private BigDecimal otherDeductions = BigDecimal.ZERO;
  @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal netAmount;
  @Column(name = "order_count", nullable = false)
  private Integer orderCount;
  @Column(name = "payout_method", length = 50)
  private String payoutMethod;
  @Column(name = "payout_reference", length = 100)
  private String payoutReference;
  @Column(name = "bank_reference", length = 100)
  private String bankReference;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private SettlementStatus status = SettlementStatus.PENDING;
  @Column(name = "failure_count")
  @Builder.Default
  private Integer failureCount = 0;
  @Column(name = "failure_reason", columnDefinition = "TEXT")
  private String failureReason;
  @Column(name = "last_failure_at")
  private Instant lastFailureAt;
  @Column(name = "scheduled_for")
  private Instant scheduledFor;
  @Column(name = "initiated_at")
  private Instant initiatedAt;
  @Column(name = "completed_at")
  private Instant completedAt;
  // Approval fields for admin workflow
  @Column(name = "approved_by")
  private UUID approvedBy;
  @Column(name = "approved_at")
  private Instant approvedAt;
  @Column(name = "approval_remarks", columnDefinition = "TEXT")
  private String approvalRemarks;
  @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<SettlementItem> items = new ArrayList<>();
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
