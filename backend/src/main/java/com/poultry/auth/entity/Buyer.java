package com.poultry.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "buyers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Buyer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "phone_encrypted", nullable = false)
    private byte[] phoneEncrypted;

    @Column(name = "phone_hash", nullable = false, unique = true)
    private String phoneHash;

    private String name;

    @Column(name = "business_name")
    private String businessName;

    private String gstin;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<Address> addresses = List.of();

    @Column(name = "default_address_index")
    @Builder.Default
    private Integer defaultAddressIndex = 0;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "preferred_language")
    @Builder.Default
    private String preferredLanguage = "en";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BuyerStatus status = BuyerStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum BuyerStatus {
        ACTIVE,
        INACTIVE,
        BLOCKED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String label; // "Home", "Farm", "Warehouse"
        private String line1;
        private String line2;
        private String city;
        private String state;
        private String pincode;
        private String landmark;
        private Double latitude;
        private Double longitude;
        private String contactName;
        private String contactPhone;
    }
}
