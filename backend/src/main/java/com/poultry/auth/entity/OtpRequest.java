package com.poultry.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "otp_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "phone_hash", nullable = false)
    private String phoneHash;

    @Column(name = "phone_encrypted", nullable = false)
    private byte[] phoneEncrypted;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpPurpose purpose;

    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 3;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "device_info", columnDefinition = "jsonb")
    private Map<String, Object> deviceInfo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum OtpPurpose {
        LOGIN,
        DELIVERY_CONFIRM,
        PASSWORD_RESET
    }

    public boolean isValid() {
        return !verified && expiresAt.isAfter(Instant.now()) && attempts < maxAttempts;
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public void markVerified() {
        this.verified = true;
        this.verifiedAt = Instant.now();
    }
}
