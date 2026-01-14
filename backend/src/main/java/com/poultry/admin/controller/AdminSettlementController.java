package com.poultry.admin.controller;

import com.poultry.admin.dto.SettlementListDto;
import com.poultry.admin.service.AdminSettlementService;
import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
import com.poultry.settlement.entity.Settlement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/settlements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Settlements", description = "Admin settlement management and approval APIs")
public class AdminSettlementController
{

  @GetMapping
  @Operation(summary = "List settlements", description = "Get paginated list of settlements with filters")
  public ResponseEntity<ApiResponse<Page<SettlementListDto>>> getSettlements(
      @Parameter(description = "Filter by seller")
      @RequestParam(required = false)
      UUID sellerId,
      @Parameter(description = "Filter by status")
      @RequestParam(required = false)
      String status,
      @Parameter(description = "Filter by start date")
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      LocalDate startDate,
      @Parameter(description = "Filter by end date")
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      LocalDate endDate,
      @PageableDefault(size = 20)
      Pageable pageable)
  {

    Page<SettlementListDto> settlements = settlementService.getSettlements(
        sellerId, status, startDate, endDate, pageable);
    return ResponseEntity.ok(ApiResponse.success(settlements));
  }

  @GetMapping("/pending")
  @Operation(summary = "Get pending settlements", description = "Get all settlements pending approval")
  public ResponseEntity<ApiResponse<Page<SettlementListDto>>> getPendingSettlements(
      @PageableDefault(size = 20)
      Pageable pageable)
  {

    Page<SettlementListDto> settlements = settlementService.getPendingSettlements(pageable);
    return ResponseEntity.ok(ApiResponse.success(settlements));
  }

  @GetMapping("/stats")
  @Operation(summary = "Get settlement stats", description = "Get settlement statistics")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getSettlementStats()
  {
    Map<String, Object> stats = settlementService.getSettlementStats();
    return ResponseEntity.ok(ApiResponse.success(stats));
  }

  @GetMapping("/{settlementId}")
  @Operation(summary = "Get settlement details", description = "Get detailed settlement information")
  public ResponseEntity<ApiResponse<Settlement>> getSettlement(
      @PathVariable
      UUID settlementId)
  {

    Settlement settlement = settlementService.getSettlementById(settlementId);
    return ResponseEntity.ok(ApiResponse.success(settlement));
  }

  @PostMapping("/{settlementId}/approve")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('FINANCE') or hasAuthority('SUPER_ADMIN')")
  @Operation(summary = "Approve settlement", description = "Approve a pending settlement for payout")
  public ResponseEntity<ApiResponse<Settlement>> approveSettlement(
      @PathVariable
      UUID settlementId,
      @RequestParam(required = false)
      String remarks,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    Settlement settlement = settlementService.approveSettlement(
        settlementId, principal.getId(), remarks);
    return ResponseEntity.ok(ApiResponse.success(settlement, "Settlement approved"));
  }

  @PostMapping("/{settlementId}/reject")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('FINANCE') or hasAuthority('SUPER_ADMIN')")
  @Operation(summary = "Reject settlement", description = "Reject a pending settlement")
  public ResponseEntity<ApiResponse<Settlement>> rejectSettlement(
      @PathVariable
      UUID settlementId,
      @RequestParam
      String reason,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    Settlement settlement = settlementService.rejectSettlement(
        settlementId, principal.getId(), reason);
    return ResponseEntity.ok(ApiResponse.success(settlement, "Settlement rejected"));
  }

  @PostMapping("/{settlementId}/initiate")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('FINANCE') or hasAuthority('SUPER_ADMIN')")
  @Operation(summary = "Initiate settlement", description = "Initiate payout for an approved settlement")
  public ResponseEntity<ApiResponse<Settlement>> initiateSettlement(
      @PathVariable
      UUID settlementId,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    Settlement settlement = settlementService.initiateSettlement(
        settlementId, principal.getId());
    return ResponseEntity.ok(ApiResponse.success(settlement, "Settlement payout initiated"));
  }

  @PostMapping("/{settlementId}/mark-success")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('FINANCE') or hasAuthority('SUPER_ADMIN')")
  @Operation(summary = "Mark settlement success", description = "Mark a processing settlement as successful")
  public ResponseEntity<ApiResponse<Settlement>> markSettlementSuccess(
      @PathVariable
      UUID settlementId,
      @RequestParam
      String bankReference,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    Settlement settlement = settlementService.markSettlementSuccess(
        settlementId, bankReference, principal.getId());
    return ResponseEntity.ok(ApiResponse.success(settlement, "Settlement marked as successful"));
  }

  @PostMapping("/{settlementId}/mark-failed")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('FINANCE') or hasAuthority('SUPER_ADMIN')")
  @Operation(summary = "Mark settlement failed", description = "Mark a processing settlement as failed")
  public ResponseEntity<ApiResponse<Settlement>> markSettlementFailed(
      @PathVariable
      UUID settlementId,
      @RequestParam
      String reason,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    Settlement settlement = settlementService.markSettlementFailed(
        settlementId, reason, principal.getId());
    return ResponseEntity.ok(ApiResponse.success(settlement, "Settlement marked as failed"));
  }

  @PostMapping("/{settlementId}/retry")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('FINANCE') or hasAuthority('SUPER_ADMIN')")
  @Operation(summary = "Retry settlement", description = "Retry a failed settlement")
  public ResponseEntity<ApiResponse<Settlement>> retrySettlement(
      @PathVariable
      UUID settlementId,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    Settlement settlement = settlementService.retrySettlement(settlementId, principal.getId());
    return ResponseEntity.ok(ApiResponse.success(settlement, "Settlement queued for retry"));
  }

  private final AdminSettlementService settlementService;
}
