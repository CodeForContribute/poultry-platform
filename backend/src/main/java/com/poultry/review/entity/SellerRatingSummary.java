package com.poultry.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seller_rating_summary")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerRatingSummary
{

  public void incrementRatingCount(int rating)
  {
    switch (rating)
    {
      case 1 -> rating1Count++;
      case 2 -> rating2Count++;
      case 3 -> rating3Count++;
      case 4 -> rating4Count++;
      case 5 -> rating5Count++;
    }
    totalReviews++;
  }

  public void decrementRatingCount(int rating)
  {
    switch (rating)
    {
      case 1 -> rating1Count = Math.max(0, rating1Count - 1);
      case 2 -> rating2Count = Math.max(0, rating2Count - 1);
      case 3 -> rating3Count = Math.max(0, rating3Count - 1);
      case 4 -> rating4Count = Math.max(0, rating4Count - 1);
      case 5 -> rating5Count = Math.max(0, rating5Count - 1);
    }
    totalReviews = Math.max(0, totalReviews - 1);
  }
  @Id
  @Column(name = "seller_id")
  private UUID sellerId;
  @Column(name = "total_reviews", nullable = false)
  @Builder.Default
  private Integer totalReviews = 0;
  @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
  @Builder.Default
  private BigDecimal averageRating = BigDecimal.ZERO;
  @Column(name = "average_quality", precision = 3, scale = 2)
  private BigDecimal averageQuality;
  @Column(name = "average_delivery", precision = 3, scale = 2)
  private BigDecimal averageDelivery;
  @Column(name = "average_communication", precision = 3, scale = 2)
  private BigDecimal averageCommunication;
  @Column(name = "rating_1_count", nullable = false)
  @Builder.Default
  private Integer rating1Count = 0;
  @Column(name = "rating_2_count", nullable = false)
  @Builder.Default
  private Integer rating2Count = 0;
  @Column(name = "rating_3_count", nullable = false)
  @Builder.Default
  private Integer rating3Count = 0;
  @Column(name = "rating_4_count", nullable = false)
  @Builder.Default
  private Integer rating4Count = 0;
  @Column(name = "rating_5_count", nullable = false)
  @Builder.Default
  private Integer rating5Count = 0;
  @Column(name = "last_calculated_at", nullable = false)
  @Builder.Default
  private Instant lastCalculatedAt = Instant.now();
}
