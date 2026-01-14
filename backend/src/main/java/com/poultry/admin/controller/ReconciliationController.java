package com.poultry.admin.controller;

import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
import com.poultry.reconciliation.dto.ReconMismatchDto;
import com.poultry.reconciliation.dto.ReconciliationRunDto;
import com.poultry.reconciliation.dto.ResolveMismatchRequest;
import com.poultry.reconciliation.dto.StartReconciliationRequest;
import com.poultry.reconciliation.service.ReconciliationService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/reconciliation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') and hasAuthority('FINANCE')")
@Tag(name = "Admin - Reconciliation", description = "Payment reconciliation management APIs")
public class ReconciliationController
{

  @PostMapping("/start")
  @Operation(summary = "Start reconciliation", description = "Start a new reconciliation run")
  public ResponseEntity<ApiResponse<ReconciliationRunDto>> startReconciliation(
      @Valid
      @RequestBody
      StartReconciliationRequest request)
  {

    ReconciliationRunDto run = reconciliationService.startReconciliation(request);
    return ResponseEntity.ok(ApiResponse.success(run, "Reconciliation started"));
  }

  @PostMapping("/runs/{runId}/upload")
  @Operation(summary = "Upload file for reconciliation", description = "Upload a file to process for reconciliation")
  public ResponseEntity<ApiResponse<ReconciliationRunDto>> uploadAndProcess(
      @PathVariable
      UUID runId,
      @RequestParam("file")
      MultipartFile file) throws IOException
  {

    String content = new String(file.getBytes(), StandardCharsets.UTF_8);
    ReconciliationRunDto run = reconciliationService.processReconciliation(runId, content);
    return ResponseEntity.ok(ApiResponse.success(run, "Reconciliation processed"));
  }

  @GetMapping("/runs")
  @Operation(summary = "List reconciliation runs", description = "Get paginated list of reconciliation runs")
  public ResponseEntity<ApiResponse<Page<ReconciliationRunDto>>> getReconciliationRuns(
      @PageableDefault(size = 20)
      Pageable pageable)
  {

    Page<ReconciliationRunDto> runs = reconciliationService.getRecentRuns(pageable);
    return ResponseEntity.ok(ApiResponse.success(runs));
  }

  @GetMapping("/runs/{runId}")
  @Operation(summary = "Get reconciliation run", description = "Get details of a reconciliation run")
  public ResponseEntity<ApiResponse<ReconciliationRunDto>> getReconciliationRun(
      @PathVariable
      UUID runId)
  {

    ReconciliationRunDto run = reconciliationService.getReconciliationRun(runId);
    return ResponseEntity.ok(ApiResponse.success(run));
  }

  @GetMapping("/runs/{runId}/mismatches")
  @Operation(summary = "Get run mismatches", description = "Get mismatches for a reconciliation run")
  public ResponseEntity<ApiResponse<Page<ReconMismatchDto>>> getRunMismatches(
      @PathVariable
      UUID runId,
      @PageableDefault(size = 20)
      Pageable pageable)
  {

    Page<ReconMismatchDto> mismatches = reconciliationService.getMismatches(runId, pageable);
    return ResponseEntity.ok(ApiResponse.success(mismatches));
  }

  @GetMapping("/mismatches/unresolved")
  @Operation(summary = "Get unresolved mismatches", description = "Get all unresolved mismatches across all runs")
  public ResponseEntity<ApiResponse<Page<ReconMismatchDto>>> getUnresolvedMismatches(
      @PageableDefault(size = 20)
      Pageable pageable)
  {

    Page<ReconMismatchDto> mismatches = reconciliationService.getUnresolvedMismatches(pageable);
    return ResponseEntity.ok(ApiResponse.success(mismatches));
  }

  @PostMapping("/mismatches/{mismatchId}/resolve")
  @Operation(summary = "Resolve mismatch", description = "Resolve a reconciliation mismatch")
  public ResponseEntity<ApiResponse<ReconMismatchDto>> resolveMismatch(
      @PathVariable
      UUID mismatchId,
      @Valid
      @RequestBody
      ResolveMismatchRequest request,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    ReconMismatchDto mismatch = reconciliationService.resolveMismatch(
        mismatchId, principal.getId(), request);
    return ResponseEntity.ok(ApiResponse.success(mismatch, "Mismatch resolved"));
  }

  @GetMapping("/stats")
  @Operation(summary = "Get reconciliation stats", description = "Get reconciliation statistics")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getReconciliationStats()
  {
    Map<String, Object> stats = reconciliationService.getReconciliationStats();
    return ResponseEntity.ok(ApiResponse.success(stats));
  }
  private final ReconciliationService reconciliationService;
}
