package com.poultry.delivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateLocationRequest {
    @NotNull
    private UUID deliveryId;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;
}
