package com.poultry.delivery.dto;

import com.poultry.delivery.entity.Delivery;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class DeliveryDto {
    private UUID id;
    private UUID orderId;
    private UUID agentId;
    private String agentName;
    private String agentPhone;
    private Delivery.DeliveryStatus status;
    private Map<String, Object> currentLocation;
    private Instant slaDeadline;
    private Instant dispatchedAt;
    private Instant deliveredAt;
    private Integer retryCount;
    private Instant createdAt;
}
