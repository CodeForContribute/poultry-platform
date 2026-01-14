package com.poultry.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSentResponse {

    private String maskedPhone;
    private int expiresInSeconds;
    private int attemptsRemaining;
    private boolean newUser;
}
