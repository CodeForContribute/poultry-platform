package com.poultry.reconciliation.dto;

import com.poultry.reconciliation.entity.ReconciliationRun;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationRunDto
{
  public static ReconciliationRunDto fromEntity(ReconciliationRun run)
  {
    return ReconciliationRunDto.builder()
                               .id(run.getId())
                               .runDate(run.getRunDate())
                               .runType(run.getRunType().name())
                               .sourceType(run.getSourceType().name())
                               .razorpayFileUrl(run.getRazorpayFileUrl())
                               .bankFileUrl(run.getBankFileUrl())
                               .totalRecords(run.getTotalRecords())
                               .matchedCount(run.getMatchedCount())
                               .mismatchedCount(run.getMismatchedCount())
                               .pendingCount(run.getPendingCount())
                               .totalExpectedAmount(run.getTotalExpectedAmount())
                               .totalActualAmount(run.getTotalActualAmount())
                               .discrepancyAmount(run.getDiscrepancyAmount())
                               .status(run.getStatus().name())
                               .errorMessage(run.getErrorMessage())
                               .startedAt(run.getStartedAt())
                               .completedAt(run.getCompletedAt())
                               .reportUrl(run.getReportUrl())
                               .createdAt(run.getCreatedAt())
                               .build();
  }
  private UUID id;
  private LocalDate runDate;
  private String runType;
  private String sourceType;
  private String razorpayFileUrl;
  private String bankFileUrl;
  private Integer totalRecords;
  private Integer matchedCount;
  private Integer mismatchedCount;
  private Integer pendingCount;
  private BigDecimal totalExpectedAmount;
  private BigDecimal totalActualAmount;
  private BigDecimal discrepancyAmount;
  private String status;
  private String errorMessage;
  private Instant startedAt;
  private Instant completedAt;
  private String reportUrl;
  private Instant createdAt;
}
