package com.poultry.order.dto;

import com.poultry.order.entity.Order;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {

    @NotNull(message = "Target status is required")
    private Order.OrderStatus status;

    private String reason;

    // For optimistic locking
    private Integer expectedVersion;
}
