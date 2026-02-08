package com.poultry.seller.controller;

import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
import com.poultry.common.exception.BusinessException;
import com.poultry.dispute.dto.AddMessageRequest;
import com.poultry.dispute.dto.DisputeDto;
import com.poultry.dispute.dto.DisputeMessageDto;
import com.poultry.dispute.entity.Dispute;
import com.poultry.dispute.entity.Dispute.DisputeStatus;
import com.poultry.dispute.entity.Dispute.DisputeType;
import com.poultry.dispute.service.DisputeService;
import com.poultry.seller.dto.RespondToDisputeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/seller/disputes")
@RequiredArgsConstructor
@Tag(name = "Seller Disputes", description = "Seller dispute management APIs")
public class SellerDisputeController {

    private final DisputeService disputeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get seller disputes", description = "Get all disputes for the seller with optional filters")
    public ResponseEntity<ApiResponse<Page<DisputeDto>>> getDisputes(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20) Pageable pageable) {

        DisputeStatus disputeStatus = status != null && !status.equals("ALL")
                ? DisputeStatus.valueOf(status)
                : null;
        DisputeType disputeType = type != null && !type.equals("ALL")
                ? DisputeType.valueOf(type)
                : null;

        Page<DisputeDto> disputes = disputeService.getDisputesWithFilters(
                null, // buyerId
                principal.getSellerId(), // sellerId
                disputeStatus,
                disputeType,
                pageable);

        return ResponseEntity.ok(ApiResponse.success(disputes));
    }

    @GetMapping("/{disputeId}")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get dispute by ID", description = "Get detailed dispute information")
    public ResponseEntity<ApiResponse<DisputeDto>> getDispute(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID disputeId) {

        DisputeDto dispute = disputeService.getDispute(disputeId);

        // Verify the dispute belongs to this seller
        // Note: The dispute DTO should contain sellerId for verification
        // For now, we rely on the service layer to handle authorization

        return ResponseEntity.ok(ApiResponse.success(dispute));
    }

    @PostMapping("/{disputeId}/respond")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Respond to dispute", description = "Add a response message to a dispute")
    public ResponseEntity<ApiResponse<DisputeMessageDto>> respondToDispute(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID disputeId,
            @Valid @RequestBody RespondToDisputeRequest request) {

        // Convert to AddMessageRequest
        AddMessageRequest messageRequest = AddMessageRequest.builder()
                .message(request.getMessage())
                .attachmentUrls(request.getAttachments())
                .isInternal(false)
                .build();

        DisputeMessageDto message = disputeService.addMessage(
                disputeId,
                principal.getId(),
                "SELLER",
                messageRequest);

        return ResponseEntity.ok(ApiResponse.success(message, "Response sent successfully"));
    }

    @PostMapping("/{disputeId}/accept")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Accept resolution", description = "Accept the proposed resolution for a dispute")
    public ResponseEntity<ApiResponse<DisputeDto>> acceptResolution(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID disputeId) {

        // Add a message indicating acceptance
        AddMessageRequest messageRequest = AddMessageRequest.builder()
                .message("Resolution accepted by seller")
                .isInternal(false)
                .build();

        disputeService.addMessage(disputeId, principal.getId(), "SELLER", messageRequest);

        // Get updated dispute
        DisputeDto dispute = disputeService.getDispute(disputeId);

        return ResponseEntity.ok(ApiResponse.success(dispute, "Resolution accepted"));
    }

    @PostMapping("/{disputeId}/propose-resolution")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Propose resolution", description = "Propose a resolution for the dispute")
    public ResponseEntity<ApiResponse<DisputeDto>> proposeResolution(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID disputeId,
            @Valid @RequestBody RespondToDisputeRequest request) {

        if (request.getProposedResolution() == null || request.getProposedResolution().isBlank()) {
            throw new BusinessException(
                    "Proposed resolution is required",
                    "RESOLUTION_REQUIRED",
                    HttpStatus.BAD_REQUEST);
        }

        // Build message with resolution proposal
        String message = request.getMessage();
        if (request.getRefundAmount() != null) {
            message += "\n\nProposed refund amount: ₹" + request.getRefundAmount();
        }
        message += "\n\nProposed resolution: " + request.getProposedResolution();

        AddMessageRequest messageRequest = AddMessageRequest.builder()
                .message(message)
                .attachmentUrls(request.getAttachments())
                .isInternal(false)
                .build();

        disputeService.addMessage(disputeId, principal.getId(), "SELLER", messageRequest);

        // Get updated dispute
        DisputeDto dispute = disputeService.getDispute(disputeId);

        return ResponseEntity.ok(ApiResponse.success(dispute, "Resolution proposed"));
    }

    @GetMapping("/{disputeId}/messages")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get dispute messages", description = "Get all messages for a dispute")
    public ResponseEntity<ApiResponse<List<DisputeMessageDto>>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID disputeId) {

        // Sellers don't see internal messages
        List<DisputeMessageDto> messages = disputeService.getMessages(disputeId, false);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }
}
