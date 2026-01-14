package com.poultry.cart.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart
{

  public void addItem(CartItem item)
  {
    items.add(item);
    item.setCart(this);
  }

  public void removeItem(CartItem item)
  {
    items.remove(item);
    item.setCart(null);
  }

  public void clear()
  {
    items.forEach(item -> item.setCart(null));
    items.clear();
  }

  public BigDecimal calculateTotal()
  {
    return items.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public int getItemCount()
  {
    return items.size();
  }

  public BigDecimal getTotalQuantity()
  {
    return items.stream()
                .map(CartItem::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column(name = "buyer_id", nullable = false)
  private UUID buyerId;
  @Column(name = "seller_id", nullable = false)
  private UUID sellerId;
  @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<CartItem> items = new ArrayList<>();
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
