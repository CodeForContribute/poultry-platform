package com.poultry.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when delivery status changes.
 * Used for asynchronous processing of delivery-related workflows.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryEvent
{

  private UUID deliveryId;
  private UUID orderId;
  private UUID agentId;

  /**
   * Event type: ASSIGNED, PICKED_UP, IN_TRANSIT, DELIVERED, FAILED
   */
  private String eventType;

  private String status;
  private Instant timestamp;
}
