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
@Table(name = "delivery_agents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(nullable = false)
    private String name;

    @Column(name = "phone_encrypted", nullable = false)
    private byte[] phoneEncrypted;

    @Column(name = "phone_hash", nullable = false)
    private String phoneHash;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "vehicle_number")
    private String vehicleNumber;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "current_location", columnDefinition = "jsonb")
    private Map<String, Object> currentLocation;

    @Column(name = "total_deliveries", nullable = false)
    @Builder.Default
    private Integer totalDeliveries = 0;

    @Column(name = "successful_deliveries", nullable = false)
    @Builder.Default
    private Integer successfulDeliveries = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AgentStatus status = AgentStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum AgentStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }

    public void incrementDeliveryCount(boolean successful) {
        this.totalDeliveries++;
        if (successful) {
            this.successfulDeliveries++;
        }
    }
}
