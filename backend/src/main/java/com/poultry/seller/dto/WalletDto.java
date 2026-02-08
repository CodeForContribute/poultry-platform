package com.poultry.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDto {
    private BigDecimal availableBalance;
    private BigDecimal pendingBalance;
    private BigDecimal totalEarnings;
    private BigDecimal totalWithdrawn;
    private Instant lastUpdated;
}
