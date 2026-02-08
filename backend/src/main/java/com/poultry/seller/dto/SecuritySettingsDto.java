package com.poultry.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecuritySettingsDto {
    private boolean twoFactorEnabled;
    private Instant lastPasswordChange;
    private Instant lastLoginAt;
    private boolean mustChangePassword;
    private int activeSessions;
}
