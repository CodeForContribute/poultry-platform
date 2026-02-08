package com.poultry.seller.controller;

import com.poultry.auth.dto.ChangePasswordRequest;
import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
import com.poultry.seller.dto.*;
import com.poultry.seller.service.SellerSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/seller/security")
@RequiredArgsConstructor
@Tag(name = "Seller Security", description = "Seller security and authentication management APIs")
public class SellerSecurityController {

    private final SellerSecurityService sellerSecurityService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get security settings", description = "Get the current security settings and status")
    public ResponseEntity<ApiResponse<SecuritySettingsDto>> getSecuritySettings(
            @AuthenticationPrincipal UserPrincipal principal) {

        SecuritySettingsDto settings = sellerSecurityService.getSecuritySettings(
                principal.getId(), principal.getSellerId());
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Change password", description = "Change the user's password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {

        sellerSecurityService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    // ============ Two-Factor Authentication Endpoints ============

    @GetMapping("/2fa/setup")
    @PreAuthorize("hasRole('SELLER_ADMIN')")
    @Operation(summary = "Get 2FA setup", description = "Get the QR code and secret for setting up 2FA")
    public ResponseEntity<ApiResponse<TwoFactorSetupDto>> setup2FA(
            @AuthenticationPrincipal UserPrincipal principal) {

        TwoFactorSetupDto setup = sellerSecurityService.setup2FA(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(setup));
    }

    @PostMapping("/2fa/enable")
    @PreAuthorize("hasRole('SELLER_ADMIN')")
    @Operation(summary = "Enable 2FA", description = "Enable two-factor authentication with a verification code")
    public ResponseEntity<ApiResponse<BackupCodesDto>> enable2FA(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TwoFactorVerifyRequest request) {

        sellerSecurityService.enable2FA(principal.getId(), request);

        // Generate and return backup codes
        BackupCodesDto backupCodes = sellerSecurityService.generateBackupCodes(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(backupCodes, "Two-factor authentication enabled"));
    }

    @PostMapping("/2fa/disable")
    @PreAuthorize("hasRole('SELLER_ADMIN')")
    @Operation(summary = "Disable 2FA", description = "Disable two-factor authentication")
    public ResponseEntity<ApiResponse<Void>> disable2FA(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TwoFactorVerifyRequest request) {

        sellerSecurityService.disable2FA(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Two-factor authentication disabled"));
    }

    @PostMapping("/2fa/verify")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Verify 2FA code", description = "Verify a two-factor authentication code")
    public ResponseEntity<ApiResponse<Boolean>> verify2FA(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TwoFactorVerifyRequest request) {

        boolean valid = sellerSecurityService.verify2FA(principal.getId(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success(valid, valid ? "Code verified" : "Invalid code"));
    }

    @GetMapping("/2fa/backup-codes")
    @PreAuthorize("hasRole('SELLER_ADMIN')")
    @Operation(summary = "Get backup codes", description = "Generate new backup codes for 2FA recovery")
    public ResponseEntity<ApiResponse<BackupCodesDto>> getBackupCodes(
            @AuthenticationPrincipal UserPrincipal principal) {

        BackupCodesDto codes = sellerSecurityService.generateBackupCodes(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(codes));
    }

    // ============ Session Management ============

    @PostMapping("/logout-all")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Logout all sessions", description = "Logout from all devices and sessions")
    public ResponseEntity<ApiResponse<Void>> logoutAllSessions(
            @AuthenticationPrincipal UserPrincipal principal) {

        sellerSecurityService.logoutAllSessions(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "All sessions logged out"));
    }
}
