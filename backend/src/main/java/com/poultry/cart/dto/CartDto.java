package com.poultry.cart.dto;

import com.poultry.cart.entity.Cart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDto
{
  public static CartDto fromEntity(Cart cart)
  {
    return CartDto.builder()
                  .id(cart.getId())
                  .buyerId(cart.getBuyerId())
                  .sellerId(cart.getSellerId())
                  .items(cart.getItems().stream()
                             .map(CartItemDto::fromEntity)
                             .collect(Collectors.toList()))
                  .itemCount(cart.getItemCount())
                  .totalAmount(cart.calculateTotal())
                  .createdAt(cart.getCreatedAt())
                  .updatedAt(cart.getUpdatedAt())
                  .build();
  }
  private UUID id;
  private UUID buyerId;
  private UUID sellerId;
  private String sellerName;
  private String sellerBusinessName;
  private List<CartItemDto> items;
  private int itemCount;
  private BigDecimal totalAmount;
  private Instant createdAt;
  private Instant updatedAt;
}
