package com.poultry.payment.service;

import com.poultry.common.exception.BusinessException;
import com.poultry.ledger.service.LedgerService;
import com.poultry.notification.service.NotificationService;
import com.poultry.order.entity.Order;
import com.poultry.order.repository.OrderRepository;
import com.poultry.order.statemachine.OrderStateMachine;
import com.poultry.payment.dto.CreatePaymentRequest;
import com.poultry.payment.dto.PaymentDto;
import com.poultry.payment.entity.Payment;
import com.poultry.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStateMachine orderStateMachine;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RazorpayService razorpayService;

    @InjectMocks
    private PaymentService paymentService;

    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;

    private UUID buyerId;
    private UUID orderId;
    private UUID paymentId;
    private Order order;
    private Payment payment;
    private CreatePaymentRequest createPaymentRequest;

    @BeforeEach
    void setUp() {
        buyerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        order = Order.builder()
                .id(orderId)
                .buyerId(buyerId)
                .sellerId(UUID.randomUUID())
                .orderNumber("ORD-123456")
                .status(Order.OrderStatus.SELLER_CONFIRMED)
                .totalAmount(new BigDecimal("1500.00"))
                .build();

        payment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .gateway(Payment.PaymentGateway.RAZORPAY)
                .gatewayOrderId("order_razorpay123")
                .gatewayPaymentId("pay_razorpay456")
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .status(Payment.PaymentStatus.SUCCESS)
                .paidAt(Instant.now())
                .build();

        createPaymentRequest = CreatePaymentRequest.builder()
                .orderId(orderId)
                .build();
    }

    @Nested
    @DisplayName("createPayment")
    class CreatePaymentTests {

        @Test
        @DisplayName("should create payment successfully for valid order")
        void shouldCreatePaymentSuccessfully() {
            // Arrange
            Map<String, Object> razorpayOrder = Map.of(
                    "id", "order_razorpay123",
                    "amount", 150000,
                    "currency", "INR",
                    "status", "created"
            );

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderStateMachine.requiresPayment(Order.OrderStatus.SELLER_CONFIRMED)).thenReturn(true);
            when(paymentRepository.countSuccessfulPaymentsForOrder(orderId)).thenReturn(0L);
            when(razorpayService.createOrder(any(), anyString(), anyString(), any())).thenReturn(razorpayOrder);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment p = invocation.getArgument(0);
                p.setId(paymentId);
                return p;
            });
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(razorpayService.getRazorpayKeyId()).thenReturn("rzp_test_key");

            // Act
            PaymentDto result = paymentService.createPayment(buyerId, createPaymentRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getGatewayOrderId()).isEqualTo("order_razorpay123");
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));

            verify(paymentRepository).save(paymentCaptor.capture());
            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(Payment.PaymentStatus.CREATED);
            assertThat(savedPayment.getGateway()).isEqualTo(Payment.PaymentGateway.RAZORPAY);
        }

        @Test
        @DisplayName("should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.createPayment(buyerId, createPaymentRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Order not found");
        }

        @Test
        @DisplayName("should throw exception when buyer does not own order")
        void shouldThrowExceptionWhenBuyerDoesNotOwnOrder() {
            // Arrange
            UUID differentBuyerId = UUID.randomUUID();
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.createPayment(differentBuyerId, createPaymentRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("don't have access to this order");
        }

        @Test
        @DisplayName("should throw exception when order is not in payment required state")
        void shouldThrowExceptionWhenOrderNotInPaymentRequiredState() {
            // Arrange
            order.setStatus(Order.OrderStatus.DRAFT);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderStateMachine.requiresPayment(Order.OrderStatus.DRAFT)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> paymentService.createPayment(buyerId, createPaymentRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Order is not in a state that requires payment");
        }

        @Test
        @DisplayName("should throw exception when payment already completed")
        void shouldThrowExceptionWhenPaymentAlreadyCompleted() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderStateMachine.requiresPayment(Order.OrderStatus.SELLER_CONFIRMED)).thenReturn(true);
            when(paymentRepository.countSuccessfulPaymentsForOrder(orderId)).thenReturn(1L);

            // Act & Assert
            assertThatThrownBy(() -> paymentService.createPayment(buyerId, createPaymentRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Payment already completed")
                    .extracting("errorCode")
                    .isEqualTo("DUPLICATE_PAYMENT");
        }

        @Test
        @DisplayName("should update order status to PAYMENT_PENDING when order is SELLER_CONFIRMED")
        void shouldUpdateOrderStatusToPaymentPending() {
            // Arrange
            Map<String, Object> razorpayOrder = Map.of(
                    "id", "order_razorpay123",
                    "amount", 150000,
                    "currency", "INR",
                    "status", "created"
            );

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderStateMachine.requiresPayment(Order.OrderStatus.SELLER_CONFIRMED)).thenReturn(true);
            when(paymentRepository.countSuccessfulPaymentsForOrder(orderId)).thenReturn(0L);
            when(razorpayService.createOrder(any(), anyString(), anyString(), any())).thenReturn(razorpayOrder);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment p = invocation.getArgument(0);
                p.setId(paymentId);
                return p;
            });
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(razorpayService.getRazorpayKeyId()).thenReturn("rzp_test_key");

            // Act
            paymentService.createPayment(buyerId, createPaymentRequest);

            // Assert
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getStatus()).isEqualTo(Order.OrderStatus.PAYMENT_PENDING);
        }
    }

    @Nested
    @DisplayName("createPaymentFallback")
    class CreatePaymentFallbackTests {

        @Test
        @DisplayName("should throw SERVICE_UNAVAILABLE exception in fallback")
        void shouldThrowServiceUnavailableInFallback() {
            // Act & Assert
            assertThatThrownBy(() -> paymentService.createPaymentFallback(
                    buyerId, createPaymentRequest, new RuntimeException("Gateway error")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Payment service temporarily unavailable")
                    .extracting("errorCode")
                    .isEqualTo("PAYMENT_SERVICE_UNAVAILABLE");
        }
    }

    @Nested
    @DisplayName("handleWebhook")
    class HandleWebhookTests {

        @Test
        @DisplayName("should handle payment.captured webhook successfully")
        void shouldHandlePaymentCapturedWebhook() {
            // Arrange
            String signature = "valid_signature";
            Map<String, Object> entity = new HashMap<>();
            entity.put("id", "pay_razorpay456");
            entity.put("order_id", "order_razorpay123");

            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("entity", entity);

            Map<String, Object> payloadData = new HashMap<>();
            payloadData.put("payment", paymentData);

            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "payment.captured");
            payload.put("payload", payloadData);

            Payment existingPayment = Payment.builder()
                    .id(paymentId)
                    .orderId(orderId)
                    .gateway(Payment.PaymentGateway.RAZORPAY)
                    .gatewayOrderId("order_razorpay123")
                    .amount(new BigDecimal("1500.00"))
                    .status(Payment.PaymentStatus.CREATED)
                    .build();

            when(razorpayService.verifyWebhookSignature(anyString(), eq(signature))).thenReturn(true);
            when(paymentRepository.existsByGatewayPaymentId("pay_razorpay456")).thenReturn(false);
            when(paymentRepository.findByGatewayOrderId("order_razorpay123")).thenReturn(Optional.of(existingPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(existingPayment);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            // Act
            paymentService.handleWebhook(signature, payload);

            // Assert
            verify(paymentRepository).save(paymentCaptor.capture());
            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(Payment.PaymentStatus.SUCCESS);
            assertThat(savedPayment.getGatewayPaymentId()).isEqualTo("pay_razorpay456");
            assertThat(savedPayment.getSignatureVerified()).isTrue();
        }

        @Test
        @DisplayName("should handle payment.failed webhook successfully")
        void shouldHandlePaymentFailedWebhook() {
            // Arrange
            String signature = "valid_signature";
            Map<String, Object> entity = new HashMap<>();
            entity.put("id", "pay_razorpay456");
            entity.put("order_id", "order_razorpay123");
            entity.put("error_code", "BAD_REQUEST_ERROR");
            entity.put("error_description", "Payment failed due to insufficient funds");

            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("entity", entity);

            Map<String, Object> payloadData = new HashMap<>();
            payloadData.put("payment", paymentData);

            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "payment.failed");
            payload.put("payload", payloadData);

            Payment existingPayment = Payment.builder()
                    .id(paymentId)
                    .orderId(orderId)
                    .gateway(Payment.PaymentGateway.RAZORPAY)
                    .gatewayOrderId("order_razorpay123")
                    .amount(new BigDecimal("1500.00"))
                    .status(Payment.PaymentStatus.CREATED)
                    .build();

            order.setStatus(Order.OrderStatus.PAYMENT_PENDING);

            when(razorpayService.verifyWebhookSignature(anyString(), eq(signature))).thenReturn(true);
            when(paymentRepository.existsByGatewayPaymentId("pay_razorpay456")).thenReturn(false);
            when(paymentRepository.findByGatewayOrderId("order_razorpay123")).thenReturn(Optional.of(existingPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(existingPayment);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            // Act
            paymentService.handleWebhook(signature, payload);

            // Assert
            verify(paymentRepository).save(paymentCaptor.capture());
            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
            assertThat(savedPayment.getErrorCode()).isEqualTo("BAD_REQUEST_ERROR");
            assertThat(savedPayment.getErrorDescription()).isEqualTo("Payment failed due to insufficient funds");
        }

        @Test
        @DisplayName("should throw exception when signature verification fails")
        void shouldThrowExceptionWhenSignatureVerificationFails() {
            // Arrange
            String signature = "invalid_signature";
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "payment.captured");

            when(razorpayService.verifyWebhookSignature(anyString(), eq(signature))).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> paymentService.handleWebhook(signature, payload))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid signature")
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("should skip duplicate webhook")
        void shouldSkipDuplicateWebhook() {
            // Arrange
            String signature = "valid_signature";
            Map<String, Object> entity = new HashMap<>();
            entity.put("id", "pay_razorpay456");
            entity.put("order_id", "order_razorpay123");

            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("entity", entity);

            Map<String, Object> payloadData = new HashMap<>();
            payloadData.put("payment", paymentData);

            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "payment.captured");
            payload.put("payload", payloadData);

            when(razorpayService.verifyWebhookSignature(anyString(), eq(signature))).thenReturn(true);
            when(paymentRepository.existsByGatewayPaymentId("pay_razorpay456")).thenReturn(true);

            // Act
            paymentService.handleWebhook(signature, payload);

            // Assert
            verify(paymentRepository, never()).findByGatewayOrderId(anyString());
            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("should throw exception when payment not found for gateway order")
        void shouldThrowExceptionWhenPaymentNotFound() {
            // Arrange
            String signature = "valid_signature";
            Map<String, Object> entity = new HashMap<>();
            entity.put("id", "pay_razorpay456");
            entity.put("order_id", "order_unknown");

            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("entity", entity);

            Map<String, Object> payloadData = new HashMap<>();
            payloadData.put("payment", paymentData);

            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "payment.captured");
            payload.put("payload", payloadData);

            when(razorpayService.verifyWebhookSignature(anyString(), eq(signature))).thenReturn(true);
            when(paymentRepository.existsByGatewayPaymentId("pay_razorpay456")).thenReturn(false);
            when(paymentRepository.findByGatewayOrderId("order_unknown")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.handleWebhook(signature, payload))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Payment not found");
        }

        @Test
        @DisplayName("should update order status to PAID on payment success")
        void shouldUpdateOrderStatusToPaidOnPaymentSuccess() {
            // Arrange
            String signature = "valid_signature";
            Map<String, Object> entity = new HashMap<>();
            entity.put("id", "pay_razorpay456");
            entity.put("order_id", "order_razorpay123");

            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("entity", entity);

            Map<String, Object> payloadData = new HashMap<>();
            payloadData.put("payment", paymentData);

            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "payment.captured");
            payload.put("payload", payloadData);

            Payment existingPayment = Payment.builder()
                    .id(paymentId)
                    .orderId(orderId)
                    .gateway(Payment.PaymentGateway.RAZORPAY)
                    .gatewayOrderId("order_razorpay123")
                    .amount(new BigDecimal("1500.00"))
                    .status(Payment.PaymentStatus.CREATED)
                    .build();

            order.setStatus(Order.OrderStatus.PAYMENT_PENDING);

            when(razorpayService.verifyWebhookSignature(anyString(), eq(signature))).thenReturn(true);
            when(paymentRepository.existsByGatewayPaymentId("pay_razorpay456")).thenReturn(false);
            when(paymentRepository.findByGatewayOrderId("order_razorpay123")).thenReturn(Optional.of(existingPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(existingPayment);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            // Act
            paymentService.handleWebhook(signature, payload);

            // Assert
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getStatus()).isEqualTo(Order.OrderStatus.PAID);
        }

        @Test
        @DisplayName("should call ledger service on payment success")
        void shouldCallLedgerServiceOnPaymentSuccess() {
            // Arrange
            String signature = "valid_signature";
            Map<String, Object> entity = new HashMap<>();
            entity.put("id", "pay_razorpay456");
            entity.put("order_id", "order_razorpay123");

            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("entity", entity);

            Map<String, Object> payloadData = new HashMap<>();
            payloadData.put("payment", paymentData);

            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "payment.captured");
            payload.put("payload", payloadData);

            Payment existingPayment = Payment.builder()
                    .id(paymentId)
                    .orderId(orderId)
                    .gateway(Payment.PaymentGateway.RAZORPAY)
                    .gatewayOrderId("order_razorpay123")
                    .amount(new BigDecimal("1500.00"))
                    .status(Payment.PaymentStatus.CREATED)
                    .build();

            order.setStatus(Order.OrderStatus.PAYMENT_PENDING);

            when(razorpayService.verifyWebhookSignature(anyString(), eq(signature))).thenReturn(true);
            when(paymentRepository.existsByGatewayPaymentId("pay_razorpay456")).thenReturn(false);
            when(paymentRepository.findByGatewayOrderId("order_razorpay123")).thenReturn(Optional.of(existingPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(existingPayment);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            // Act
            paymentService.handleWebhook(signature, payload);

            // Assert
            verify(ledgerService).recordPaymentReceived(
                    eq(orderId),
                    eq(paymentId),
                    eq(new BigDecimal("1500.00")),
                    eq(buyerId)
            );
        }

        @Test
        @DisplayName("should not fail payment if ledger service throws exception")
        void shouldNotFailPaymentIfLedgerServiceThrowsException() {
            // Arrange
            String signature = "valid_signature";
            Map<String, Object> entity = new HashMap<>();
            entity.put("id", "pay_razorpay456");
            entity.put("order_id", "order_razorpay123");

            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("entity", entity);

            Map<String, Object> payloadData = new HashMap<>();
            payloadData.put("payment", paymentData);

            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "payment.captured");
            payload.put("payload", payloadData);

            Payment existingPayment = Payment.builder()
                    .id(paymentId)
                    .orderId(orderId)
                    .gateway(Payment.PaymentGateway.RAZORPAY)
                    .gatewayOrderId("order_razorpay123")
                    .amount(new BigDecimal("1500.00"))
                    .status(Payment.PaymentStatus.CREATED)
                    .build();

            order.setStatus(Order.OrderStatus.PAYMENT_PENDING);

            when(razorpayService.verifyWebhookSignature(anyString(), eq(signature))).thenReturn(true);
            when(paymentRepository.existsByGatewayPaymentId("pay_razorpay456")).thenReturn(false);
            when(paymentRepository.findByGatewayOrderId("order_razorpay123")).thenReturn(Optional.of(existingPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(existingPayment);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            doThrow(new RuntimeException("Ledger error")).when(ledgerService).recordPaymentReceived(any(), any(), any(), any());

            // Act - should not throw
            paymentService.handleWebhook(signature, payload);

            // Assert - payment was still saved
            verify(paymentRepository).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("initiateRefund")
    class InitiateRefundTests {

        @Test
        @DisplayName("should initiate refund successfully")
        void shouldInitiateRefundSuccessfully() {
            // Arrange
            Payment successfulPayment = Payment.builder()
                    .id(paymentId)
                    .orderId(orderId)
                    .gateway(Payment.PaymentGateway.RAZORPAY)
                    .gatewayOrderId("order_razorpay123")
                    .gatewayPaymentId("pay_razorpay456")
                    .amount(new BigDecimal("1500.00"))
                    .status(Payment.PaymentStatus.SUCCESS)
                    .paidAt(Instant.now())
                    .build();

            Map<String, Object> refundResponse = Map.of(
                    "id", "rfnd_123456",
                    "status", "processed"
            );

            when(paymentRepository.findSuccessfulPaymentForOrder(orderId)).thenReturn(Optional.of(successfulPayment));
            when(razorpayService.createRefund(anyString(), any(BigDecimal.class), anyString(), any()))
                    .thenReturn(refundResponse);
            when(paymentRepository.save(any(Payment.class))).thenReturn(successfulPayment);

            // Act
            paymentService.initiateRefund(orderId, "Customer request");

            // Assert
            verify(paymentRepository).save(paymentCaptor.capture());
            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getRefundId()).isEqualTo("rfnd_123456");
            assertThat(savedPayment.getRefundStatus()).isEqualTo("processed");
            assertThat(savedPayment.getRefundAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(savedPayment.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        }

        @Test
        @DisplayName("should throw exception when no successful payment found")
        void shouldThrowExceptionWhenNoSuccessfulPaymentFound() {
            // Arrange
            when(paymentRepository.findSuccessfulPaymentForOrder(orderId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.initiateRefund(orderId, "Customer request"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Payment not found");
        }

        @Test
        @DisplayName("should throw exception when payment cannot be refunded")
        void shouldThrowExceptionWhenPaymentCannotBeRefunded() {
            // Arrange
            Payment alreadyRefundedPayment = Payment.builder()
                    .id(paymentId)
                    .orderId(orderId)
                    .gateway(Payment.PaymentGateway.RAZORPAY)
                    .gatewayPaymentId("pay_razorpay456")
                    .amount(new BigDecimal("1500.00"))
                    .status(Payment.PaymentStatus.SUCCESS)
                    .refundId("rfnd_existing")  // Already has refund
                    .paidAt(Instant.now())
                    .build();

            when(paymentRepository.findSuccessfulPaymentForOrder(orderId)).thenReturn(Optional.of(alreadyRefundedPayment));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.initiateRefund(orderId, "Customer request"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Payment cannot be refunded");
        }

        @Test
        @DisplayName("should handle null reason for refund")
        void shouldHandleNullReasonForRefund() {
            // Arrange
            Payment successfulPayment = Payment.builder()
                    .id(paymentId)
                    .orderId(orderId)
                    .gateway(Payment.PaymentGateway.RAZORPAY)
                    .gatewayOrderId("order_razorpay123")
                    .gatewayPaymentId("pay_razorpay456")
                    .amount(new BigDecimal("1500.00"))
                    .status(Payment.PaymentStatus.SUCCESS)
                    .paidAt(Instant.now())
                    .build();

            Map<String, Object> refundResponse = Map.of(
                    "id", "rfnd_123456",
                    "status", "processed"
            );

            when(paymentRepository.findSuccessfulPaymentForOrder(orderId)).thenReturn(Optional.of(successfulPayment));
            when(razorpayService.createRefund(anyString(), any(BigDecimal.class), isNull(), any()))
                    .thenReturn(refundResponse);
            when(paymentRepository.save(any(Payment.class))).thenReturn(successfulPayment);

            // Act
            paymentService.initiateRefund(orderId, null);

            // Assert
            verify(razorpayService).createRefund(
                    eq("pay_razorpay456"),
                    eq(new BigDecimal("1500.00")),
                    isNull(),
                    any()
            );
        }
    }

    @Nested
    @DisplayName("getPayment")
    class GetPaymentTests {

        @Test
        @DisplayName("should return payment dto successfully")
        void shouldReturnPaymentDtoSuccessfully() {
            // Arrange
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            // Act
            PaymentDto result = paymentService.getPayment(paymentId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(paymentId);
            assertThat(result.getOrderId()).isEqualTo(orderId);
            assertThat(result.getGateway()).isEqualTo(Payment.PaymentGateway.RAZORPAY);
            assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.SUCCESS);
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        }

        @Test
        @DisplayName("should throw exception when payment not found")
        void shouldThrowExceptionWhenPaymentNotFound() {
            // Arrange
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.getPayment(paymentId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Payment not found");
        }
    }
}
