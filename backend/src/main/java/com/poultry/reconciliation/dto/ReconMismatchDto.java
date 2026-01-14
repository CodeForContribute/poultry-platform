package com.poultry.reconciliation.dto;

import com.poultry.reconciliation.entity.ReconMismatch;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconMismatchDto
{
  public static ReconMismatchDto fromEntity(ReconMismatch mismatch)
  {
    return ReconMismatchDto.builder()
                           .id(mismatch.getId())
                           .reconciliationRunId(mismatch.getReconciliationRun().getId())
                           .source(mismatch.getSource())
                           .referenceId(mismatch.getReferenceId())
                           .mismatchType(mismatch.getMismatchType().name())
                           .expectedAmount(mismatch.getExpectedAmount())
                           .actualAmount(mismatch.getActualAmount())
                           .variance(mismatch.getVariance())
                           .expectedStatus(mismatch.getExpectedStatus())
                           .actualStatus(mismatch.getActualStatus())
                           .orderId(mismatch.getOrderId())
                           .paymentId(mismatch.getPaymentId())
                           .settlementId(mismatch.getSettlementId())
                           .resolved(mismatch.getResolved())
                           .resolvedAt(mismatch.getResolvedAt())
                           .resolvedBy(mismatch.getResolvedBy())
                           .resolutionType(mismatch.getResolutionType())
                           .notes(mismatch.getNotes())
                           .createdAt(mismatch.getCreatedAt())
                           .build();
  }
  private UUID id;
  private UUID reconciliationRunId;
  private String source;
  private String referenceId;
  private String mismatchType;
  private BigDecimal expectedAmount;
  private BigDecimal actualAmount;
  private BigDecimal variance;
  private String expectedStatus;
  private String actualStatus;
  private UUID orderId;
  private UUID paymentId;
  private UUID settlementId;
  private Boolean resolved;
  private Instant resolvedAt;
  private UUID resolvedBy;
  private String resolutionType;
  private String notes;
  private Instant createdAt;
}
