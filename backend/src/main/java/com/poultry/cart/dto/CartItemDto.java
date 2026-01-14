package com.poultry.cart.dto;

import com.poultry.cart.entity.CartItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto
{
  public static CartItemDto fromEntity(CartItem item)
  {
    return CartItemDto.builder()
                      .id(item.getId())
                      .productId(item.getProductId())
                      .quantity(item.getQuantity())
                      .unitPrice(item.getUnitPrice())
                      .lineTotal(item.getLineTotal())
                      .notes(item.getNotes())
                      .createdAt(item.getCreatedAt())
                      .build();
  }
  private UUID id;
  private UUID productId;
  private String productName;
  private String productSku;
  private String productUnit;
  private BigDecimal quantity;
  private BigDecimal unitPrice;
  private BigDecimal lineTotal;
  private String notes;
  private Instant createdAt;
}
