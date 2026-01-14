package com.poultry.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewRequest
{

  @Min(value = 1, message = "Rating must be between 1 and 5")
  @Max(value = 5, message = "Rating must be between 1 and 5")
  private Integer rating;

  @Size(max = 200, message = "Title must be at most 200 characters")
  private String title;

  private String comment;

  @Min(value = 1, message = "Quality rating must be between 1 and 5")
  @Max(value = 5, message = "Quality rating must be between 1 and 5")
  private Integer qualityRating;

  @Min(value = 1, message = "Delivery rating must be between 1 and 5")
  @Max(value = 5, message = "Delivery rating must be between 1 and 5")
  private Integer deliveryRating;

  @Min(value = 1, message = "Communication rating must be between 1 and 5")
  @Max(value = 5, message = "Communication rating must be between 1 and 5")
  private Integer communicationRating;
}
