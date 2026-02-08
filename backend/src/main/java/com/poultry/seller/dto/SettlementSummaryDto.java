package com.poultry.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementSummaryDto {
    private long totalSettlements;
    private long pendingSettlements;
    private long completedSettlements;
    private long failedSettlements;
    private BigDecimal totalSettledAmount;
    private BigDecimal pendingAmount;
    private BigDecimal thisMonthEarnings;
    private BigDecimal lastMonthEarnings;
}
