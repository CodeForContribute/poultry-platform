package com.poultry.reconciliation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveMismatchRequest
{

  @NotBlank(message = "Resolution type is required")
  private String resolutionType; // ADJUSTED, ACCEPTED, WRITTEN_OFF, PENDING_INVESTIGATION

  private String notes;
}
