package com.poultry.review.dto;

import com.poultry.review.entity.SellerRatingSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingSummaryDto
{
  public static RatingSummaryDto fromEntity(SellerRatingSummary summary)
  {
    return RatingSummaryDto.builder()
                           .sellerId(summary.getSellerId())
                           .totalReviews(summary.getTotalReviews())
                           .averageRating(summary.getAverageRating())
                           .averageQuality(summary.getAverageQuality())
                           .averageDelivery(summary.getAverageDelivery())
                           .averageCommunication(summary.getAverageCommunication())
                           .rating1Count(summary.getRating1Count())
                           .rating2Count(summary.getRating2Count())
                           .rating3Count(summary.getRating3Count())
                           .rating4Count(summary.getRating4Count())
                           .rating5Count(summary.getRating5Count())
                           .lastCalculatedAt(summary.getLastCalculatedAt())
                           .build();
  }
  private UUID sellerId;
  private String sellerName;
  private Integer totalReviews;
  private BigDecimal averageRating;
  private BigDecimal averageQuality;
  private BigDecimal averageDelivery;
  private BigDecimal averageCommunication;
  private Integer rating1Count;
  private Integer rating2Count;
  private Integer rating3Count;
  private Integer rating4Count;
  private Integer rating5Count;
  private Instant lastCalculatedAt;
}
