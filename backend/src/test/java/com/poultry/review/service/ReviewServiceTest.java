package com.poultry.review.service;

import com.poultry.common.exception.BusinessException;
import com.poultry.order.entity.Order;
import com.poultry.order.repository.OrderRepository;
import com.poultry.review.dto.CreateReviewRequest;
import com.poultry.review.dto.ReviewDto;
import com.poultry.review.entity.SellerReview;
import com.poultry.review.repository.SellerRatingSummaryRepository;
import com.poultry.review.repository.SellerReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Unit Tests")
class ReviewServiceTest
{

  @BeforeEach
  void setUp()
  {
    buyerId = UUID.randomUUID();
    sellerId = UUID.randomUUID();
    orderId = UUID.randomUUID();
    reviewId = UUID.randomUUID();

    order = new Order();
    order.setId(orderId);
    order.setBuyerId(buyerId);
    order.setSellerId(sellerId);
    order.setStatus(Order.OrderStatus.DELIVERED);
    order.setTotalAmount(BigDecimal.valueOf(1000));

    review = new SellerReview();
    review.setId(reviewId);
    review.setSellerId(sellerId);
    review.setBuyerId(buyerId);
    review.setOrderId(orderId);
    review.setRating(5);
    review.setTitle("Great Service");
    review.setComment("Excellent quality chicken");
    review.setStatus(SellerReview.ReviewStatus.APPROVED);
    review.setHelpfulCount(0);
  }

  @Nested
  @DisplayName("createReview")
  class CreateReviewTests
  {

    @Test
    @DisplayName("should throw exception when order not found")
    void shouldThrowExceptionWhenOrderNotFound()
    {
      CreateReviewRequest request = new CreateReviewRequest();
      request.setOrderId(orderId);
      request.setRating(5);

      when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> reviewService.createReview(buyerId, request))
          .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("should throw exception when buyer is not the order owner")
    void shouldThrowExceptionWhenBuyerNotOrderOwner()
    {
      UUID differentBuyerId = UUID.randomUUID();
      CreateReviewRequest request = new CreateReviewRequest();
      request.setOrderId(orderId);
      request.setRating(5);

      when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

      assertThatThrownBy(() -> reviewService.createReview(differentBuyerId, request))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("not your order");
    }

    @Test
    @DisplayName("should throw exception when order not delivered")
    void shouldThrowExceptionWhenOrderNotDelivered()
    {
      order.setStatus(Order.OrderStatus.PAID);
      CreateReviewRequest request = new CreateReviewRequest();
      request.setOrderId(orderId);
      request.setRating(5);

      when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

      assertThatThrownBy(() -> reviewService.createReview(buyerId, request))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("not delivered");
    }

    @Test
    @DisplayName("should throw exception when review already exists")
    void shouldThrowExceptionWhenReviewAlreadyExists()
    {
      CreateReviewRequest request = new CreateReviewRequest();
      request.setOrderId(orderId);
      request.setRating(5);

      when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
      when(reviewRepository.existsByOrderId(orderId)).thenReturn(true);

      assertThatThrownBy(() -> reviewService.createReview(buyerId, request))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("already reviewed");
    }

    @Test
    @DisplayName("should create review successfully")
    void shouldCreateReviewSuccessfully()
    {
      CreateReviewRequest request = new CreateReviewRequest();
      request.setOrderId(orderId);
      request.setRating(5);
      request.setTitle("Great Service");
      request.setComment("Excellent quality");

      when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
      when(reviewRepository.existsByOrderId(orderId)).thenReturn(false);
      when(reviewRepository.save(any(SellerReview.class))).thenReturn(review);

      ReviewDto result = reviewService.createReview(buyerId, request);

      assertThat(result).isNotNull();
      assertThat(result.getRating()).isEqualTo(5);
      verify(reviewRepository).save(any(SellerReview.class));
    }
  }

  @Nested
  @DisplayName("getSellerReviews")
  class GetSellerReviewsTests
  {

    @Test
    @DisplayName("should return paginated reviews for seller")
    void shouldReturnPaginatedReviews()
    {
      Pageable pageable = PageRequest.of(0, 20);
      Page<SellerReview> reviewPage = new PageImpl<>(List.of(review), pageable, 1);

      when(reviewRepository.findApprovedBySellerId(sellerId, pageable)).thenReturn(reviewPage);

      Page<ReviewDto> result = reviewService.getSellerReviews(sellerId, pageable);

      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).getRating()).isEqualTo(5);
    }
  }

  @Nested
  @DisplayName("deleteReview")
  class DeleteReviewTests
  {

    @Test
    @DisplayName("should delete review successfully")
    void shouldDeleteReviewSuccessfully()
    {
      when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

      reviewService.deleteReview(buyerId, reviewId);

      verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("should throw exception when buyer is not review owner")
    void shouldThrowExceptionWhenBuyerNotReviewOwner()
    {
      UUID differentBuyerId = UUID.randomUUID();

      when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

      assertThatThrownBy(() -> reviewService.deleteReview(differentBuyerId, reviewId))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("not your review");
    }
  }

  @Nested
  @DisplayName("addSellerResponse")
  class AddSellerResponseTests
  {

    @Test
    @DisplayName("should throw exception when seller is not the review target")
    void shouldThrowExceptionWhenSellerNotReviewTarget()
    {
      UUID differentSellerId = UUID.randomUUID();
      String response = "Thank you!";

      when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

      assertThatThrownBy(() -> reviewService.addSellerResponse(differentSellerId, reviewId, response))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("not for your store");
    }

    @Test
    @DisplayName("should add seller response successfully")
    void shouldAddSellerResponseSuccessfully()
    {
      String response = "Thank you for your feedback!";

      when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
      when(reviewRepository.save(any(SellerReview.class))).thenReturn(review);

      ReviewDto result = reviewService.addSellerResponse(sellerId, reviewId, response);

      assertThat(result).isNotNull();
      verify(reviewRepository).save(any(SellerReview.class));
    }
  }
  @Mock
  private SellerReviewRepository reviewRepository;
  @Mock
  private SellerRatingSummaryRepository ratingSummaryRepository;
  @Mock
  private OrderRepository orderRepository;
  @InjectMocks
  private ReviewService reviewService;
  private UUID buyerId;
  private UUID sellerId;
  private UUID orderId;
  private UUID reviewId;
  private Order order;
  private SellerReview review;
}
