package com.poultry.product.dto;

import com.poultry.product.entity.PriceHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceDto {

    private UUID id;
    private UUID productId;
    private BigDecimal basePrice;
    private List<PriceHistory.BulkDiscountSlab> bulkDiscountSlabs;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private boolean isCurrentlyActive;
    private boolean isScheduled;
    private Instant createdAt;
}
