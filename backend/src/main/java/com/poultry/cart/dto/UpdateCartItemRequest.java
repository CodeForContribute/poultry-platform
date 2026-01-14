package com.poultry.cart.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartItemRequest
{

  @NotNull(message = "Quantity is required")
  @DecimalMin(value = "0.001", message = "Quantity must be greater than 0")
  private BigDecimal quantity;

  private String notes;
}
