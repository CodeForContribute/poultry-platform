package com.poultry.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountDto {
    private UUID id;
    private String accountHolderName;
    private String bankName;
    private String accountNumberLast4;
    private String ifscCode;
    private String accountType;
    private boolean isPrimary;
    private boolean isVerified;
    private Instant createdAt;
}
