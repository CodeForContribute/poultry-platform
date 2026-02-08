package com.poultry.seller.controller;

import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
import com.poultry.seller.dto.*;
import com.poultry.seller.service.SellerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/seller")
@RequiredArgsConstructor
@Tag(name = "Seller Profile", description = "Seller profile and settings management APIs")
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;

    // ============ Profile Endpoints ============

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get seller profile", description = "Get the authenticated seller's business profile")
    public ResponseEntity<ApiResponse<SellerProfileDto>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal) {

        SellerProfileDto profile = sellerProfileService.getProfile(principal.getSellerId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('SELLER_ADMIN')")
    @Operation(summary = "Update seller profile", description = "Update the seller's business profile (admin only)")
    public ResponseEntity<ApiResponse<SellerProfileDto>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateSellerProfileRequest request) {

        SellerProfileDto profile = sellerProfileService.updateProfile(principal.getSellerId(), request);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile updated successfully"));
    }

    // ============ Bank Account Endpoints ============

    @GetMapping("/bank-accounts")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get bank accounts", description = "Get all bank accounts for the seller")
    public ResponseEntity<ApiResponse<List<BankAccountDto>>> getBankAccounts(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<BankAccountDto> accounts = sellerProfileService.getBankAccounts(principal.getSellerId());
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @PostMapping("/bank-accounts")
    @PreAuthorize("hasRole('SELLER_ADMIN')")
    @Operation(summary = "Add bank account", description = "Add a new bank account (admin only)")
    public ResponseEntity<ApiResponse<BankAccountDto>> addBankAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddBankAccountRequest request) {

        BankAccountDto account = sellerProfileService.addBankAccount(principal.getSellerId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(account, "Bank account added successfully"));
    }

    @DeleteMapping("/bank-accounts/{bankAccountId}")
    @PreAuthorize("hasRole('SELLER_ADMIN')")
    @Operation(summary = "Delete bank account", description = "Delete a bank account (admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteBankAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID bankAccountId) {

        sellerProfileService.deleteBankAccount(principal.getSellerId(), bankAccountId);
        return ResponseEntity.ok(ApiResponse.success(null, "Bank account deleted successfully"));
    }

    @PostMapping("/bank-accounts/{bankAccountId}/set-primary")
    @PreAuthorize("hasRole('SELLER_ADMIN')")
    @Operation(summary = "Set primary bank account", description = "Set a bank account as the primary account (admin only)")
    public ResponseEntity<ApiResponse<BankAccountDto>> setPrimaryBankAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID bankAccountId) {

        BankAccountDto account = sellerProfileService.setPrimaryBankAccount(principal.getSellerId(), bankAccountId);
        return ResponseEntity.ok(ApiResponse.success(account, "Primary bank account updated"));
    }

    // ============ Notification Preferences Endpoints ============

    @GetMapping("/preferences/notifications")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get notification preferences", description = "Get notification preferences for the seller")
    public ResponseEntity<ApiResponse<NotificationPreferencesDto>> getNotificationPreferences(
            @AuthenticationPrincipal UserPrincipal principal) {

        NotificationPreferencesDto preferences = sellerProfileService.getNotificationPreferences(principal.getSellerId());
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    @PutMapping("/preferences/notifications")
    @PreAuthorize("hasRole('SELLER_ADMIN')")
    @Operation(summary = "Update notification preferences", description = "Update notification preferences (admin only)")
    public ResponseEntity<ApiResponse<NotificationPreferencesDto>> updateNotificationPreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NotificationPreferencesDto request) {

        NotificationPreferencesDto preferences = sellerProfileService.updateNotificationPreferences(
                principal.getSellerId(), request);
        return ResponseEntity.ok(ApiResponse.success(preferences, "Notification preferences updated"));
    }
}
