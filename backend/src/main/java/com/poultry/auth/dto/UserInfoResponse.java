package com.poultry.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    private UUID userId;
    private String userType;
    private String name;
    private String email;
    private String role;
    private UUID sellerId;
    private String businessName;
    private boolean mustChangePassword;
}
