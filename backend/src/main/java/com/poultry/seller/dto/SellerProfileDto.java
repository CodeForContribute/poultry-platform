package com.poultry.seller.dto;

import com.poultry.product.entity.Seller;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerProfileDto {
    private UUID id;
    private String businessName;
    private String gstin;
    private String pan;
    private String phone;
    private String email;
    private Map<String, Object> address;
    private String fssaiNumber;
    private Seller.SellerStatus status;
    private Seller.SettlementCycle settlementCycle;
    private BigDecimal platformFeePercent;
    private Instant createdAt;
    private Instant updatedAt;

    // Bank account info (masked)
    private String bankAccountLast4;
    private String bankIfsc;
    private String bankName;
    private String bankAccountHolder;
    private boolean hasBankAccount;
}
