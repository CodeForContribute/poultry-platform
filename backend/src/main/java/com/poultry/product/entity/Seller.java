package com.poultry.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "sellers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(unique = true)
    private String gstin;

    private String pan;

    @Column(name = "phone_encrypted", nullable = false)
    private byte[] phoneEncrypted;

    private String email;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> address;

    @Column(name = "bank_account_number_encrypted")
    private byte[] bankAccountNumberEncrypted;

    @Column(name = "bank_ifsc")
    private String bankIfsc;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_account_holder")
    private String bankAccountHolder;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_cycle", nullable = false)
    @Builder.Default
    private SettlementCycle settlementCycle = SettlementCycle.T_PLUS_2;

    @Column(name = "platform_fee_percent", nullable = false)
    @Builder.Default
    private BigDecimal platformFeePercent = new BigDecimal("2.00");

    @Column(name = "fssai_number")
    private String fssaiNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SellerStatus status = SellerStatus.PENDING_VERIFICATION;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum SellerStatus {
        ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION
    }

    public enum SettlementCycle {
        T_PLUS_1, T_PLUS_2, WEEKLY
    }
}
