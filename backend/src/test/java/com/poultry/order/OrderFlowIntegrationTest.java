package com.poultry.order;

import com.poultry.auth.entity.Buyer;
import com.poultry.auth.repository.BuyerRepository;
import com.poultry.config.BaseIntegrationTest;
import com.poultry.order.dto.CreateOrderRequest;
import com.poultry.order.dto.OrderDto;
import com.poultry.order.dto.UpdateOrderStatusRequest;
import com.poultry.order.entity.Order;
import com.poultry.order.entity.Order.OrderStatus;
import com.poultry.order.repository.OrderRepository;
import com.poultry.order.service.OrderService;
import com.poultry.payment.dto.CreatePaymentRequest;
import com.poultry.payment.dto.PaymentDto;
import com.poultry.payment.entity.Payment;
import com.poultry.payment.repository.PaymentRepository;
import com.poultry.payment.service.PaymentService;
import com.poultry.product.entity.Category;
import com.poultry.product.entity.PriceHistory;
import com.poultry.product.entity.Product;
import com.poultry.product.entity.Seller;
import com.poultry.product.repository.CategoryRepository;
import com.poultry.product.repository.PriceHistoryRepository;
import com.poultry.product.repository.ProductRepository;
import com.poultry.product.repository.SellerRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Order Flow Integration Tests")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderFlowIntegrationTest extends BaseIntegrationTest
{

  @Autowired
  private OrderService orderService;

  @Autowired
  private PaymentService paymentService;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private PaymentRepository paymentRepository;

  @Autowired
  private BuyerRepository buyerRepository;

  @Autowired
  private SellerRepository sellerRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  @Autowired
  private PriceHistoryRepository priceHistoryRepository;

  private Buyer testBuyer;
  private Seller testSeller;
  private Product testProduct;
  private Category testCategory;

  @BeforeEach
  void setUp()
  {
    // Create test buyer
    testBuyer = Buyer.builder()
        .phoneEncrypted("test-encrypted-phone".getBytes())
        .phoneHash("testhash123")
        .name("Test Buyer")
        .status(Buyer.BuyerStatus.ACTIVE)
        .build();
    testBuyer = buyerRepository.save(testBuyer);

    // Create test seller
    testSeller = Seller.builder()
        .businessName("Test Poultry Farm")
        .phoneEncrypted("test-seller-phone".getBytes())
        .email("seller@test.com")
        .gstin("29ABCDE1234F1Z5")
        .pan("ABCDE1234F")
        .status(Seller.SellerStatus.ACTIVE)
        .platformFeePercent(BigDecimal.valueOf(2))
        .settlementCycle(Seller.SettlementCycle.T_PLUS_1)
        .build();
    testSeller = sellerRepository.save(testSeller);

    // Create test category
    testCategory = Category.builder()
        .code(Category.CategoryCode.BROILERS)
        .name("Broilers")
        .hsnCode("0105")
        .description("Fresh chicken products")
        .gstRate(BigDecimal.valueOf(5))
        .status(Category.ProductStatus.ACTIVE)
        .build();
    testCategory = categoryRepository.save(testCategory);

    // Create test product
    testProduct = Product.builder()
        .sellerId(testSeller.getId())
        .category(testCategory)
        .name("Fresh Broiler Chicken")
        .sku("BRL-001")
        .description("Fresh farm-raised broiler chicken")
        .unit(Product.ProductUnit.KG)
        .minOrderQty(BigDecimal.ONE)
        .maxOrderQty(BigDecimal.valueOf(100))
        .status(Product.ProductStatus.ACTIVE)
        .deletedAt(null)
        .build();
    testProduct = productRepository.save(testProduct);

    // Create price for product
    PriceHistory priceHistory = PriceHistory.builder()
        .productId(testProduct.getId())
        .basePrice(BigDecimal.valueOf(250))
        .effectiveFrom(Instant.now().minusSeconds(3600))
        .bulkDiscountSlabs(List.of())
        .build();
    priceHistoryRepository.save(priceHistory);
  }

  @AfterEach
  void tearDown()
  {
    paymentRepository.deleteAll();
    orderRepository.deleteAll();
    priceHistoryRepository.deleteAll();
    productRepository.deleteAll();
    categoryRepository.deleteAll();
    sellerRepository.deleteAll();
    buyerRepository.deleteAll();
  }

  @Nested
  @DisplayName("Complete Order Flow")
  class CompleteOrderFlowTests
  {

    @Test
    @DisplayName("should complete full order flow: create -> place -> confirm -> payment -> paid")
    void shouldCompleteFullOrderFlow()
    {
      // Step 1: Create order
      CreateOrderRequest createRequest = CreateOrderRequest.builder()
          .idempotencyKey(UUID.randomUUID().toString())
          .sellerId(testSeller.getId())
          .type(Order.OrderType.INSTANT)
          .items(List.of(
              CreateOrderRequest.OrderItemRequest.builder()
                  .productId(testProduct.getId())
                  .quantity(BigDecimal.valueOf(5))
                  .build()
          ))
          .deliveryAddress(CreateOrderRequest.DeliveryAddress.builder()
              .line1("123 Test Street")
              .city("Bangalore")
              .state("Karnataka")
              .pincode("560001")
              .contactName("Test Contact")
              .contactPhone("9876543210")
              .build())
          .build();

      OrderDto createdOrder = orderService.createOrder(testBuyer.getId(), createRequest);

      assertThat(createdOrder).isNotNull();
      assertThat(createdOrder.getId()).isNotNull();
      assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.DRAFT);
      assertThat(createdOrder.getTotalAmount()).isGreaterThan(BigDecimal.ZERO);
      assertThat(createdOrder.getItems()).hasSize(1);

      // Step 2: Place order
      OrderDto placedOrder = orderService.placeOrder(testBuyer.getId(), createdOrder.getId());

      assertThat(placedOrder.getStatus()).isEqualTo(OrderStatus.PLACED);

      // Step 3: Seller confirms order
      UpdateOrderStatusRequest confirmRequest = UpdateOrderStatusRequest.builder()
          .status(OrderStatus.SELLER_CONFIRMED)
          .expectedVersion(placedOrder.getVersion())
          .build();

      OrderDto confirmedOrder = orderService.updateOrderStatus(
          placedOrder.getId(), confirmRequest, "SELLER", testSeller.getId());

      assertThat(confirmedOrder.getStatus()).isEqualTo(OrderStatus.SELLER_CONFIRMED);
      assertThat(confirmedOrder.getExpiresAt()).isNotNull(); // Payment expiry set

      // Step 4: Create payment
      CreatePaymentRequest paymentRequest = CreatePaymentRequest.builder()
          .orderId(confirmedOrder.getId())
          .build();

      PaymentDto payment = paymentService.createPayment(testBuyer.getId(), paymentRequest);

      assertThat(payment).isNotNull();
      assertThat(payment.getOrderId()).isEqualTo(confirmedOrder.getId());
      assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.CREATED);
      assertThat(payment.getGatewayOrderId()).isNotNull();

      // Verify order status changed to PAYMENT_PENDING
      Order orderAfterPayment = orderRepository.findById(confirmedOrder.getId()).orElseThrow();
      assertThat(orderAfterPayment.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    @Test
    @DisplayName("should handle idempotent order creation")
    void shouldHandleIdempotentOrderCreation()
    {
      String idempotencyKey = UUID.randomUUID().toString();

      CreateOrderRequest request = CreateOrderRequest.builder()
          .idempotencyKey(idempotencyKey)
          .sellerId(testSeller.getId())
          .type(Order.OrderType.INSTANT)
          .items(List.of(
              CreateOrderRequest.OrderItemRequest.builder()
                  .productId(testProduct.getId())
                  .quantity(BigDecimal.valueOf(5))
                  .build()
          ))
          .deliveryAddress(CreateOrderRequest.DeliveryAddress.builder()
              .line1("123 Test Street")
              .city("Bangalore")
              .state("Karnataka")
              .pincode("560001")
              .contactName("Test Contact")
              .contactPhone("9876543210")
              .build())
          .build();

      // Create first order
      OrderDto firstOrder = orderService.createOrder(testBuyer.getId(), request);

      // Try to create again with same idempotency key
      OrderDto secondOrder = orderService.createOrder(testBuyer.getId(), request);

      // Should return same order
      assertThat(secondOrder.getId()).isEqualTo(firstOrder.getId());

      // Verify only one order exists
      long orderCount = orderRepository.count();
      assertThat(orderCount).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("Order State Transitions")
  class OrderStateTransitionTests
  {

    @Test
    @DisplayName("should allow seller to reject order")
    void shouldAllowSellerToRejectOrder()
    {
      // Create and place order
      OrderDto order = createAndPlaceOrder();

      // Seller rejects
      UpdateOrderStatusRequest rejectRequest = UpdateOrderStatusRequest.builder()
          .status(OrderStatus.SELLER_REJECTED)
          .reason("Out of stock")
          .expectedVersion(order.getVersion())
          .build();

      OrderDto rejectedOrder = orderService.updateOrderStatus(
          order.getId(), rejectRequest, "SELLER", testSeller.getId());

      assertThat(rejectedOrder.getStatus()).isEqualTo(OrderStatus.SELLER_REJECTED);
    }

    @Test
    @DisplayName("should allow buyer to cancel paid order")
    void shouldAllowBuyerToCancelPaidOrder()
    {
      // Create, place, and move to PAID status
      OrderDto order = createAndPlaceOrder();

      // Seller confirms
      UpdateOrderStatusRequest confirmRequest = UpdateOrderStatusRequest.builder()
          .status(OrderStatus.SELLER_CONFIRMED)
          .expectedVersion(order.getVersion())
          .build();
      order = orderService.updateOrderStatus(order.getId(), confirmRequest, "SELLER", testSeller.getId());

      // Simulate payment success (move to PAID)
      UpdateOrderStatusRequest paidRequest = UpdateOrderStatusRequest.builder()
          .status(OrderStatus.PAID)
          .expectedVersion(order.getVersion())
          .build();
      order = orderService.updateOrderStatus(order.getId(), paidRequest, "SYSTEM", null);

      assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
      assertThat(order.isCanCancel()).isTrue();

      // Buyer cancels
      UpdateOrderStatusRequest cancelRequest = UpdateOrderStatusRequest.builder()
          .status(OrderStatus.CANCELLED_BY_BUYER)
          .reason("Changed my mind")
          .expectedVersion(order.getVersion())
          .build();

      OrderDto cancelledOrder = orderService.updateOrderStatus(
          order.getId(), cancelRequest, "BUYER", testBuyer.getId());

      assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED_BY_BUYER);
      assertThat(cancelledOrder.getCancellationReason()).isEqualTo("Changed my mind");
    }
  }

  @Nested
  @DisplayName("Order Calculations")
  class OrderCalculationTests
  {

    @Test
    @DisplayName("should calculate order totals correctly")
    void shouldCalculateOrderTotalsCorrectly()
    {
      CreateOrderRequest request = CreateOrderRequest.builder()
          .idempotencyKey(UUID.randomUUID().toString())
          .sellerId(testSeller.getId())
          .type(Order.OrderType.INSTANT)
          .items(List.of(
              CreateOrderRequest.OrderItemRequest.builder()
                  .productId(testProduct.getId())
                  .quantity(BigDecimal.valueOf(10)) // 10 KG
                  .build()
          ))
          .deliveryAddress(CreateOrderRequest.DeliveryAddress.builder()
              .line1("123 Test Street")
              .city("Bangalore")
              .state("Karnataka")
              .pincode("560001")
              .contactName("Test Contact")
              .contactPhone("9876543210")
              .build())
          .build();

      OrderDto order = orderService.createOrder(testBuyer.getId(), request);

      // Base price: 250/kg * 10kg = 2500
      // GST 5%: 125
      // Total: 2625
      assertThat(order.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(2500));
      assertThat(order.getGstAmount()).isEqualByComparingTo(BigDecimal.valueOf(125));
      assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(2625));

      // Platform fee should be calculated (2%)
      assertThat(order.getPlatformFee()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("should capture price snapshot at order creation")
    void shouldCapturePriceSnapshotAtOrderCreation()
    {
      OrderDto order = createAndPlaceOrder();

      assertThat(order.getPriceSnapshot()).isNotNull();
      assertThat(order.getPriceSnapshot()).containsKey("items");
      assertThat(order.getPriceSnapshot()).containsKey("capturedAt");
    }
  }

  private OrderDto createAndPlaceOrder()
  {
    CreateOrderRequest request = CreateOrderRequest.builder()
        .idempotencyKey(UUID.randomUUID().toString())
        .sellerId(testSeller.getId())
        .type(Order.OrderType.INSTANT)
        .items(List.of(
            CreateOrderRequest.OrderItemRequest.builder()
                .productId(testProduct.getId())
                .quantity(BigDecimal.valueOf(5))
                .build()
        ))
        .deliveryAddress(CreateOrderRequest.DeliveryAddress.builder()
            .line1("123 Test Street")
            .city("Bangalore")
            .state("Karnataka")
            .pincode("560001")
            .contactName("Test Contact")
            .contactPhone("9876543210")
            .build())
        .build();

    OrderDto order = orderService.createOrder(testBuyer.getId(), request);
    return orderService.placeOrder(testBuyer.getId(), order.getId());
  }
}
