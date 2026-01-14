package com.poultry.order.dto;

import com.poultry.order.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

    private UUID id;
    private String orderNumber;
    private UUID buyerId;
    private String buyerName;
    private UUID sellerId;
    private String sellerName;
    private Order.OrderType type;
    private Order.OrderStatus status;

    // Amounts
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal gstAmount;
    private BigDecimal deliveryCharge;
    private BigDecimal totalAmount;
    private BigDecimal platformFee;

    // Delivery
    private LocalDate deliveryDate;
    private String deliverySlot;
    private Map<String, Object> deliveryAddress;
    private String deliveryInstructions;

    // Items
    private List<OrderItemDto> items;

    // Metadata
    private Map<String, Object> priceSnapshot;
    private Integer version;
    private Instant expiresAt;

    // Cancellation
    private Instant cancelledAt;
    private String cancelledBy;
    private String cancellationReason;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;

    // State machine info
    private List<Order.OrderStatus> allowedNextStates;
    private boolean canCancel;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDto {
        private UUID id;
        private UUID productId;
        private String productName;
        private String productSku;
        private Map<String, Object> productSnapshot;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal discountPercent;
        private BigDecimal discountAmount;
        private BigDecimal gstPercent;
        private BigDecimal gstAmount;
        private BigDecimal lineTotal;
        private BigDecimal deliveredQuantity;
        private String status;
    }
}
