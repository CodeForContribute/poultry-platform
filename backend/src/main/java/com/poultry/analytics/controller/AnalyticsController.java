package com.poultry.analytics.controller;

import com.poultry.analytics.dto.*;
import com.poultry.analytics.service.AnalyticsService;
import com.poultry.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Analytics and Reporting APIs")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard stats", description = "Get overall platform statistics")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats(
            @Parameter(description = "Start date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        DateRangeRequest range = DateRangeRequest.builder()
                .startDate(startDate).endDate(endDate).build();
        DashboardStatsDto stats = analyticsService.getDashboardStats(range);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/dashboard/today")
    @Operation(summary = "Get today's stats", description = "Get platform statistics for today")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getTodaysStats() {
        DashboardStatsDto stats = analyticsService.getDashboardStats(DateRangeRequest.today());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/dashboard/last-7-days")
    @Operation(summary = "Get last 7 days stats", description = "Get platform statistics for last 7 days")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getLast7DaysStats() {
        DashboardStatsDto stats = analyticsService.getDashboardStats(DateRangeRequest.lastNDays(7));
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/dashboard/last-30-days")
    @Operation(summary = "Get last 30 days stats", description = "Get platform statistics for last 30 days")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getLast30DaysStats() {
        DashboardStatsDto stats = analyticsService.getDashboardStats(DateRangeRequest.lastNDays(30));
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/orders")
    @Operation(summary = "Get order analytics", description = "Get detailed order analytics")
    public ResponseEntity<ApiResponse<OrderAnalyticsDto>> getOrderAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        DateRangeRequest range = DateRangeRequest.builder()
                .startDate(startDate).endDate(endDate).build();
        OrderAnalyticsDto analytics = analyticsService.getOrderAnalytics(range);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @GetMapping("/sellers/top")
    @Operation(summary = "Get top sellers", description = "Get highest revenue sellers")
    public ResponseEntity<ApiResponse<List<TopSellerDto>>> getTopSellers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {

        DateRangeRequest range = DateRangeRequest.builder()
                .startDate(startDate).endDate(endDate).build();
        List<TopSellerDto> topSellers = analyticsService.getTopSellers(range, limit);
        return ResponseEntity.ok(ApiResponse.success(topSellers));
    }
}
