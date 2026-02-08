package com.poultry.seller.controller;

import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
import com.poultry.seller.dto.*;
import com.poultry.seller.service.SellerSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/v1/seller")
@RequiredArgsConstructor
@Tag(name = "Seller Settlements", description = "Seller settlements and wallet management APIs")
public class SellerSettlementController {

    private final SellerSettlementService settlementService;

    // ============ Settlement Endpoints ============

    @GetMapping("/settlements")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get settlements", description = "Get paginated list of seller's settlements")
    public ResponseEntity<ApiResponse<Page<SettlementDto>>> getSettlements(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<SettlementDto> settlements = settlementService.getSettlements(
                principal.getSellerId(), status, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(settlements));
    }

    @GetMapping("/settlements/{settlementId}")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get settlement by ID", description = "Get detailed settlement information")
    public ResponseEntity<ApiResponse<SettlementDto>> getSettlementById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID settlementId) {

        SettlementDto settlement = settlementService.getSettlementById(principal.getSellerId(), settlementId);
        return ResponseEntity.ok(ApiResponse.success(settlement));
    }

    @GetMapping("/settlements/summary")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get settlement summary", description = "Get summary statistics for settlements")
    public ResponseEntity<ApiResponse<SettlementSummaryDto>> getSettlementSummary(
            @AuthenticationPrincipal UserPrincipal principal) {

        SettlementSummaryDto summary = settlementService.getSettlementSummary(principal.getSellerId());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/settlements/{settlementId}/export")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Export settlement", description = "Export settlement details as PDF or CSV")
    public ResponseEntity<byte[]> exportSettlement(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID settlementId,
            @RequestParam(defaultValue = "pdf") String format) {

        byte[] content = settlementService.exportSettlement(principal.getSellerId(), settlementId, format);

        String filename = "settlement_" + settlementId + "." + format.toLowerCase();
        MediaType mediaType = "pdf".equalsIgnoreCase(format)
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("text/csv");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(content);
    }

    // ============ Wallet Endpoints ============

    @GetMapping("/wallet")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get wallet balance", description = "Get current wallet balance and earnings")
    public ResponseEntity<ApiResponse<WalletDto>> getWallet(
            @AuthenticationPrincipal UserPrincipal principal) {

        WalletDto wallet = settlementService.getWallet(principal.getSellerId());
        return ResponseEntity.ok(ApiResponse.success(wallet));
    }

    @PostMapping("/settlements/withdraw")
    @PreAuthorize("hasRole('SELLER_ADMIN')")
    @Operation(summary = "Request withdrawal", description = "Request a withdrawal from available balance")
    public ResponseEntity<ApiResponse<SettlementDto>> requestWithdrawal(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody WithdrawalRequest request) {

        SettlementDto settlement = settlementService.requestWithdrawal(principal.getSellerId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(settlement, "Withdrawal request submitted successfully"));
    }
}
