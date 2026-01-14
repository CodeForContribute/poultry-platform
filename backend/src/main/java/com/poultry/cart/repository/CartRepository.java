package com.poultry.cart.repository;

import com.poultry.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository
    extends JpaRepository<Cart, UUID>
{

  List<Cart> findByBuyerId(UUID buyerId);

  Optional<Cart> findByBuyerIdAndSellerId(UUID buyerId, UUID sellerId);

  void deleteByBuyerIdAndSellerId(UUID buyerId, UUID sellerId);

  @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.buyerId = :buyerId")
  List<Cart> findByBuyerIdWithItems(
      @Param("buyerId")
      UUID buyerId);

  @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.buyerId = :buyerId AND c.sellerId = :sellerId")
  Optional<Cart> findByBuyerIdAndSellerIdWithItems(
      @Param("buyerId")
      UUID buyerId,
      @Param("sellerId")
      UUID sellerId);

  @Query("SELECT COUNT(c) FROM Cart c WHERE c.buyerId = :buyerId")
  int countByBuyerId(
      @Param("buyerId")
      UUID buyerId);
}
