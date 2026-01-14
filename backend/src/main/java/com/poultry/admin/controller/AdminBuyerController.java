package com.poultry.admin.controller;

import com.poultry.admin.dto.BuyerListDto;
import com.poultry.admin.service.AdminBuyerService;
import com.poultry.auth.entity.Buyer;
import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
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

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/buyers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Buyers", description = "Admin buyer management APIs")
public class AdminBuyerController
{

  @GetMapping
  @Operation(summary = "List buyers", description = "Get paginated list of buyers with filters")
  public ResponseEntity<ApiResponse<Page<BuyerListDto>>> getBuyers(
      @Parameter(description = "Search by name, email, phone")
      @RequestParam(required = false)
      String search,
      @Parameter(description = "Filter by status")
      @RequestParam(required = false)
      String status,
      @PageableDefault(size = 20)
      Pageable pageable)
  {

    Page<BuyerListDto> buyers = buyerService.getBuyers(search, status, pageable);
    return ResponseEntity.ok(ApiResponse.success(buyers));
  }

  @GetMapping("/{buyerId}")
  @Operation(summary = "Get buyer details", description = "Get detailed buyer information")
  public ResponseEntity<ApiResponse<Buyer>> getBuyer(
      @PathVariable
      UUID buyerId)
  {

    Buyer buyer = buyerService.getBuyerById(buyerId);
    return ResponseEntity.ok(ApiResponse.success(buyer));
  }

  @PatchMapping("/{buyerId}/status")
  @Operation(summary = "Update buyer status", description = "Update buyer account status")
  public ResponseEntity<ApiResponse<Void>> updateBuyerStatus(
      @PathVariable
      UUID buyerId,
      @RequestParam
      Buyer.BuyerStatus status,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    buyerService.updateBuyerStatus(buyerId, status, principal.getId());
    return ResponseEntity.ok(ApiResponse.success(null, "Buyer status updated"));
  }

  @PostMapping("/{buyerId}/block")
  @Operation(summary = "Block buyer", description = "Block a buyer account")
  public ResponseEntity<ApiResponse<Void>> blockBuyer(
      @PathVariable
      UUID buyerId,
      @RequestParam
      String reason,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    buyerService.blockBuyer(buyerId, reason, principal.getId());
    return ResponseEntity.ok(ApiResponse.success(null, "Buyer blocked"));
  }

  @PostMapping("/{buyerId}/unblock")
  @Operation(summary = "Unblock buyer", description = "Unblock a buyer account")
  public ResponseEntity<ApiResponse<Void>> unblockBuyer(
      @PathVariable
      UUID buyerId,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    buyerService.unblockBuyer(buyerId, principal.getId());
    return ResponseEntity.ok(ApiResponse.success(null, "Buyer unblocked"));
  }
  private final AdminBuyerService buyerService;
}
