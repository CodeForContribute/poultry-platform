package com.poultry.seller.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seller_notification_preferences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerNotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "seller_id", nullable = false, unique = true)
    private UUID sellerId;

    // Email preferences
    @Column(name = "email_new_orders", nullable = false)
    @Builder.Default
    private boolean emailNewOrders = true;

    @Column(name = "email_order_updates", nullable = false)
    @Builder.Default
    private boolean emailOrderUpdates = true;

    @Column(name = "email_payment_received", nullable = false)
    @Builder.Default
    private boolean emailPaymentReceived = true;

    @Column(name = "email_settlement_completed", nullable = false)
    @Builder.Default
    private boolean emailSettlementCompleted = true;

    @Column(name = "email_dispute_raised", nullable = false)
    @Builder.Default
    private boolean emailDisputeRaised = true;

    @Column(name = "email_promotions", nullable = false)
    @Builder.Default
    private boolean emailPromotions = false;

    // SMS preferences
    @Column(name = "sms_new_orders", nullable = false)
    @Builder.Default
    private boolean smsNewOrders = true;

    @Column(name = "sms_order_updates", nullable = false)
    @Builder.Default
    private boolean smsOrderUpdates = false;

    @Column(name = "sms_payment_received", nullable = false)
    @Builder.Default
    private boolean smsPaymentReceived = true;

    @Column(name = "sms_settlement_completed", nullable = false)
    @Builder.Default
    private boolean smsSettlementCompleted = true;

    @Column(name = "sms_dispute_raised", nullable = false)
    @Builder.Default
    private boolean smsDisputeRaised = true;

    // Push notification preferences
    @Column(name = "push_new_orders", nullable = false)
    @Builder.Default
    private boolean pushNewOrders = true;

    @Column(name = "push_order_updates", nullable = false)
    @Builder.Default
    private boolean pushOrderUpdates = true;

    @Column(name = "push_payment_received", nullable = false)
    @Builder.Default
    private boolean pushPaymentReceived = true;

    @Column(name = "push_settlement_completed", nullable = false)
    @Builder.Default
    private boolean pushSettlementCompleted = true;

    @Column(name = "push_dispute_raised", nullable = false)
    @Builder.Default
    private boolean pushDisputeRaised = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
