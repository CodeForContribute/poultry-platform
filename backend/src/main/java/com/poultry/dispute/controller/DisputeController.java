package com.poultry.dispute.controller;

import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
import com.poultry.dispute.dto.AddMessageRequest;
import com.poultry.dispute.dto.CreateDisputeRequest;
import com.poultry.dispute.dto.DisputeDto;
import com.poultry.dispute.dto.DisputeMessageDto;
import com.poultry.dispute.entity.Dispute.RaisedBy;
import com.poultry.dispute.service.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/v1/disputes")
@RequiredArgsConstructor
@Tag(name = "Disputes", description = "Dispute management APIs")
public class DisputeController
{

  @PostMapping("/buyer")
  @PreAuthorize("hasRole('BUYER')")
  @Operation(summary = "Create buyer dispute", description = "Create a new dispute as a buyer")
  public ResponseEntity<ApiResponse<DisputeDto>> createBuyerDispute(
      @Valid
      @RequestBody
      CreateDisputeRequest request,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    DisputeDto dispute = disputeService.createDispute(principal.getId(), RaisedBy.BUYER, request);
    return ResponseEntity.ok(ApiResponse.success(dispute, "Dispute created successfully"));
  }

  @PostMapping("/seller")
  @PreAuthorize("hasRole('SELLER')")
  @Operation(summary = "Create seller dispute", description = "Create a new dispute as a seller")
  public ResponseEntity<ApiResponse<DisputeDto>> createSellerDispute(
      @Valid
      @RequestBody
      CreateDisputeRequest request,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    DisputeDto dispute = disputeService.createDispute(principal.getId(), RaisedBy.SELLER, request);
    return ResponseEntity.ok(ApiResponse.success(dispute, "Dispute created successfully"));
  }

  @GetMapping("/{disputeId}")
  @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
  @Operation(summary = "Get dispute", description = "Get dispute details by ID")
  public ResponseEntity<ApiResponse<DisputeDto>> getDispute(
      @PathVariable
      UUID disputeId)
  {

    DisputeDto dispute = disputeService.getDispute(disputeId);
    return ResponseEntity.ok(ApiResponse.success(dispute));
  }

  @GetMapping("/number/{disputeNumber}")
  @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
  @Operation(summary = "Get dispute by number", description = "Get dispute details by dispute number")
  public ResponseEntity<ApiResponse<DisputeDto>> getDisputeByNumber(
      @PathVariable
      String disputeNumber)
  {

    DisputeDto dispute = disputeService.getDisputeByNumber(disputeNumber);
    return ResponseEntity.ok(ApiResponse.success(dispute));
  }

  @GetMapping("/my/buyer")
  @PreAuthorize("hasRole('BUYER')")
  @Operation(summary = "Get my disputes as buyer", description = "Get all disputes for the authenticated buyer")
  public ResponseEntity<ApiResponse<Page<DisputeDto>>> getMyBuyerDisputes(
      @AuthenticationPrincipal
      UserPrincipal principal,
      @PageableDefault(size = 20)
      Pageable pageable)
  {

    Page<DisputeDto> disputes = disputeService.getDisputesByBuyer(principal.getId(), pageable);
    return ResponseEntity.ok(ApiResponse.success(disputes));
  }

  @GetMapping("/my/seller")
  @PreAuthorize("hasRole('SELLER')")
  @Operation(summary = "Get my disputes as seller", description = "Get all disputes for the authenticated seller")
  public ResponseEntity<ApiResponse<Page<DisputeDto>>> getMySellerDisputes(
      @AuthenticationPrincipal
      UserPrincipal principal,
      @PageableDefault(size = 20)
      Pageable pageable)
  {

    Page<DisputeDto> disputes = disputeService.getDisputesBySeller(principal.getId(), pageable);
    return ResponseEntity.ok(ApiResponse.success(disputes));
  }

  @PostMapping("/{disputeId}/messages")
  @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
  @Operation(summary = "Add message", description = "Add a message to the dispute conversation")
  public ResponseEntity<ApiResponse<DisputeMessageDto>> addMessage(
      @PathVariable
      UUID disputeId,
      @Valid
      @RequestBody
      AddMessageRequest request,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    String senderType = principal.getAuthorities().stream()
                                 .anyMatch(a -> a.getAuthority().equals("ROLE_BUYER")) ? "BUYER" : "SELLER";

    DisputeMessageDto message = disputeService.addMessage(disputeId, principal.getId(), senderType, request);
    return ResponseEntity.ok(ApiResponse.success(message, "Message added"));
  }

  @GetMapping("/{disputeId}/messages")
  @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
  @Operation(summary = "Get messages", description = "Get all messages for a dispute")
  public ResponseEntity<ApiResponse<List<DisputeMessageDto>>> getMessages(
      @PathVariable
      UUID disputeId,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    boolean includeInternal = principal.getAuthorities().stream()
                                       .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

    List<DisputeMessageDto> messages = disputeService.getMessages(disputeId, includeInternal);
    return ResponseEntity.ok(ApiResponse.success(messages));
  }
  private final DisputeService disputeService;
}
