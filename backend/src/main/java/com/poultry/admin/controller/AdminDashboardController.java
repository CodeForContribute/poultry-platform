package com.poultry.admin.controller;

import com.poultry.admin.dto.AdminDashboardDto;
import com.poultry.admin.service.AdminDashboardService;
import com.poultry.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Dashboard", description = "Admin dashboard and overview APIs")
public class AdminDashboardController
{

  @GetMapping
  @Operation(summary = "Get dashboard", description = "Get admin dashboard with overview metrics")
  public ResponseEntity<ApiResponse<AdminDashboardDto>> getDashboard()
  {
    AdminDashboardDto dashboard = dashboardService.getDashboard();
    return ResponseEntity.ok(ApiResponse.success(dashboard));
  }

  private final AdminDashboardService dashboardService;
}
