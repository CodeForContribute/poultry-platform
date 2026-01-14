package com.poultry.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn; // seconds
    private Instant expiresAt;

    // User info
    private UUID userId;
    private String userType;
    private String name;
    private String email;
    private String role;

    // For sellers
    private UUID sellerId;
    private String businessName;

    // Flags
    private boolean mustChangePassword;
    private boolean newUser;
}
