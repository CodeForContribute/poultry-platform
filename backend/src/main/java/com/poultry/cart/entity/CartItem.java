package com.poultry.cart.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem
{

  public BigDecimal getLineTotal()
  {
    if (unitPrice == null || quantity == null)
    {
      return BigDecimal.ZERO;
    }
    return unitPrice.multiply(quantity);
  }
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cart_id", nullable = false)
  private Cart cart;
  @Column(name = "product_id", nullable = false)
  private UUID productId;
  @Column(nullable = false)
  private BigDecimal quantity;
  @Column(name = "unit_price")
  private BigDecimal unitPrice;
  private String notes;
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
