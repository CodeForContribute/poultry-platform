package com.poultry.admin.controller;

import com.poultry.admin.dto.AdminLoginRequest;
import com.poultry.admin.dto.AdminUserDto;
import com.poultry.admin.dto.CreateAdminRequest;
import com.poultry.admin.service.AdminAuthService;
import com.poultry.auth.dto.ChangePasswordRequest;
import com.poultry.auth.dto.TokenResponse;
import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Admin Authentication", description = "Admin login and account management")
public class AdminAuthController
{

  @PostMapping("/login")
  @Operation(summary = "Admin login", description = "Authenticate admin and get access tokens")
  public ResponseEntity<ApiResponse<TokenResponse>> login(
      @Valid
      @RequestBody
      AdminLoginRequest request,
      HttpServletRequest httpRequest)
  {

    String ipAddress = httpRequest.getRemoteAddr();
    String userAgent = httpRequest.getHeader("User-Agent");

    TokenResponse response = adminAuthService.login(request, ipAddress, userAgent);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @GetMapping("/me")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get current admin", description = "Get details of logged in admin")
  public ResponseEntity<ApiResponse<AdminUserDto>> getCurrentAdmin(
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    AdminUserDto admin = adminAuthService.getAdminById(principal.getId());
    return ResponseEntity.ok(ApiResponse.success(admin));
  }

  @PostMapping("/change-password")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Change password", description = "Change admin password")
  public ResponseEntity<ApiResponse<Void>> changePassword(
      @AuthenticationPrincipal
      UserPrincipal principal,
      @Valid
      @RequestBody
      ChangePasswordRequest request)
  {

    adminAuthService.changePassword(principal.getId(), request.getCurrentPassword(), request.getNewPassword());
    return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
  }

  @PostMapping("/users")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('SUPER_ADMIN')")
  @Operation(summary = "Create admin user", description = "Create new admin user (Super Admin only)")
  public ResponseEntity<ApiResponse<AdminUserDto>> createAdmin(
      @AuthenticationPrincipal
      UserPrincipal principal,
      @Valid
      @RequestBody
      CreateAdminRequest request)
  {

    AdminUserDto admin = adminAuthService.createAdmin(request, principal.getId());
    return ResponseEntity.ok(ApiResponse.success(admin, "Admin created successfully"));
  }
  private final AdminAuthService adminAuthService;
}
