package com.poultry.cart.repository;

import com.poultry.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository
    extends JpaRepository<CartItem, UUID>
{

  List<CartItem> findByCartId(UUID cartId);

  Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);

  void deleteByCartIdAndProductId(UUID cartId, UUID productId);

  int countByCartId(UUID cartId);
}
