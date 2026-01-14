package com.poultry.dispute.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeStatsDto
{
  private long totalOpen;
  private long openCount;
  private long underReviewCount;
  private long escalatedCount;
  private long awaitingResponseCount;
  private long resolvedCount;
  private long closedCount;
  private long unassignedCount;
}
