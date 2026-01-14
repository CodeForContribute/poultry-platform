package com.poultry.verification.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class VerificationActionRequest {
    @NotNull
    private UUID verificationId;

    @NotNull
    private UUID adminId;

    private String remarks;

    private String rejectionReason;
}
