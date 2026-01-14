package com.poultry.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when order status changes.
 * Used for asynchronous processing of order-related workflows.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent
{

  private UUID orderId;
  private UUID buyerId;
  private UUID sellerId;
  private String orderNumber;

  /**
   * Event type: CREATED, CONFIRMED, PAID, DISPATCHED, DELIVERED, CANCELLED
   */
  private String eventType;

  private String previousStatus;
  private String newStatus;
  private BigDecimal amount;
  private Instant timestamp;
}
