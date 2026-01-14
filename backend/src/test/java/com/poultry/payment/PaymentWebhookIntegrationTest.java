package com.poultry.payment;

import com.poultry.auth.entity.Buyer;
import com.poultry.auth.repository.BuyerRepository;
import com.poultry.config.BaseIntegrationTest;
import com.poultry.order.entity.Order;
import com.poultry.order.entity.Order.OrderStatus;
import com.poultry.order.entity.OrderItem;
import com.poultry.order.repository.OrderRepository;
import com.poultry.payment.dto.CreatePaymentRequest;
import com.poultry.payment.dto.PaymentDto;
import com.poultry.payment.entity.Payment;
import com.poultry.payment.entity.Payment.PaymentStatus;
import com.poultry.payment.repository.PaymentRepository;
import com.poultry.payment.service.PaymentService;
import com.poultry.product.entity.Category;
import com.poultry.product.entity.Seller;
import com.poultry.product.repository.CategoryRepository;
import com.poultry.product.repository.SellerRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Payment Webhook Integration Tests")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PaymentWebhookIntegrationTest extends BaseIntegrationTest
{

  @Autowired
  private PaymentService paymentService;

  @Autowired
  private PaymentRepository paymentRepository;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private BuyerRepository buyerRepository;

  @Autowired
  private SellerRepository sellerRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  private Buyer testBuyer;
  private Seller testSeller;
  private Order testOrder;
  private Category testCategory;

  @BeforeEach
  void setUp()
  {
    // Create test buyer
    testBuyer = Buyer.builder()
        .phoneEncrypted("payment-test-phone".getBytes())
        .phoneHash("paymenttesthash" + UUID.randomUUID().toString().substring(0, 8))
        .name("Payment Test Buyer")
        .status(Buyer.BuyerStatus.ACTIVE)
        .build();
    testBuyer = buyerRepository.save(testBuyer);

    // Create test seller
    testSeller = Seller.builder()
        .businessName("Payment Test Poultry Farm")
        .phoneEncrypted("payment-seller-phone".getBytes())
        .email("paymenttest@test.com")
        .gstin("29XYZAB1234C1D6")
        .pan("XYZAB1234C")
        .status(Seller.SellerStatus.ACTIVE)
        .platformFeePercent(BigDecimal.valueOf(2))
        .settlementCycle(Seller.SettlementCycle.T_PLUS_1)
        .build();
    testSeller = sellerRepository.save(testSeller);

    // Create test category
    testCategory = Category.builder()
        .code(Category.CategoryCode.BROILERS)
        .name("Chicken Products")
        .hsnCode("0105")
        .description("Fresh chicken")
        .gstRate(BigDecimal.valueOf(5))
        .status(Category.ProductStatus.ACTIVE)
        .build();
    testCategory = categoryRepository.save(testCategory);

    // Create test order in SELLER_CONFIRMED state (ready for payment)
    testOrder = Order.builder()
        .buyerId(testBuyer.getId())
        .sellerId(testSeller.getId())
        .idempotencyKey(UUID.randomUUID().toString())
        .type(Order.OrderType.INSTANT)
        .status(OrderStatus.SELLER_CONFIRMED)
        .subtotal(BigDecimal.valueOf(1000))
        .gstAmount(BigDecimal.valueOf(50))
        .totalAmount(BigDecimal.valueOf(1050))
        .platformFee(BigDecimal.valueOf(21))
        .deliveryAddress(Map.of(
            "line1", "Test Address",
            "city", "Bangalore",
            "state", "Karnataka",
            "pincode", "560001",
            "contactName", "Test",
            "contactPhone", "9876543210"
        ))
        .priceSnapshot(Map.of("capturedAt", "2024-01-01T00:00:00Z"))
        .build();

    // Add order item
    OrderItem item = OrderItem.builder()
        .productId(UUID.randomUUID())
        .productSnapshot(Map.of("name", "Test Product", "sku", "TST-001", "unit", "KG", "categoryCode", "CHICKEN"))
        .quantity(BigDecimal.valueOf(5))
        .unitPrice(BigDecimal.valueOf(200))
        .gstPercent(BigDecimal.valueOf(5))
        .gstAmount(BigDecimal.valueOf(50))
        .lineTotal(BigDecimal.valueOf(1050))
        .build();
    testOrder.addItem(item);

    testOrder = orderRepository.save(testOrder);
  }

  @AfterEach
  void tearDown()
  {
    paymentRepository.deleteAll();
    orderRepository.deleteAll();
    categoryRepository.deleteAll();
    sellerRepository.deleteAll();
    buyerRepository.deleteAll();
  }

  @Nested
  @DisplayName("Payment Creation")
  class PaymentCreationTests
  {

    @Test
    @DisplayName("should create payment for confirmed order")
    void shouldCreatePaymentForConfirmedOrder()
    {
      CreatePaymentRequest request = CreatePaymentRequest.builder()
          .orderId(testOrder.getId())
          .build();

      PaymentDto payment = paymentService.createPayment(testBuyer.getId(), request);

      assertThat(payment).isNotNull();
      assertThat(payment.getId()).isNotNull();
      assertThat(payment.getOrderId()).isEqualTo(testOrder.getId());
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CREATED);
      assertThat(payment.getAmount()).isEqualByComparingTo(testOrder.getTotalAmount());
      assertThat(payment.getGatewayOrderId()).isNotNull().startsWith("order_");

      // Verify order status changed to PAYMENT_PENDING
      Order updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
      assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    @Test
    @DisplayName("should reject payment creation for unauthorized buyer")
    void shouldRejectPaymentForUnauthorizedBuyer()
    {
      UUID otherBuyerId = UUID.randomUUID();

      CreatePaymentRequest request = CreatePaymentRequest.builder()
          .orderId(testOrder.getId())
          .build();

      assertThatThrownBy(() -> paymentService.createPayment(otherBuyerId, request))
          .hasMessageContaining("don't have access");
    }

    @Test
    @DisplayName("should prevent duplicate successful payments")
    void shouldPreventDuplicatePayments()
    {
      // Create first payment and mark as successful
      Payment existingPayment = Payment.builder()
          .orderId(testOrder.getId())
          .gateway(Payment.PaymentGateway.RAZORPAY)
          .gatewayOrderId("order_existing")
          .gatewayPaymentId("pay_existing")
          .amount(testOrder.getTotalAmount())
          .status(PaymentStatus.SUCCESS)
          .build();
      paymentRepository.save(existingPayment);

      // Try to create another payment
      CreatePaymentRequest request = CreatePaymentRequest.builder()
          .orderId(testOrder.getId())
          .build();

      assertThatThrownBy(() -> paymentService.createPayment(testBuyer.getId(), request))
          .hasMessageContaining("already completed");
    }
  }

  @Nested
  @DisplayName("Payment Webhook Handling")
  class PaymentWebhookTests
  {

    @Test
    @DisplayName("should handle payment success webhook")
    void shouldHandlePaymentSuccessWebhook()
    {
      // Create payment first
      CreatePaymentRequest request = CreatePaymentRequest.builder()
          .orderId(testOrder.getId())
          .build();
      PaymentDto createdPayment = paymentService.createPayment(testBuyer.getId(), request);

      // Simulate webhook payload
      Map<String, Object> webhookPayload = createWebhookPayload(
          "payment.captured",
          createdPayment.getGatewayOrderId(),
          "pay_" + System.currentTimeMillis()
      );

      // Process webhook (signature verification skipped in test mode)
      paymentService.handleWebhook("test_signature", webhookPayload);

      // Verify payment status
      Payment updatedPayment = paymentRepository.findById(createdPayment.getId()).orElseThrow();
      assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
      assertThat(updatedPayment.getPaidAt()).isNotNull();
      assertThat(updatedPayment.getSignatureVerified()).isTrue();

      // Verify order status
      Order updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
      assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("should handle payment failure webhook")
    void shouldHandlePaymentFailureWebhook()
    {
      // Create payment first
      CreatePaymentRequest request = CreatePaymentRequest.builder()
          .orderId(testOrder.getId())
          .build();
      PaymentDto createdPayment = paymentService.createPayment(testBuyer.getId(), request);

      // Simulate failure webhook payload
      Map<String, Object> webhookPayload = createFailureWebhookPayload(
          createdPayment.getGatewayOrderId(),
          "pay_" + System.currentTimeMillis(),
          "BAD_REQUEST_ERROR",
          "Card declined by bank"
      );

      // Process webhook
      paymentService.handleWebhook("test_signature", webhookPayload);

      // Verify payment status
      Payment updatedPayment = paymentRepository.findById(createdPayment.getId()).orElseThrow();
      assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
      assertThat(updatedPayment.getErrorCode()).isEqualTo("BAD_REQUEST_ERROR");
      assertThat(updatedPayment.getErrorDescription()).isEqualTo("Card declined by bank");

      // Verify order status
      Order updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
      assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
    }

    @Test
    @DisplayName("should handle duplicate webhook gracefully")
    void shouldHandleDuplicateWebhookGracefully()
    {
      // Create payment
      CreatePaymentRequest request = CreatePaymentRequest.builder()
          .orderId(testOrder.getId())
          .build();
      PaymentDto createdPayment = paymentService.createPayment(testBuyer.getId(), request);

      String gatewayPaymentId = "pay_" + System.currentTimeMillis();

      // First webhook
      Map<String, Object> webhookPayload = createWebhookPayload(
          "payment.captured",
          createdPayment.getGatewayOrderId(),
          gatewayPaymentId
      );
      paymentService.handleWebhook("test_signature", webhookPayload);

      // Second webhook with same payment ID (duplicate)
      // Should not throw exception, just be ignored
      paymentService.handleWebhook("test_signature", webhookPayload);

      // Verify only one payment record with this gateway payment ID
      long count = paymentRepository.findAll().stream()
          .filter(p -> gatewayPaymentId.equals(p.getGatewayPaymentId()))
          .count();
      assertThat(count).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("Refund Processing")
  class RefundProcessingTests
  {

    @Test
    @DisplayName("should initiate refund for successful payment")
    void shouldInitiateRefundForSuccessfulPayment()
    {
      // Create and complete payment
      Payment successfulPayment = Payment.builder()
          .orderId(testOrder.getId())
          .gateway(Payment.PaymentGateway.RAZORPAY)
          .gatewayOrderId("order_refund_test")
          .gatewayPaymentId("pay_refund_test")
          .amount(testOrder.getTotalAmount())
          .status(PaymentStatus.SUCCESS)
          .build();
      successfulPayment = paymentRepository.save(successfulPayment);

      // Initiate refund
      paymentService.initiateRefund(testOrder.getId(), "Customer requested refund");

      // Verify refund details
      Payment refundedPayment = paymentRepository.findById(successfulPayment.getId()).orElseThrow();
      assertThat(refundedPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
      assertThat(refundedPayment.getRefundId()).isNotNull().startsWith("rfnd_");
      assertThat(refundedPayment.getRefundAmount()).isEqualByComparingTo(testOrder.getTotalAmount());
      assertThat(refundedPayment.getRefundedAt()).isNotNull();
    }

    @Test
    @DisplayName("should reject refund for already refunded payment")
    void shouldRejectRefundForAlreadyRefundedPayment()
    {
      // Create already refunded payment
      Payment refundedPayment = Payment.builder()
          .orderId(testOrder.getId())
          .gateway(Payment.PaymentGateway.RAZORPAY)
          .gatewayOrderId("order_already_refunded")
          .gatewayPaymentId("pay_already_refunded")
          .amount(testOrder.getTotalAmount())
          .status(PaymentStatus.SUCCESS)
          .refundId("rfnd_existing")
          .build();
      paymentRepository.save(refundedPayment);

      // Try to refund again
      assertThatThrownBy(() -> paymentService.initiateRefund(testOrder.getId(), "Second refund"))
          .hasMessageContaining("cannot be refunded");
    }
  }

  private Map<String, Object> createWebhookPayload(String event, String orderId, String paymentId)
  {
    Map<String, Object> entity = new HashMap<>();
    entity.put("id", paymentId);
    entity.put("order_id", orderId);
    entity.put("amount", 105000); // In paise
    entity.put("currency", "INR");
    entity.put("status", "captured");

    Map<String, Object> paymentData = new HashMap<>();
    paymentData.put("entity", entity);

    Map<String, Object> payload = new HashMap<>();
    payload.put("payment", paymentData);

    Map<String, Object> webhookPayload = new HashMap<>();
    webhookPayload.put("event", event);
    webhookPayload.put("payload", payload);

    return webhookPayload;
  }

  private Map<String, Object> createFailureWebhookPayload(String orderId, String paymentId,
      String errorCode, String errorDescription)
  {
    Map<String, Object> entity = new HashMap<>();
    entity.put("id", paymentId);
    entity.put("order_id", orderId);
    entity.put("amount", 105000);
    entity.put("currency", "INR");
    entity.put("status", "failed");
    entity.put("error_code", errorCode);
    entity.put("error_description", errorDescription);

    Map<String, Object> paymentData = new HashMap<>();
    paymentData.put("entity", entity);

    Map<String, Object> payload = new HashMap<>();
    payload.put("payment", paymentData);

    Map<String, Object> webhookPayload = new HashMap<>();
    webhookPayload.put("event", "payment.failed");
    webhookPayload.put("payload", payload);

    return webhookPayload;
  }
}
