package com.poultry.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when payment status changes.
 * Used for asynchronous processing of payment-related workflows.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent
{

  private UUID paymentId;
  private UUID orderId;
  private UUID buyerId;

  /**
   * Event type: CREATED, SUCCESS, FAILED, REFUNDED
   */
  private String eventType;

  private String status;
  private BigDecimal amount;
  private String gatewayReference;
  private Instant timestamp;
}
