package com.poultry.verification.dto;

import com.poultry.verification.entity.SellerVerification;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SubmitVerificationRequest {
    @NotNull
    private UUID sellerId;

    @NotNull
    private SellerVerification.VerificationType verificationType;

    private List<UUID> documentIds;
}
