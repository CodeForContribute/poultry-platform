package com.poultry.dispute.dto;

import com.poultry.dispute.entity.Dispute.DisputeResolution;
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
public class ResolveDisputeRequest
{

  @NotNull(message = "Resolution is required")
  private DisputeResolution resolution;

  private BigDecimal resolutionAmount;

  private String notes;
}
