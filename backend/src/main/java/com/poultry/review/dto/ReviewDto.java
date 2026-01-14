package com.poultry.review.dto;

import com.poultry.review.entity.SellerReview;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto
{
  public static ReviewDto fromEntity(SellerReview review)
  {
    return ReviewDto.builder()
                    .id(review.getId())
                    .sellerId(review.getSellerId())
                    .buyerId(review.getBuyerId())
                    .orderId(review.getOrderId())
                    .rating(review.getRating())
                    .title(review.getTitle())
                    .comment(review.getComment())
                    .qualityRating(review.getQualityRating())
                    .deliveryRating(review.getDeliveryRating())
                    .communicationRating(review.getCommunicationRating())
                    .status(review.getStatus().name())
                    .isVerifiedPurchase(review.getIsVerifiedPurchase())
                    .helpfulCount(review.getHelpfulCount())
                    .sellerResponse(review.getSellerResponse())
                    .sellerRespondedAt(review.getSellerRespondedAt())
                    .createdAt(review.getCreatedAt())
                    .updatedAt(review.getUpdatedAt())
                    .build();
  }
  private UUID id;
  private UUID sellerId;
  private String sellerName;
  private UUID buyerId;
  private String buyerName;
  private UUID orderId;
  private Integer rating;
  private String title;
  private String comment;
  private Integer qualityRating;
  private Integer deliveryRating;
  private Integer communicationRating;
  private String status;
  private Boolean isVerifiedPurchase;
  private Integer helpfulCount;
  private String sellerResponse;
  private Instant sellerRespondedAt;
  private Instant createdAt;
  private Instant updatedAt;
}
