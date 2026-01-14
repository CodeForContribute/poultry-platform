package com.poultry.reconciliation.dto;

import com.poultry.reconciliation.entity.ReconciliationRun.RunType;
import com.poultry.reconciliation.entity.ReconciliationRun.SourceType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartReconciliationRequest
{

  @NotNull(message = "Run date is required")
  private LocalDate runDate;

  @NotNull(message = "Source type is required")
  private SourceType sourceType;

  private RunType runType;
}
