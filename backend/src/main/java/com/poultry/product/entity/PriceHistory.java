package com.poultry.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "price_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bulk_discount_slabs", columnDefinition = "jsonb")
    @Builder.Default
    private List<BulkDiscountSlab> bulkDiscountSlabs = List.of();

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkDiscountSlab {
        private BigDecimal minQty;
        private BigDecimal maxQty;
        private BigDecimal discountPercent;
    }

    public boolean isCurrentlyActive() {
        Instant now = Instant.now();
        return effectiveFrom.isBefore(now) || effectiveFrom.equals(now)
                && (effectiveTo == null || effectiveTo.isAfter(now));
    }

    public boolean isFuture() {
        return effectiveFrom.isAfter(Instant.now());
    }

    public BigDecimal calculatePrice(BigDecimal quantity) {
        BigDecimal discountPercent = BigDecimal.ZERO;

        for (BulkDiscountSlab slab : bulkDiscountSlabs) {
            if (quantity.compareTo(slab.getMinQty()) >= 0
                    && (slab.getMaxQty() == null || quantity.compareTo(slab.getMaxQty()) <= 0)) {
                discountPercent = slab.getDiscountPercent();
                break;
            }
        }

        if (discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = basePrice.multiply(discountPercent).divide(new BigDecimal("100"));
            return basePrice.subtract(discount);
        }

        return basePrice;
    }
}
