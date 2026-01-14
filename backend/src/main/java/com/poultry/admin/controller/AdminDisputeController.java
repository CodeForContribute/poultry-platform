package com.poultry.admin.controller;

import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
import com.poultry.dispute.dto.*;
import com.poultry.dispute.entity.Dispute.DisputeStatus;
import com.poultry.dispute.entity.Dispute.DisputeType;
import com.poultry.dispute.entity.DisputeHistory;
import com.poultry.dispute.service.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/disputes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Disputes", description = "Admin dispute management APIs")
public class AdminDisputeController
{

  @GetMapping
  @Operation(summary = "List disputes", description = "Get paginated list of disputes with filters")
  public ResponseEntity<ApiResponse<Page<DisputeDto>>> getDisputes(
      @Parameter(description = "Filter by buyer")
      @RequestParam(required = false)
      UUID buyerId,
      @Parameter(description = "Filter by seller")
      @RequestParam(required = false)
      UUID sellerId,
      @Parameter(description = "Filter by status")
      @RequestParam(required = false)
      DisputeStatus status,
      @Parameter(description = "Filter by type")
      @RequestParam(required = false)
      DisputeType type,
      @PageableDefault(size = 20)
      Pageable pageable)
  {

    Page<DisputeDto> disputes = disputeService.getDisputesWithFilters(buyerId, sellerId, status, type, pageable);
    return ResponseEntity.ok(ApiResponse.success(disputes));
  }

  @GetMapping("/stats")
  @Operation(summary = "Get dispute stats", description = "Get dispute statistics")
  public ResponseEntity<ApiResponse<DisputeStatsDto>> getDisputeStats()
  {
    DisputeStatsDto stats = disputeService.getDisputeStats();
    return ResponseEntity.ok(ApiResponse.success(stats));
  }

  @GetMapping("/{disputeId}")
  @Operation(summary = "Get dispute details", description = "Get detailed dispute information")
  public ResponseEntity<ApiResponse<DisputeDto>> getDispute(
      @PathVariable
      UUID disputeId)
  {

    DisputeDto dispute = disputeService.getDispute(disputeId);
    return ResponseEntity.ok(ApiResponse.success(dispute));
  }

  @GetMapping("/{disputeId}/history")
  @Operation(summary = "Get dispute history", description = "Get audit history for a dispute")
  public ResponseEntity<ApiResponse<List<DisputeHistory>>> getDisputeHistory(
      @PathVariable
      UUID disputeId)
  {

    List<DisputeHistory> history = disputeService.getDisputeHistory(disputeId);
    return ResponseEntity.ok(ApiResponse.success(history));
  }

  @GetMapping("/{disputeId}/messages")
  @Operation(summary = "Get dispute messages", description = "Get all messages including internal")
  public ResponseEntity<ApiResponse<List<DisputeMessageDto>>> getMessages(
      @PathVariable
      UUID disputeId)
  {

    List<DisputeMessageDto> messages = disputeService.getMessages(disputeId, true);
    return ResponseEntity.ok(ApiResponse.success(messages));
  }

  @PostMapping("/{disputeId}/messages")
  @Operation(summary = "Add admin message", description = "Add a message to the dispute (can be internal)")
  public ResponseEntity<ApiResponse<DisputeMessageDto>> addMessage(
      @PathVariable
      UUID disputeId,
      @Valid
      @RequestBody
      AddMessageRequest request,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    DisputeMessageDto message = disputeService.addMessage(disputeId, principal.getId(), "ADMIN", request);
    return ResponseEntity.ok(ApiResponse.success(message, "Message added"));
  }

  @PutMapping("/{disputeId}/assign")
  @Operation(summary = "Assign dispute", description = "Assign dispute to an admin")
  public ResponseEntity<ApiResponse<DisputeDto>> assignDispute(
      @PathVariable
      UUID disputeId,
      @RequestParam
      UUID assigneeId,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    DisputeDto dispute = disputeService.assignToAdmin(disputeId, principal.getId(), assigneeId);
    return ResponseEntity.ok(ApiResponse.success(dispute, "Dispute assigned"));
  }

  @PutMapping("/{disputeId}/status")
  @Operation(summary = "Update status", description = "Update dispute status")
  public ResponseEntity<ApiResponse<DisputeDto>> updateStatus(
      @PathVariable
      UUID disputeId,
      @RequestParam
      DisputeStatus status,
      @RequestParam(required = false)
      String notes,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    DisputeDto dispute = disputeService.updateStatus(disputeId, principal.getId(), status, notes);
    return ResponseEntity.ok(ApiResponse.success(dispute, "Status updated"));
  }

  @PutMapping("/{disputeId}/escalate")
  @Operation(summary = "Escalate dispute", description = "Escalate dispute to higher priority")
  public ResponseEntity<ApiResponse<DisputeDto>> escalateDispute(
      @PathVariable
      UUID disputeId,
      @RequestParam
      String reason,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    DisputeDto dispute = disputeService.escalateDispute(disputeId, principal.getId(), reason);
    return ResponseEntity.ok(ApiResponse.success(dispute, "Dispute escalated"));
  }

  @PutMapping("/{disputeId}/resolve")
  @Operation(summary = "Resolve dispute", description = "Resolve dispute with final resolution")
  public ResponseEntity<ApiResponse<DisputeDto>> resolveDispute(
      @PathVariable
      UUID disputeId,
      @Valid
      @RequestBody
      ResolveDisputeRequest request,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    DisputeDto dispute = disputeService.resolveDispute(disputeId, principal.getId(), request);
    return ResponseEntity.ok(ApiResponse.success(dispute, "Dispute resolved"));
  }

  private final DisputeService disputeService;
}
