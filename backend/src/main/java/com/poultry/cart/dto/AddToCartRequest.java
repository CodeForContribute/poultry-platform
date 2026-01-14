package com.poultry.cart.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest
{

  @NotNull(message = "Product ID is required")
  private UUID productId;

  @NotNull(message = "Quantity is required")
  @DecimalMin(value = "0.001", message = "Quantity must be greater than 0")
  private BigDecimal quantity;

  private String notes;
}
