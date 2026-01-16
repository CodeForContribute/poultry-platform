package com.poultry.product.repository;

import com.poultry.product.entity.PriceHistory;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

    @Query("SELECT ph FROM PriceHistory ph WHERE ph.productId = :productId " +
            "AND ph.effectiveFrom <= :now AND (ph.effectiveTo IS NULL OR ph.effectiveTo > :now) " +
            "ORDER BY ph.effectiveFrom DESC LIMIT 1")
    Optional<PriceHistory> findCurrentPrice(UUID productId, Instant now);

    @Query("SELECT ph FROM PriceHistory ph WHERE ph.productId = :productId " +
            "AND ph.effectiveFrom > :effectiveFrom ORDER BY ph.effectiveFrom ASC LIMIT 1")
    Optional<PriceHistory> findNextPrice(UUID productId, Instant effectiveFrom);

    boolean existsByProductIdAndEffectiveFrom(UUID productId, Instant effectiveFrom);

    @Modifying
    @Query("UPDATE PriceHistory ph SET ph.effectiveTo = :effectiveTo " +
            "WHERE ph.productId = :productId " +
            "AND ph.effectiveFrom < :effectiveTo " +
            "AND (ph.effectiveTo IS NULL OR ph.effectiveTo > :effectiveTo)")
    int closeOverlappingPriceAt(UUID productId, Instant effectiveTo);

    @Query("SELECT ph FROM PriceHistory ph WHERE ph.productId = :productId " +
            "ORDER BY ph.effectiveFrom DESC")
    List<PriceHistory> findByProductIdOrderByEffectiveFromDesc(UUID productId);

    @Query("SELECT ph FROM PriceHistory ph WHERE ph.productId = :productId " +
            "AND ph.effectiveFrom > :now ORDER BY ph.effectiveFrom ASC")
    List<PriceHistory> findScheduledPrices(UUID productId, Instant now);

    @Query("SELECT ph FROM PriceHistory ph WHERE ph.productId IN :productIds " +
            "AND ph.effectiveFrom <= :now AND (ph.effectiveTo IS NULL OR ph.effectiveTo > :now)")
    List<PriceHistory> findCurrentPricesForProducts(List<UUID> productIds, Instant now);

    @Query("SELECT DISTINCT ph.productId FROM PriceHistory ph WHERE ph.effectiveFrom > :start AND ph.effectiveFrom <= :end")
    List<UUID> findProductsWithPriceChangesInWindow(Instant start, Instant end);
}
