package com.poultry.payment.controller;

import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
import com.poultry.payment.dto.CreatePaymentRequest;
import com.poultry.payment.dto.PaymentDto;
import com.poultry.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing APIs")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/buyer/payments")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Create payment", description = "Create a payment order for checkout")
    public ResponseEntity<ApiResponse<PaymentDto>> createPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePaymentRequest request) {

        PaymentDto payment = paymentService.createPayment(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(payment, "Payment order created"));
    }

    @GetMapping("/buyer/payments/{paymentId}")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Get payment status", description = "Get payment status and details")
    public ResponseEntity<ApiResponse<PaymentDto>> getPayment(
            @PathVariable UUID paymentId) {

        PaymentDto payment = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @PostMapping("/webhooks/razorpay")
    @Operation(summary = "Razorpay webhook", description = "Handle Razorpay payment webhooks")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestHeader("X-Razorpay-Signature") String signature,
            @RequestBody Map<String, Object> payload) {

        log.info("Received Razorpay webhook: {}", payload.get("event"));
        paymentService.handleWebhook(signature, payload);
        return ResponseEntity.ok("OK");
    }
}
