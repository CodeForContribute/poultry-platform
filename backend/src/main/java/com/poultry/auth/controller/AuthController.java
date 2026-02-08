package com.poultry.auth.controller;

import com.poultry.auth.dto.UserInfoResponse;
import com.poultry.auth.security.UserPrincipal;
import com.poultry.auth.service.SellerAuthService;
import com.poultry.common.dto.ApiResponse;
import com.poultry.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Common authentication endpoints")
public class AuthController {

    private final SellerAuthService sellerAuthService;

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get current authenticated user info")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal == null) {
            throw BusinessException.unauthorized("Not authenticated");
        }

        if (principal.isSeller()) {
            UserInfoResponse userInfo = sellerAuthService.getCurrentUser(principal.getId());
            return ResponseEntity.ok(ApiResponse.success(userInfo));
        }

        // For now, only seller users are supported via this endpoint
        // Buyers typically use mobile apps with different auth flows
        throw BusinessException.forbidden("User type not supported for this endpoint");
    }
}
