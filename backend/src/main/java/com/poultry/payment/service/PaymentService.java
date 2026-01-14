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
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderStateMachine orderStateMachine;
    private final LedgerService ledgerService;
    private final NotificationService notificationService;

    @Value("${external.razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${external.razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${external.razorpay.webhook-secret:}")
    private String razorpayWebhookSecret;

    @Transactional
    @CircuitBreaker(name = "razorpay", fallbackMethod = "createPaymentFallback")
    @Retry(name = "razorpay")
    public PaymentDto createPayment(UUID buyerId, CreatePaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> BusinessException.notFound("Order", request.getOrderId()));

        // Verify buyer owns the order
        if (!order.getBuyerId().equals(buyerId)) {
            throw BusinessException.forbidden("You don't have access to this order");
        }

        // Verify order is in correct state
        if (!orderStateMachine.requiresPayment(order.getStatus())) {
            throw BusinessException.invalidState("Order is not in a state that requires payment");
        }

        // Check for duplicate successful payment
        if (paymentRepository.countSuccessfulPaymentsForOrder(order.getId()) > 0) {
            throw new BusinessException(
                    "Payment already completed for this order",
                    "DUPLICATE_PAYMENT",
                    HttpStatus.CONFLICT
            );
        }

        // Create Razorpay order (in real implementation, call Razorpay API)
        String razorpayOrderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        // Create payment record
        Payment payment = Payment.builder()
                .orderId(order.getId())
                .gateway(Payment.PaymentGateway.RAZORPAY)
                .gatewayOrderId(razorpayOrderId)
                .amount(order.getTotalAmount())
                .status(Payment.PaymentStatus.CREATED)
                .build();

        payment = paymentRepository.save(payment);

        // Update order status
        if (order.getStatus() == Order.OrderStatus.SELLER_CONFIRMED) {
            order.setStatus(Order.OrderStatus.PAYMENT_PENDING);
            orderRepository.save(order);
        }

        log.info("Payment created: {} for order: {}", payment.getId(), order.getId());

        return mapToDto(payment, true);
    }

    public PaymentDto createPaymentFallback(UUID buyerId, CreatePaymentRequest request, Exception e) {
        log.error("Payment gateway unavailable, using fallback: {}", e.getMessage());
        throw new BusinessException(
                "Payment service temporarily unavailable. Please try again.",
                "PAYMENT_SERVICE_UNAVAILABLE",
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    @Transactional
    public void handleWebhook(String signature, Map<String, Object> payload) {
        // Verify signature
        if (!verifyWebhookSignature(payload.toString(), signature)) {
            log.warn("Invalid webhook signature");
            throw new BusinessException("Invalid signature", "INVALID_SIGNATURE", HttpStatus.UNAUTHORIZED);
        }

        String event = (String) payload.get("event");
        Map<String, Object> payloadData = (Map<String, Object>) payload.get("payload");
        Map<String, Object> paymentData = (Map<String, Object>) payloadData.get("payment");
        Map<String, Object> entity = (Map<String, Object>) paymentData.get("entity");

        String paymentId = (String) entity.get("id");
        String orderId = (String) entity.get("order_id");

        // Check for duplicate webhook
        if (paymentRepository.existsByGatewayPaymentId(paymentId)) {
            log.info("Duplicate webhook received for payment: {}", paymentId);
            return;
        }

        Payment payment = paymentRepository.findByGatewayOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("Payment not found for gateway order: {}", orderId);
                    return BusinessException.notFound("Payment", orderId);
                });

        payment.setGatewayPaymentId(paymentId);
        payment.setWebhookPayload(payload);
        payment.setWebhookReceivedAt(Instant.now());
        payment.setSignatureVerified(true);

        switch (event) {
            case "payment.captured" -> handlePaymentSuccess(payment, entity);
            case "payment.failed" -> handlePaymentFailure(payment, entity);
            default -> log.info("Unhandled webhook event: {}", event);
        }
    }

    private void handlePaymentSuccess(Payment payment, Map<String, Object> entity) {
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);

        // Update order status
        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> BusinessException.notFound("Order", payment.getOrderId()));

        if (order.getStatus() == Order.OrderStatus.PAYMENT_PENDING
                || order.getStatus() == Order.OrderStatus.PAYMENT_FAILED) {
            order.setStatus(Order.OrderStatus.PAID);
            orderRepository.save(order);
        }

        log.info("Payment successful: {} for order: {}", payment.getId(), payment.getOrderId());

        // Record ledger entry for payment received
        try {
            ledgerService.recordPaymentReceived(
                    order.getId(),
                    payment.getId(),
                    payment.getAmount(),
                    order.getBuyerId()
            );
            log.info("Ledger entry created for payment: {}", payment.getId());
        } catch (Exception e) {
            log.error("Failed to create ledger entry for payment: {}", payment.getId(), e);
            // Don't fail the payment - ledger can be reconciled later
        }

        // Send notifications to buyer and seller
        try {
            notificationService.notifyPaymentSuccess(order, payment.getId(), payment.getAmount());
            notificationService.notifySellerOrderPaid(order, payment.getId(), payment.getAmount());
        } catch (Exception e) {
            log.error("Failed to send payment notifications for order: {}", order.getId(), e);
            // Don't fail the payment - notifications are non-critical
        }
    }

    private void handlePaymentFailure(Payment payment, Map<String, Object> entity) {
        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setErrorCode((String) entity.get("error_code"));
        payment.setErrorDescription((String) entity.get("error_description"));
        paymentRepository.save(payment);

        // Update order status
        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> BusinessException.notFound("Order", payment.getOrderId()));

        if (order.getStatus() == Order.OrderStatus.PAYMENT_PENDING) {
            order.setStatus(Order.OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
        }

        log.info("Payment failed: {} for order: {}", payment.getId(), payment.getOrderId());
    }

    @Transactional
    @CircuitBreaker(name = "razorpay")
    @Retry(name = "razorpay")
    public void initiateRefund(UUID orderId, String reason) {
        Payment payment = paymentRepository.findSuccessfulPaymentForOrder(orderId)
                .orElseThrow(() -> BusinessException.notFound("Payment", orderId));

        if (!payment.canBeRefunded()) {
            throw BusinessException.invalidState("Payment cannot be refunded");
        }

        // In real implementation, call Razorpay refund API
        String refundId = "rfnd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        payment.setRefundId(refundId);
        payment.setRefundAmount(payment.getAmount());
        payment.setRefundStatus("processed");
        payment.setRefundedAt(Instant.now());
        payment.setStatus(Payment.PaymentStatus.REFUNDED);

        paymentRepository.save(payment);

        log.info("Refund initiated: {} for payment: {}", refundId, payment.getId());
    }

    @Transactional(readOnly = true)
    public PaymentDto getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> BusinessException.notFound("Payment", paymentId));
        return mapToDto(payment, false);
    }

    private boolean verifyWebhookSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    razorpayWebhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return computed.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }

    private PaymentDto mapToDto(Payment payment, boolean includeCheckoutDetails) {
        PaymentDto.PaymentDtoBuilder builder = PaymentDto.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .gateway(payment.getGateway())
                .gatewayOrderId(payment.getGatewayOrderId())
                .gatewayPaymentId(payment.getGatewayPaymentId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .errorCode(payment.getErrorCode())
                .errorDescription(payment.getErrorDescription())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt());

        if (includeCheckoutDetails && payment.getGateway() == Payment.PaymentGateway.RAZORPAY) {
            builder.razorpayKey(razorpayKeyId)
                    .razorpayOrderId(payment.getGatewayOrderId());
        }

        return builder.build();
    }
}
