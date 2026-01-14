package com.poultry.delivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignAgentRequest {
    @NotNull
    private UUID orderId;

    @NotNull
    private UUID agentId;
}
