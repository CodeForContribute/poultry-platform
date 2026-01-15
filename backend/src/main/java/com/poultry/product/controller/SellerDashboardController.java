package com.poultry.product.controller;

import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
import com.poultry.product.dto.SellerDashboardDto;
import com.poultry.product.service.SellerDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/seller/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
@Tag(name = "Seller Dashboard", description = "Seller dashboard and overview APIs")
public class SellerDashboardController {

    private final SellerDashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get seller dashboard", description = "Get seller dashboard with overview metrics")
    public ResponseEntity<ApiResponse<SellerDashboardDto>> getDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {

        SellerDashboardDto dashboard = dashboardService.getDashboard(principal.getSellerId());
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }
}
