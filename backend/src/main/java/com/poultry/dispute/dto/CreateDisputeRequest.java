package com.poultry.dispute.dto;

import com.poultry.dispute.entity.Dispute.DisputeResolution;
import com.poultry.dispute.entity.Dispute.DisputeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDisputeRequest
{

  @NotNull(message = "Order ID is required")
  private UUID orderId;

  @NotNull(message = "Dispute type is required")
  private DisputeType type;

  @NotBlank(message = "Title is required")
  @Size(max = 200, message = "Title must be at most 200 characters")
  private String title;

  @NotBlank(message = "Description is required")
  private String description;

  private List<String> evidenceUrls;

  private DisputeResolution requestedResolution;

  private BigDecimal requestedAmount;
}
