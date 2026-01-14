package com.poultry.verification.controller;

import com.poultry.common.dto.ApiResponse;
import com.poultry.verification.dto.SubmitVerificationRequest;
import com.poultry.verification.dto.VerificationActionRequest;
import com.poultry.verification.entity.SellerVerification;
import com.poultry.verification.service.SellerVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/verifications")
@RequiredArgsConstructor
@Tag(name = "Seller Verification", description = "Seller verification and KYC management")
public class SellerVerificationController {

    private final SellerVerificationService verificationService;

    @PostMapping("/submit")
    @Operation(summary = "Submit verification", description = "Submit a new verification request")
    public ResponseEntity<ApiResponse<SellerVerification>> submitVerification(
            @Valid @RequestBody SubmitVerificationRequest request) {
        SellerVerification verification = verificationService.submitVerification(request);
        return ResponseEntity.ok(ApiResponse.success(verification));
    }

    @GetMapping("/sellers/{sellerId}")
    @Operation(summary = "Get seller verifications", description = "Get all verifications for a seller")
    public ResponseEntity<ApiResponse<List<SellerVerification>>> getSellerVerifications(
            @PathVariable UUID sellerId) {
        List<SellerVerification> verifications = verificationService.getVerificationStatus(sellerId);
        return ResponseEntity.ok(ApiResponse.success(verifications));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get pending", description = "Get all pending verifications (Admin only)")
    public ResponseEntity<ApiResponse<List<SellerVerification>>> getPendingVerifications() {
        List<SellerVerification> verifications = verificationService.getPendingVerifications();
        return ResponseEntity.ok(ApiResponse.success(verifications));
    }

    @PostMapping("/{verificationId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Start review", description = "Mark verification as under review (Admin only)")
    public ResponseEntity<ApiResponse<SellerVerification>> startReview(
            @PathVariable UUID verificationId,
            @RequestParam UUID adminId) {
        SellerVerification verification = verificationService.startReview(verificationId, adminId);
        return ResponseEntity.ok(ApiResponse.success(verification));
    }

    @PostMapping("/{verificationId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve", description = "Approve verification (Admin only)")
    public ResponseEntity<ApiResponse<SellerVerification>> approve(
            @PathVariable UUID verificationId,
            @RequestParam UUID adminId,
            @RequestParam(required = false) String remarks) {
        SellerVerification verification = verificationService.approveVerification(verificationId, adminId, remarks);
        return ResponseEntity.ok(ApiResponse.success(verification));
    }

    @PostMapping("/{verificationId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject", description = "Reject verification (Admin only)")
    public ResponseEntity<ApiResponse<SellerVerification>> reject(
            @PathVariable UUID verificationId,
            @RequestParam UUID adminId,
            @RequestParam String reason) {
        SellerVerification verification = verificationService.rejectVerification(verificationId, adminId, reason);
        return ResponseEntity.ok(ApiResponse.success(verification));
    }
}
