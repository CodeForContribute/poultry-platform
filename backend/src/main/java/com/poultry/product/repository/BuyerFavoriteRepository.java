package com.poultry.product.repository;

import com.poultry.product.entity.BuyerFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BuyerFavoriteRepository extends JpaRepository<BuyerFavorite, UUID> {

    List<BuyerFavorite> findByBuyerId(UUID buyerId);

    Optional<BuyerFavorite> findByBuyerIdAndProductId(UUID buyerId, UUID productId);

    Optional<BuyerFavorite> findByBuyerIdAndSellerId(UUID buyerId, UUID sellerId);

    boolean existsByBuyerIdAndProductId(UUID buyerId, UUID productId);

    boolean existsByBuyerIdAndSellerId(UUID buyerId, UUID sellerId);

    void deleteByBuyerIdAndProductId(UUID buyerId, UUID productId);

    void deleteByBuyerIdAndSellerId(UUID buyerId, UUID sellerId);

    @Query("SELECT bf.buyerId FROM BuyerFavorite bf WHERE bf.productId = :productId AND bf.notifyOnPriceChange = true")
    List<UUID> findBuyersToNotifyForProduct(UUID productId);

    @Query("SELECT bf.buyerId FROM BuyerFavorite bf WHERE bf.sellerId = :sellerId AND bf.notifyOnPriceChange = true")
    List<UUID> findBuyersToNotifyForSeller(UUID sellerId);
}
