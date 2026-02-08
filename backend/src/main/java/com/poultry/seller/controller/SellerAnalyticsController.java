package com.poultry.seller.controller;

import com.poultry.analytics.dto.DateRangeRequest;
import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
import com.poultry.seller.dto.SellerAnalyticsDto;
import com.poultry.seller.service.SellerAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/seller/analytics")
@RequiredArgsConstructor
@Tag(name = "Seller Analytics", description = "Seller analytics and reporting APIs")
public class SellerAnalyticsController {

    private final SellerAnalyticsService analyticsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get seller analytics", description = "Get analytics data for the seller with date range")
    public ResponseEntity<ApiResponse<SellerAnalyticsDto>> getAnalytics(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Default to last 30 days if not provided
        DateRangeRequest range;
        if (startDate != null && endDate != null) {
            range = DateRangeRequest.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();
        } else {
            range = DateRangeRequest.lastNDays(30);
        }

        SellerAnalyticsDto analytics = analyticsService.getAnalytics(principal.getSellerId(), range);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get today's analytics", description = "Get analytics data for today")
    public ResponseEntity<ApiResponse<SellerAnalyticsDto>> getTodayAnalytics(
            @AuthenticationPrincipal UserPrincipal principal) {

        DateRangeRequest range = DateRangeRequest.today();
        SellerAnalyticsDto analytics = analyticsService.getAnalytics(principal.getSellerId(), range);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @GetMapping("/this-month")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get this month's analytics", description = "Get analytics data for the current month")
    public ResponseEntity<ApiResponse<SellerAnalyticsDto>> getThisMonthAnalytics(
            @AuthenticationPrincipal UserPrincipal principal) {

        DateRangeRequest range = DateRangeRequest.thisMonth();
        SellerAnalyticsDto analytics = analyticsService.getAnalytics(principal.getSellerId(), range);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @GetMapping("/last-30-days")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get last 30 days analytics", description = "Get analytics data for the last 30 days")
    public ResponseEntity<ApiResponse<SellerAnalyticsDto>> getLast30DaysAnalytics(
            @AuthenticationPrincipal UserPrincipal principal) {

        DateRangeRequest range = DateRangeRequest.lastNDays(30);
        SellerAnalyticsDto analytics = analyticsService.getAnalytics(principal.getSellerId(), range);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
}
