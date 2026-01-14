package com.poultry.auth.controller;

import com.poultry.auth.dto.*;
import com.poultry.auth.security.UserPrincipal;
import com.poultry.auth.service.BuyerAuthService;
import com.poultry.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth/buyer")
@RequiredArgsConstructor
@Tag(name = "Buyer Authentication", description = "OTP-based authentication for buyers")
public class BuyerAuthController {

    private final BuyerAuthService buyerAuthService;

    @PostMapping("/otp/request")
    @Operation(summary = "Request OTP", description = "Send OTP to buyer's phone number")
    public ResponseEntity<ApiResponse<OtpSentResponse>> requestOtp(
            @Valid @RequestBody BuyerOtpRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        OtpSentResponse response = buyerAuthService.requestOtp(request, ipAddress);
        return ResponseEntity.ok(ApiResponse.success(response, "OTP sent successfully"));
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify OTP", description = "Verify OTP and login/register buyer")
    public ResponseEntity<ApiResponse<TokenResponse>> verifyOtp(
            @Valid @RequestBody BuyerOtpVerifyRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        TokenResponse response = buyerAuthService.verifyOtpAndLogin(request, ipAddress);
        return ResponseEntity.ok(ApiResponse.success(response,
                response.isNewUser() ? "Registration successful" : "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Get new access token using refresh token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        TokenResponse response = buyerAuthService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Logout from current device")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {

        buyerAuthService.logout(principal.getId(), refreshToken);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout all devices", description = "Logout from all devices")
    public ResponseEntity<ApiResponse<Void>> logoutAllDevices(
            @AuthenticationPrincipal UserPrincipal principal) {

        buyerAuthService.logoutAllDevices(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out from all devices"));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
