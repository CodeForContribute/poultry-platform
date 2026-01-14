package com.poultry.review.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seller_reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerReview
{

  public enum ReviewStatus
  {
    PENDING,
    APPROVED,
    REJECTED,
    HIDDEN
  }

  /**
   * Validates that the rating is within the allowed range (1-5).
   */
  public boolean isValidRating()
  {
    return rating != null && rating >= 1 && rating <= 5;
  }

  /**
   * Validates that all optional sub-ratings are within the allowed range.
   */
  public boolean areSubRatingsValid()
  {
    return isSubRatingValid(qualityRating)
        && isSubRatingValid(deliveryRating)
        && isSubRatingValid(communicationRating);
  }

  /**
   * Checks if the review can be edited by the buyer.
   */
  public boolean canBeEditedByBuyer(UUID requestingBuyerId)
  {
    return buyerId.equals(requestingBuyerId)
        && (status == ReviewStatus.APPROVED || status == ReviewStatus.PENDING);
  }

  /**
   * Checks if the review can be deleted by the buyer.
   */
  public boolean canBeDeletedByBuyer(UUID requestingBuyerId)
  {
    return buyerId.equals(requestingBuyerId);
  }

  /**
   * Checks if the seller can respond to this review.
   */
  public boolean canSellerRespond(UUID requestingSellerId)
  {
    return sellerId.equals(requestingSellerId)
        && status == ReviewStatus.APPROVED
        && sellerResponse == null;
  }

  /**
   * Increments the helpful count.
   */
  public void incrementHelpfulCount()
  {
    this.helpfulCount = (this.helpfulCount != null ? this.helpfulCount : 0) + 1;
  }

  private boolean isSubRatingValid(Integer subRating)
  {
    return subRating == null || (subRating >= 1 && subRating <= 5);
  }
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column(name = "seller_id", nullable = false)
  private UUID sellerId;
  @Column(name = "buyer_id", nullable = false)
  private UUID buyerId;
  @Column(name = "order_id", nullable = false, unique = true)
  private UUID orderId;
  @Column(nullable = false)
  private Integer rating;
  @Column(length = 200)
  private String title;
  @Column(columnDefinition = "TEXT")
  private String comment;
  @Column(name = "quality_rating")
  private Integer qualityRating;
  @Column(name = "delivery_rating")
  private Integer deliveryRating;
  @Column(name = "communication_rating")
  private Integer communicationRating;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "review_status")
  @Builder.Default
  private ReviewStatus status = ReviewStatus.APPROVED;
  @Column(name = "is_verified_purchase", nullable = false)
  @Builder.Default
  private Boolean isVerifiedPurchase = true;
  @Column(name = "helpful_count", nullable = false)
  @Builder.Default
  private Integer helpfulCount = 0;
  @Column(name = "seller_response", columnDefinition = "TEXT")
  private String sellerResponse;
  @Column(name = "seller_responded_at")
  private Instant sellerRespondedAt;
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
