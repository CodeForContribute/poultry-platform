package com.poultry.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when settlement status changes.
 * Used for asynchronous processing of settlement-related workflows.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementEvent
{

  private UUID settlementId;
  private UUID sellerId;

  /**
   * Event type: CREATED, APPROVED, PROCESSING, SUCCESS, FAILED
   */
  private String eventType;

  private BigDecimal amount;
  private Instant timestamp;
}
