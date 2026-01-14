package com.poultry.product.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetPriceRequest {

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal basePrice;

    private List<BulkDiscountSlabRequest> bulkDiscountSlabs;

    // If null, effective immediately
    private Instant effectiveFrom;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkDiscountSlabRequest {
        @NotNull
        @DecimalMin(value = "1")
        private BigDecimal minQty;

        private BigDecimal maxQty;

        @NotNull
        @DecimalMin(value = "0")
        @DecimalMax(value = "100")
        private BigDecimal discountPercent;
    }
}
