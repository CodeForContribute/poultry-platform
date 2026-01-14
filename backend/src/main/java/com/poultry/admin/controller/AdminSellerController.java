package com.poultry.admin.controller;

import com.poultry.admin.dto.SellerListDto;
import com.poultry.admin.service.AdminSellerService;
import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
import com.poultry.product.entity.Seller;
import com.poultry.verification.entity.SellerVerification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/sellers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Sellers", description = "Admin seller management APIs")
public class AdminSellerController
{

  @GetMapping
  @Operation(summary = "List sellers", description = "Get paginated list of sellers with filters")
  public ResponseEntity<ApiResponse<Page<SellerListDto>>> getSellers(
      @Parameter(description = "Search by name, email, GSTIN")
      @RequestParam(required = false)
      String search,
      @Parameter(description = "Filter by status")
      @RequestParam(required = false)
      String status,
      @PageableDefault(size = 20)
      Pageable pageable)
  {

    Page<SellerListDto> sellers = sellerService.getSellers(search, status, pageable);
    return ResponseEntity.ok(ApiResponse.success(sellers));
  }

  @GetMapping("/{sellerId}")
  @Operation(summary = "Get seller details", description = "Get detailed seller information")
  public ResponseEntity<ApiResponse<Seller>> getSeller(
      @PathVariable
      UUID sellerId)
  {

    Seller seller = sellerService.getSellerById(sellerId);
    return ResponseEntity.ok(ApiResponse.success(seller));
  }

  @PatchMapping("/{sellerId}/status")
  @Operation(summary = "Update seller status", description = "Activate, suspend, or deactivate a seller")
  public ResponseEntity<ApiResponse<Void>> updateSellerStatus(
      @PathVariable
      UUID sellerId,
      @RequestParam
      Seller.SellerStatus status,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    sellerService.updateSellerStatus(sellerId, status, principal.getId());
    return ResponseEntity.ok(ApiResponse.success(null, "Seller status updated"));
  }

  @PatchMapping("/{sellerId}/platform-fee")
  @Operation(summary = "Update platform fee", description = "Set custom platform fee for a seller")
  public ResponseEntity<ApiResponse<Void>> updatePlatformFee(
      @PathVariable
      UUID sellerId,
      @RequestParam
      BigDecimal feePercent,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    sellerService.updatePlatformFee(sellerId, feePercent, principal.getId());
    return ResponseEntity.ok(ApiResponse.success(null, "Platform fee updated"));
  }

  @GetMapping("/{sellerId}/verifications")
  @Operation(summary = "Get seller verifications", description = "Get all verifications for a seller")
  public ResponseEntity<ApiResponse<List<SellerVerification>>> getSellerVerifications(
      @PathVariable
      UUID sellerId)
  {

    List<SellerVerification> verifications = sellerService.getSellerVerifications(sellerId);
    return ResponseEntity.ok(ApiResponse.success(verifications));
  }

  @GetMapping("/verifications/pending")
  @Operation(summary = "Get pending verifications", description = "Get all pending seller verifications")
  public ResponseEntity<ApiResponse<Page<SellerVerification>>> getPendingVerifications(
      @PageableDefault(size = 20)
      Pageable pageable)
  {

    Page<SellerVerification> verifications = sellerService.getPendingVerifications(pageable);
    return ResponseEntity.ok(ApiResponse.success(verifications));
  }

  @PostMapping("/verifications/{verificationId}/approve")
  @Operation(summary = "Approve verification", description = "Approve a seller verification")
  public ResponseEntity<ApiResponse<SellerVerification>> approveVerification(
      @PathVariable
      UUID verificationId,
      @RequestParam(required = false)
      String remarks,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    SellerVerification verification = sellerService.approveVerification(
        verificationId, principal.getId(), remarks);
    return ResponseEntity.ok(ApiResponse.success(verification, "Verification approved"));
  }

  @PostMapping("/verifications/{verificationId}/reject")
  @Operation(summary = "Reject verification", description = "Reject a seller verification")
  public ResponseEntity<ApiResponse<SellerVerification>> rejectVerification(
      @PathVariable
      UUID verificationId,
      @RequestParam
      String reason,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    SellerVerification verification = sellerService.rejectVerification(
        verificationId, principal.getId(), reason);
    return ResponseEntity.ok(ApiResponse.success(verification, "Verification rejected"));
  }

  private final AdminSellerService sellerService;
}
