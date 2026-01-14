package com.poultry.review.repository;

import com.poultry.review.entity.SellerReview;
import com.poultry.review.entity.SellerReview.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerReviewRepository
    extends JpaRepository<SellerReview, UUID>
{

  @Query(
      "SELECT r FROM SellerReview r WHERE r.sellerId = :sellerId AND r.status = 'APPROVED' ORDER BY r.createdAt DESC")
  Page<SellerReview> findApprovedBySellerId(
      @Param("sellerId")
      UUID sellerId, Pageable pageable);

  @Query("SELECT r FROM SellerReview r WHERE r.sellerId = :sellerId ORDER BY r.createdAt DESC")
  Page<SellerReview> findBySellerId(
      @Param("sellerId")
      UUID sellerId, Pageable pageable);

  Page<SellerReview> findByBuyerId(UUID buyerId, Pageable pageable);

  Optional<SellerReview> findByOrderId(UUID orderId);

  boolean existsByOrderId(UUID orderId);

  @Query("SELECT COUNT(r) FROM SellerReview r WHERE r.sellerId = :sellerId AND r.status = 'APPROVED'")
  long countApprovedBySellerId(
      @Param("sellerId")
      UUID sellerId);

  @Query("SELECT AVG(r.rating) FROM SellerReview r WHERE r.sellerId = :sellerId AND r.status = 'APPROVED'")
  BigDecimal calculateAverageRating(
      @Param("sellerId")
      UUID sellerId);

  @Query(
      "SELECT AVG(r.qualityRating) FROM SellerReview r WHERE r.sellerId = :sellerId AND r.status = 'APPROVED' AND r.qualityRating IS NOT NULL")
  BigDecimal calculateAverageQualityRating(
      @Param("sellerId")
      UUID sellerId);

  @Query(
      "SELECT AVG(r.deliveryRating) FROM SellerReview r WHERE r.sellerId = :sellerId AND r.status = 'APPROVED' AND r.deliveryRating IS NOT NULL")
  BigDecimal calculateAverageDeliveryRating(
      @Param("sellerId")
      UUID sellerId);

  @Query(
      "SELECT AVG(r.communicationRating) FROM SellerReview r WHERE r.sellerId = :sellerId AND r.status = 'APPROVED' AND r.communicationRating IS NOT NULL")
  BigDecimal calculateAverageCommunicationRating(
      @Param("sellerId")
      UUID sellerId);

  @Query(
      "SELECT COUNT(r) FROM SellerReview r WHERE r.sellerId = :sellerId AND r.status = 'APPROVED' AND r.rating = :rating")
  long countBySellerIdAndRating(
      @Param("sellerId")
      UUID sellerId,
      @Param("rating")
      int rating);

  @Query("SELECT r FROM SellerReview r WHERE r.status = :status ORDER BY r.createdAt DESC")
  Page<SellerReview> findByStatus(
      @Param("status")
      ReviewStatus status, Pageable pageable);
}
