package com.poultry.payment.dto;

import com.poultry.payment.entity.Payment;
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
public class PaymentDto {

    private UUID id;
    private UUID orderId;
    private Payment.PaymentGateway gateway;
    private String gatewayOrderId;
    private String gatewayPaymentId;
    private BigDecimal amount;
    private String currency;
    private Payment.PaymentStatus status;
    private String errorCode;
    private String errorDescription;
    private Instant paidAt;
    private Instant createdAt;

    // For Razorpay checkout
    private String razorpayKey;
    private String razorpayOrderId;
}
