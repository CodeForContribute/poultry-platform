package com.poultry.seller.dto;

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
public class SettlementDto {
    private UUID id;
    private String settlementNumber;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private int orderCount;
    private BigDecimal grossAmount;
    private BigDecimal platformFee;
    private BigDecimal tdsAmount;
    private BigDecimal otherDeductions;
    private BigDecimal netAmount;
    private Settlement.SettlementStatus status;
    private String payoutMethod;
    private String payoutReference;
    private String bankReference;
    private Instant scheduledFor;
    private Instant completedAt;
    private Instant createdAt;

    // Bank account info (masked)
    private String bankAccountLast4;
    private String bankName;

    public static SettlementDto fromEntity(Settlement settlement) {
        return SettlementDto.builder()
                .id(settlement.getId())
                .periodStart(settlement.getPeriodStart())
                .periodEnd(settlement.getPeriodEnd())
                .orderCount(settlement.getOrderCount())
                .grossAmount(settlement.getGrossAmount())
                .platformFee(settlement.getPlatformFeeTotal())
                .tdsAmount(settlement.getTdsAmount())
                .otherDeductions(settlement.getOtherDeductions())
                .netAmount(settlement.getNetAmount())
                .status(settlement.getStatus())
                .payoutMethod(settlement.getPayoutMethod())
                .payoutReference(settlement.getPayoutReference())
                .bankReference(settlement.getBankReference())
                .scheduledFor(settlement.getScheduledFor())
                .completedAt(settlement.getCompletedAt())
                .createdAt(settlement.getCreatedAt())
                .build();
    }
}
