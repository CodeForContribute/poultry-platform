package com.poultry.review.controller;

import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
import com.poultry.review.dto.*;
import com.poultry.review.service.ReviewService;
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

import java.util.UUID;

@RestController
@RequestMapping("/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Seller reviews and ratings APIs")
public class ReviewController
{

  @PostMapping
  @PreAuthorize("hasRole('BUYER')")
  @Operation(summary = "Create review", description = "Create a review for a delivered order")
  public ResponseEntity<ApiResponse<ReviewDto>> createReview(
      @Valid
      @RequestBody
      CreateReviewRequest request,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    ReviewDto review = reviewService.createReview(principal.getId(), request);
    return ResponseEntity.ok(ApiResponse.success(review, "Review created successfully"));
  }

  @GetMapping("/seller/{sellerId}")
  @Operation(summary = "Get seller reviews", description = "Get all approved reviews for a seller")
  public ResponseEntity<ApiResponse<Page<ReviewDto>>> getSellerReviews(
      @PathVariable
      UUID sellerId,
      @PageableDefault(size = 20)
      Pageable pageable)
  {

    Page<ReviewDto> reviews = reviewService.getSellerReviews(sellerId, pageable);
    return ResponseEntity.ok(ApiResponse.success(reviews));
  }

  @GetMapping("/seller/{sellerId}/summary")
  @Operation(summary = "Get seller rating summary", description = "Get rating summary for a seller")
  public ResponseEntity<ApiResponse<RatingSummaryDto>> getSellerRatingSummary(
      @PathVariable
      UUID sellerId)
  {

    RatingSummaryDto summary = reviewService.getSellerRatingSummary(sellerId);
    return ResponseEntity.ok(ApiResponse.success(summary));
  }

  @GetMapping("/my")
  @PreAuthorize("hasRole('BUYER')")
  @Operation(summary = "Get my reviews", description = "Get all reviews created by the authenticated buyer")
  public ResponseEntity<ApiResponse<Page<ReviewDto>>> getMyReviews(
      @AuthenticationPrincipal
      UserPrincipal principal,
      @PageableDefault(size = 20)
      Pageable pageable)
  {

    Page<ReviewDto> reviews = reviewService.getBuyerReviews(principal.getId(), pageable);
    return ResponseEntity.ok(ApiResponse.success(reviews));
  }

  @PutMapping("/{reviewId}")
  @PreAuthorize("hasRole('BUYER')")
  @Operation(summary = "Update review", description = "Update an existing review")
  public ResponseEntity<ApiResponse<ReviewDto>> updateReview(
      @PathVariable
      UUID reviewId,
      @Valid
      @RequestBody
      UpdateReviewRequest request,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    ReviewDto review = reviewService.updateReview(principal.getId(), reviewId, request);
    return ResponseEntity.ok(ApiResponse.success(review, "Review updated successfully"));
  }

  @DeleteMapping("/{reviewId}")
  @PreAuthorize("hasRole('BUYER')")
  @Operation(summary = "Delete review", description = "Delete an existing review")
  public ResponseEntity<ApiResponse<Void>> deleteReview(
      @PathVariable
      UUID reviewId,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    reviewService.deleteReview(principal.getId(), reviewId);
    return ResponseEntity.ok(ApiResponse.success(null, "Review deleted successfully"));
  }

  @PostMapping("/{reviewId}/helpful")
  @PreAuthorize("hasRole('BUYER')")
  @Operation(summary = "Mark review helpful", description = "Mark a review as helpful")
  public ResponseEntity<ApiResponse<ReviewDto>> markHelpful(
      @PathVariable
      UUID reviewId,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    ReviewDto review = reviewService.markHelpful(principal.getId(), reviewId);
    return ResponseEntity.ok(ApiResponse.success(review));
  }

  @PostMapping("/{reviewId}/response")
  @PreAuthorize("hasRole('SELLER')")
  @Operation(summary = "Add seller response", description = "Add seller response to a review")
  public ResponseEntity<ApiResponse<ReviewDto>> addSellerResponse(
      @PathVariable
      UUID reviewId,
      @Valid
      @RequestBody
      SellerResponseRequest request,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    ReviewDto review = reviewService.addSellerResponse(principal.getId(), reviewId, request.getResponse());
    return ResponseEntity.ok(ApiResponse.success(review, "Response added successfully"));
  }
  private final ReviewService reviewService;
}
