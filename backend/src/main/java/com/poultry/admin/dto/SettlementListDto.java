package com.poultry.admin.dto;

import com.poultry.settlement.entity.Settlement;
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
public class SettlementListDto
{
  public static SettlementListDto fromEntity(Settlement settlement)
  {
    return SettlementListDto.builder()
                            .id(settlement.getId())
                            .sellerId(settlement.getSellerId())
                            .periodStart(settlement.getPeriodStart())
                            .periodEnd(settlement.getPeriodEnd())
                            .grossAmount(settlement.getGrossAmount())
                            .platformFeeTotal(settlement.getPlatformFeeTotal())
                            .tdsAmount(settlement.getTdsAmount())
                            .netAmount(settlement.getNetAmount())
                            .orderCount(settlement.getOrderCount())
                            .status(settlement.getStatus().name())
                            .payoutMethod(settlement.getPayoutMethod())
                            .bankReference(settlement.getBankReference())
                            .failureCount(settlement.getFailureCount())
                            .failureReason(settlement.getFailureReason())
                            .scheduledFor(settlement.getScheduledFor())
                            .approvedAt(settlement.getApprovedAt())
                            .completedAt(settlement.getCompletedAt())
                            .createdAt(settlement.getCreatedAt())
                            .build();
  }
  private UUID id;
  private UUID sellerId;
  private String sellerName;
  private String sellerBusinessName;
  private LocalDate periodStart;
  private LocalDate periodEnd;
  private BigDecimal grossAmount;
  private BigDecimal platformFeeTotal;
  private BigDecimal tdsAmount;
  private BigDecimal netAmount;
  private Integer orderCount;
  private String status;
  private String payoutMethod;
  private String bankReference;
  private Integer failureCount;
  private String failureReason;
  private Instant scheduledFor;
  private Instant approvedAt;
  private Instant completedAt;
  private Instant createdAt;
}
