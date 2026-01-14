package com.poultry.review.service;

import com.poultry.common.exception.BusinessException;
import com.poultry.order.entity.Order;
import com.poultry.order.repository.OrderRepository;
import com.poultry.review.dto.CreateReviewRequest;
import com.poultry.review.dto.RatingSummaryDto;
import com.poultry.review.dto.ReviewDto;
import com.poultry.review.dto.UpdateReviewRequest;
import com.poultry.review.entity.SellerRatingSummary;
import com.poultry.review.entity.SellerReview;
import com.poultry.review.entity.SellerReview.ReviewStatus;
import com.poultry.review.repository.SellerRatingSummaryRepository;
import com.poultry.review.repository.SellerReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService
{

  @Transactional
  public ReviewDto createReview(UUID buyerId, CreateReviewRequest request)
  {
    log.info("Creating review for order {} by buyer {}", request.getOrderId(), buyerId);

    // Check if review already exists for this order
    if (reviewRepository.existsByOrderId(request.getOrderId()))
    {
      throw new BusinessException("Review already exists for this order", "REVIEW_EXISTS", HttpStatus.CONFLICT);
    }

    // Validate order exists and belongs to buyer
    Order order = orderRepository.findById(request.getOrderId())
                                 .orElseThrow(() -> new BusinessException("Order not found", "ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));

    if (!order.getBuyerId().equals(buyerId))
    {
      throw new BusinessException("Order does not belong to this buyer", "UNAUTHORIZED", HttpStatus.FORBIDDEN);
    }

    // Validate order is delivered
    if (order.getStatus() != Order.OrderStatus.DELIVERED && order.getStatus() != Order.OrderStatus.SETTLED)
    {
      throw new BusinessException("Can only review delivered orders", "ORDER_NOT_DELIVERED", HttpStatus.BAD_REQUEST);
    }

    SellerReview review = SellerReview.builder()
                                      .sellerId(order.getSellerId())
                                      .buyerId(buyerId)
                                      .orderId(request.getOrderId())
                                      .rating(request.getRating())
                                      .title(request.getTitle())
                                      .comment(request.getComment())
                                      .qualityRating(request.getQualityRating())
                                      .deliveryRating(request.getDeliveryRating())
                                      .communicationRating(request.getCommunicationRating())
                                      .isVerifiedPurchase(true)
                                      .status(ReviewStatus.APPROVED)
                                      .build();

    review = reviewRepository.save(review);

    // Update seller rating summary
    recalculateSellerRating(order.getSellerId());

    log.info("Review {} created successfully", review.getId());
    return ReviewDto.fromEntity(review);
  }

  @Transactional
  public ReviewDto updateReview(UUID buyerId, UUID reviewId, UpdateReviewRequest request)
  {
    log.info("Updating review {} by buyer {}", reviewId, buyerId);

    SellerReview review = reviewRepository.findById(reviewId)
                                          .orElseThrow(() -> new BusinessException("Review not found", "REVIEW_NOT_FOUND", HttpStatus.NOT_FOUND));

    if (!review.canBeEditedByBuyer(buyerId))
    {
      throw new BusinessException("Cannot edit this review", "UNAUTHORIZED", HttpStatus.FORBIDDEN);
    }

    int oldRating = review.getRating();

    if (request.getRating() != null)
    {
      review.setRating(request.getRating());
    }
    if (request.getTitle() != null)
    {
      review.setTitle(request.getTitle());
    }
    if (request.getComment() != null)
    {
      review.setComment(request.getComment());
    }
    if (request.getQualityRating() != null)
    {
      review.setQualityRating(request.getQualityRating());
    }
    if (request.getDeliveryRating() != null)
    {
      review.setDeliveryRating(request.getDeliveryRating());
    }
    if (request.getCommunicationRating() != null)
    {
      review.setCommunicationRating(request.getCommunicationRating());
    }

    review = reviewRepository.save(review);

    // Recalculate if rating changed
    if (request.getRating() != null && request.getRating() != oldRating)
    {
      recalculateSellerRating(review.getSellerId());
    }

    log.info("Review {} updated successfully", reviewId);
    return ReviewDto.fromEntity(review);
  }

  @Transactional
  public void deleteReview(UUID buyerId, UUID reviewId)
  {
    log.info("Deleting review {} by buyer {}", reviewId, buyerId);

    SellerReview review = reviewRepository.findById(reviewId)
                                          .orElseThrow(() -> new BusinessException("Review not found", "REVIEW_NOT_FOUND", HttpStatus.NOT_FOUND));

    if (!review.canBeDeletedByBuyer(buyerId))
    {
      throw new BusinessException("Cannot delete this review", "UNAUTHORIZED", HttpStatus.FORBIDDEN);
    }

    UUID sellerId = review.getSellerId();
    reviewRepository.delete(review);

    // Recalculate seller rating
    recalculateSellerRating(sellerId);

    log.info("Review {} deleted successfully", reviewId);
  }

  @Transactional(readOnly = true)
  public Page<ReviewDto> getSellerReviews(UUID sellerId, Pageable pageable)
  {
    log.info("Fetching reviews for seller {}", sellerId);
    return reviewRepository.findApprovedBySellerId(sellerId, pageable)
                           .map(ReviewDto::fromEntity);
  }

  @Transactional(readOnly = true)
  public Page<ReviewDto> getBuyerReviews(UUID buyerId, Pageable pageable)
  {
    log.info("Fetching reviews by buyer {}", buyerId);
    return reviewRepository.findByBuyerId(buyerId, pageable)
                           .map(ReviewDto::fromEntity);
  }

  @Transactional(readOnly = true)
  public RatingSummaryDto getSellerRatingSummary(UUID sellerId)
  {
    log.info("Fetching rating summary for seller {}", sellerId);
    return summaryRepository.findById(sellerId)
                            .map(RatingSummaryDto::fromEntity)
                            .orElseGet(() -> RatingSummaryDto.builder()
                                                             .sellerId(sellerId)
                                                             .totalReviews(0)
                                                             .averageRating(BigDecimal.ZERO)
                                                             .rating1Count(0)
                                                             .rating2Count(0)
                                                             .rating3Count(0)
                                                             .rating4Count(0)
                                                             .rating5Count(0)
                                                             .build());
  }

  @Transactional
  public ReviewDto addSellerResponse(UUID sellerId, UUID reviewId, String response)
  {
    log.info("Adding seller response to review {}", reviewId);

    SellerReview review = reviewRepository.findById(reviewId)
                                          .orElseThrow(() -> new BusinessException("Review not found", "REVIEW_NOT_FOUND", HttpStatus.NOT_FOUND));

    if (!review.canSellerRespond(sellerId))
    {
      throw new BusinessException("Cannot respond to this review", "UNAUTHORIZED", HttpStatus.FORBIDDEN);
    }

    review.setSellerResponse(response);
    review.setSellerRespondedAt(Instant.now());
    review = reviewRepository.save(review);

    log.info("Seller response added to review {}", reviewId);
    return ReviewDto.fromEntity(review);
  }

  @Transactional
  public ReviewDto markHelpful(UUID buyerId, UUID reviewId)
  {
    log.info("Marking review {} as helpful by buyer {}", reviewId, buyerId);

    SellerReview review = reviewRepository.findById(reviewId)
                                          .orElseThrow(() -> new BusinessException("Review not found", "REVIEW_NOT_FOUND", HttpStatus.NOT_FOUND));

    // Can't mark own review as helpful
    if (review.getBuyerId().equals(buyerId))
    {
      throw new BusinessException("Cannot mark own review as helpful", "INVALID_ACTION", HttpStatus.BAD_REQUEST);
    }

    review.incrementHelpfulCount();
    review = reviewRepository.save(review);

    return ReviewDto.fromEntity(review);
  }

  @Transactional
  public void recalculateSellerRating(UUID sellerId)
  {
    log.info("Recalculating rating summary for seller {}", sellerId);

    BigDecimal avgRating = reviewRepository.calculateAverageRating(sellerId);
    BigDecimal avgQuality = reviewRepository.calculateAverageQualityRating(sellerId);
    BigDecimal avgDelivery = reviewRepository.calculateAverageDeliveryRating(sellerId);
    BigDecimal avgCommunication = reviewRepository.calculateAverageCommunicationRating(sellerId);
    long totalReviews = reviewRepository.countApprovedBySellerId(sellerId);

    SellerRatingSummary summary = summaryRepository.findById(sellerId)
                                                   .orElseGet(() -> SellerRatingSummary.builder()
                                                                                       .sellerId(sellerId)
                                                                                       .build());

    summary.setTotalReviews((int) totalReviews);
    summary.setAverageRating(avgRating != null ? avgRating.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
    summary.setAverageQuality(avgQuality != null ? avgQuality.setScale(2, RoundingMode.HALF_UP) : null);
    summary.setAverageDelivery(avgDelivery != null ? avgDelivery.setScale(2, RoundingMode.HALF_UP) : null);
    summary.setAverageCommunication(avgCommunication != null ? avgCommunication.setScale(2, RoundingMode.HALF_UP) : null);

    summary.setRating1Count((int) reviewRepository.countBySellerIdAndRating(sellerId, 1));
    summary.setRating2Count((int) reviewRepository.countBySellerIdAndRating(sellerId, 2));
    summary.setRating3Count((int) reviewRepository.countBySellerIdAndRating(sellerId, 3));
    summary.setRating4Count((int) reviewRepository.countBySellerIdAndRating(sellerId, 4));
    summary.setRating5Count((int) reviewRepository.countBySellerIdAndRating(sellerId, 5));

    summary.setLastCalculatedAt(Instant.now());

    summaryRepository.save(summary);
    log.info("Rating summary updated for seller {}", sellerId);
  }

  // Admin methods
  @Transactional(readOnly = true)
  public Page<ReviewDto> getPendingReviews(Pageable pageable)
  {
    return reviewRepository.findByStatus(ReviewStatus.PENDING, pageable)
                           .map(ReviewDto::fromEntity);
  }

  @Transactional
  public ReviewDto moderateReview(UUID reviewId, ReviewStatus status)
  {
    log.info("Moderating review {} to status {}", reviewId, status);

    SellerReview review = reviewRepository.findById(reviewId)
                                          .orElseThrow(() -> new BusinessException("Review not found", "REVIEW_NOT_FOUND", HttpStatus.NOT_FOUND));

    review.setStatus(status);
    review = reviewRepository.save(review);

    // Recalculate seller rating if status changed
    recalculateSellerRating(review.getSellerId());

    return ReviewDto.fromEntity(review);
  }
  private final SellerReviewRepository reviewRepository;
  private final SellerRatingSummaryRepository summaryRepository;
  private final OrderRepository orderRepository;
}
