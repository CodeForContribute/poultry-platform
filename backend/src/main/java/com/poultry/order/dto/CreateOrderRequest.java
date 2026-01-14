package com.poultry.order.dto;

import com.poultry.order.entity.Order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 64, message = "Idempotency key cannot exceed 64 characters")
    private String idempotencyKey;

    @NotNull(message = "Seller ID is required")
    private UUID sellerId;

    @NotNull(message = "Order type is required")
    private Order.OrderType type;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<OrderItemRequest> items;

    @NotNull(message = "Delivery address is required")
    private DeliveryAddress deliveryAddress;

    // For preorders
    @Future(message = "Delivery date must be in the future")
    private LocalDate deliveryDate;

    private String deliverySlot;

    private String deliveryInstructions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        @NotNull(message = "Product ID is required")
        private UUID productId;

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.001", message = "Quantity must be greater than 0")
        private BigDecimal quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryAddress {
        private String label;

        @NotBlank(message = "Address line 1 is required")
        private String line1;

        private String line2;

        @NotBlank(message = "City is required")
        private String city;

        @NotBlank(message = "State is required")
        private String state;

        @NotBlank(message = "Pincode is required")
        @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid pincode")
        private String pincode;

        private String landmark;
        private Double latitude;
        private Double longitude;

        @NotBlank(message = "Contact name is required")
        private String contactName;

        @NotBlank(message = "Contact phone is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid phone number")
        private String contactPhone;

        public Map<String, Object> toMap() {
            return Map.of(
                    "label", label != null ? label : "",
                    "line1", line1,
                    "line2", line2 != null ? line2 : "",
                    "city", city,
                    "state", state,
                    "pincode", pincode,
                    "landmark", landmark != null ? landmark : "",
                    "latitude", latitude != null ? latitude : 0,
                    "longitude", longitude != null ? longitude : 0,
                    "contactName", contactName,
                    "contactPhone", contactPhone
            );
        }
    }
}
