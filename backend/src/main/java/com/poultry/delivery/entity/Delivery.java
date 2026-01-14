package com.poultry.delivery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "deliveries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "agent_id")
    private UUID agentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING_ASSIGNMENT;

    @Column(name = "delivery_otp")
    private String deliveryOtp;

    @Column(name = "otp_generated_at")
    private Instant otpGeneratedAt;

    @Column(name = "otp_verified_at")
    private Instant otpVerifiedAt;

    @Column(name = "photo_proof_url")
    private String photoProofUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "current_location", columnDefinition = "jsonb")
    private Map<String, Object> currentLocation;

    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 2;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum DeliveryStatus {
        PENDING_ASSIGNMENT,
        ASSIGNED,
        DISPATCHED,
        IN_TRANSIT,
        ARRIVED,
        DELIVERED,
        FAILED,
        RETURNED
    }

    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    public boolean isOtpValid() {
        if (deliveryOtp == null || otpGeneratedAt == null) return false;
        return Instant.now().isBefore(otpGeneratedAt.plusSeconds(600)); // 10 minutes validity
    }
}
