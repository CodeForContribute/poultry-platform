package com.poultry.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", unique = true)
    private String orderNumber;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderType type = OrderType.INSTANT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.DRAFT;

    // Amounts
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "gst_amount", nullable = false)
    @Builder.Default
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(name = "delivery_charge", nullable = false)
    @Builder.Default
    private BigDecimal deliveryCharge = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "platform_fee", nullable = false)
    @Builder.Default
    private BigDecimal platformFee = BigDecimal.ZERO;

    // Delivery info
    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "delivery_slot")
    private String deliverySlot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "delivery_address", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> deliveryAddress;

    @Column(name = "delivery_instructions")
    private String deliveryInstructions;

    // Price snapshot (immutable)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "price_snapshot", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> priceSnapshot = Map.of();

    // Optimistic locking
    @Version
    @Column(nullable = false)
    private Integer version;

    // Expiry
    @Column(name = "expires_at")
    private Instant expiresAt;

    // Cancellation
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    // Order items
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum OrderType {
        INSTANT, PREORDER
    }

    public enum OrderStatus {
        DRAFT,
        PLACED,
        SELLER_CONFIRMED,
        SELLER_REJECTED,
        PAYMENT_PENDING,
        PAYMENT_FAILED,
        PAID,
        DISPATCHED,
        DELIVERED,
        SETTLED,
        CANCELLED_BY_BUYER,
        REFUND_INITIATED,
        REFUNDED
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    public boolean isTerminal() {
        return status == OrderStatus.SELLER_REJECTED
                || status == OrderStatus.SETTLED
                || status == OrderStatus.REFUNDED;
    }

    public boolean canBeCancelled() {
        return status == OrderStatus.PAID;
    }

    public void calculateTotals() {
        this.subtotal = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.gstAmount = items.stream()
                .map(OrderItem::getGstAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalAmount = subtotal
                .subtract(discountAmount)
                .add(gstAmount)
                .add(deliveryCharge);
    }
}
