package com.poultry.payment.service;

import com.poultry.common.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayService {

    private final RestTemplate restTemplate;

    @Value("${external.razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${external.razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${external.razorpay.webhook-secret:}")
    private String razorpayWebhookSecret;

    @Value("${external.razorpay.enabled:false}")
    private boolean razorpayEnabled;

    private static final String RAZORPAY_API_BASE = "https://api.razorpay.com/v1";

    /**
     * Create a Razorpay order for payment collection
     */
    @CircuitBreaker(name = "razorpay", fallbackMethod = "createOrderFallback")
    @Retry(name = "razorpay")
    public Map<String, Object> createOrder(BigDecimal amount, String currency, String receipt, Map<String, String> notes) {
        if (!razorpayEnabled || razorpayKeyId.isEmpty()) {
            log.warn("[RAZORPAY NOT CONFIGURED] Would create order for amount: {} {}", amount, currency);
            // Return mock order for testing
            return Map.of(
                    "id", "order_" + System.currentTimeMillis(),
                    "amount", amount.multiply(BigDecimal.valueOf(100)).intValue(),
                    "currency", currency,
                    "receipt", receipt,
                    "status", "created"
            );
        }

        String url = RAZORPAY_API_BASE + "/orders";

        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue()); // Amount in paise
        payload.put("currency", currency);
        payload.put("receipt", receipt);
        if (notes != null && !notes.isEmpty()) {
            payload.put("notes", notes);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Razorpay order created: {}", response.getBody().get("id"));
                return response.getBody();
            } else {
                log.error("Razorpay order creation failed: {}", response.getBody());
                throw new RuntimeException("Failed to create Razorpay order");
            }
        } catch (Exception e) {
            log.error("Error creating Razorpay order: {}", e.getMessage());
            throw e;
        }
    }

    public Map<String, Object> createOrderFallback(BigDecimal amount, String currency, String receipt,
                                                    Map<String, String> notes, Exception e) {
        log.error("Razorpay order creation failed, using fallback: {}", e.getMessage());
        throw new BusinessException("Payment service temporarily unavailable", "PAYMENT_SERVICE_UNAVAILABLE",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * Verify payment signature
     */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        if (razorpayKeySecret.isEmpty()) {
            log.warn("[RAZORPAY NOT CONFIGURED] Skipping signature verification");
            return true;
        }

        try {
            String payload = orderId + "|" + paymentId;
            String computed = computeHmacSha256(payload, razorpayKeySecret);
            return computed.equals(signature);
        } catch (Exception e) {
            log.error("Payment signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verify webhook signature
     */
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (razorpayWebhookSecret.isEmpty()) {
            log.warn("[RAZORPAY NOT CONFIGURED] Skipping webhook signature verification");
            return true;
        }

        try {
            String computed = computeHmacSha256(payload, razorpayWebhookSecret);
            return computed.equals(signature);
        } catch (Exception e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Fetch payment details
     */
    @CircuitBreaker(name = "razorpay")
    @Retry(name = "razorpay")
    public Map<String, Object> fetchPayment(String paymentId) {
        if (!razorpayEnabled || razorpayKeyId.isEmpty()) {
            log.warn("[RAZORPAY NOT CONFIGURED] Would fetch payment: {}", paymentId);
            return Map.of("id", paymentId, "status", "captured");
        }

        String url = RAZORPAY_API_BASE + "/payments/" + paymentId;

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching Razorpay payment: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Create a refund for a payment
     */
    @CircuitBreaker(name = "razorpay", fallbackMethod = "createRefundFallback")
    @Retry(name = "razorpay")
    public Map<String, Object> createRefund(String paymentId, BigDecimal amount, String reason, Map<String, String> notes) {
        if (!razorpayEnabled || razorpayKeyId.isEmpty()) {
            log.warn("[RAZORPAY NOT CONFIGURED] Would create refund for payment: {}, amount: {}", paymentId, amount);
            return Map.of(
                    "id", "rfnd_" + System.currentTimeMillis(),
                    "payment_id", paymentId,
                    "amount", amount.multiply(BigDecimal.valueOf(100)).intValue(),
                    "status", "processed"
            );
        }

        String url = RAZORPAY_API_BASE + "/payments/" + paymentId + "/refund";

        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue()); // Amount in paise
        if (reason != null) {
            payload.put("speed", "normal");
            payload.put("notes", notes != null ? notes : Map.of("reason", reason));
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Razorpay refund created: {} for payment: {}", response.getBody().get("id"), paymentId);
                return response.getBody();
            } else {
                log.error("Razorpay refund creation failed: {}", response.getBody());
                throw new RuntimeException("Failed to create refund");
            }
        } catch (Exception e) {
            log.error("Error creating Razorpay refund: {}", e.getMessage());
            throw e;
        }
    }

    public Map<String, Object> createRefundFallback(String paymentId, BigDecimal amount, String reason,
                                                     Map<String, String> notes, Exception e) {
        log.error("Razorpay refund creation failed: {}", e.getMessage());
        throw new BusinessException("Refund service temporarily unavailable", "REFUND_SERVICE_UNAVAILABLE",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * Create a payout to a bank account (for seller settlements)
     */
    @CircuitBreaker(name = "razorpay", fallbackMethod = "createPayoutFallback")
    @Retry(name = "razorpay")
    public Map<String, Object> createPayout(String fundAccountId, BigDecimal amount, String currency,
                                             String mode, String purpose, String referenceId, String narration) {
        if (!razorpayEnabled || razorpayKeyId.isEmpty()) {
            log.warn("[RAZORPAY NOT CONFIGURED] Would create payout to fund account: {}, amount: {}", fundAccountId, amount);
            return Map.of(
                    "id", "pout_" + System.currentTimeMillis(),
                    "fund_account_id", fundAccountId,
                    "amount", amount.multiply(BigDecimal.valueOf(100)).intValue(),
                    "status", "processing",
                    "utr", "UTR" + System.currentTimeMillis()
            );
        }

        String url = RAZORPAY_API_BASE + "/payouts";

        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("account_number", getPayoutAccountNumber()); // Your Razorpay X account number
        payload.put("fund_account_id", fundAccountId);
        payload.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue());
        payload.put("currency", currency);
        payload.put("mode", mode); // NEFT, RTGS, IMPS, UPI
        payload.put("purpose", purpose); // payout, salary, refund, cashback, etc.
        payload.put("queue_if_low_balance", true);
        payload.put("reference_id", referenceId);
        payload.put("narration", narration);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Razorpay payout created: {}", response.getBody().get("id"));
                return response.getBody();
            } else {
                log.error("Razorpay payout creation failed: {}", response.getBody());
                throw new RuntimeException("Failed to create payout");
            }
        } catch (Exception e) {
            log.error("Error creating Razorpay payout: {}", e.getMessage());
            throw e;
        }
    }

    public Map<String, Object> createPayoutFallback(String fundAccountId, BigDecimal amount, String currency,
                                                     String mode, String purpose, String referenceId,
                                                     String narration, Exception e) {
        log.error("Razorpay payout creation failed: {}", e.getMessage());
        throw new BusinessException("Payout service temporarily unavailable", "PAYOUT_SERVICE_UNAVAILABLE",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * Create a fund account for a seller (to receive payouts)
     */
    @CircuitBreaker(name = "razorpay")
    @Retry(name = "razorpay")
    public Map<String, Object> createFundAccount(String contactId, String accountNumber, String ifscCode,
                                                  String beneficiaryName) {
        if (!razorpayEnabled || razorpayKeyId.isEmpty()) {
            log.warn("[RAZORPAY NOT CONFIGURED] Would create fund account for contact: {}", contactId);
            return Map.of(
                    "id", "fa_" + System.currentTimeMillis(),
                    "contact_id", contactId,
                    "account_type", "bank_account"
            );
        }

        String url = RAZORPAY_API_BASE + "/fund_accounts";

        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> bankAccount = new HashMap<>();
        bankAccount.put("name", beneficiaryName);
        bankAccount.put("ifsc", ifscCode);
        bankAccount.put("account_number", accountNumber);

        Map<String, Object> payload = new HashMap<>();
        payload.put("contact_id", contactId);
        payload.put("account_type", "bank_account");
        payload.put("bank_account", bankAccount);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Razorpay fund account created: {}", response.getBody().get("id"));
                return response.getBody();
            } else {
                log.error("Razorpay fund account creation failed: {}", response.getBody());
                throw new RuntimeException("Failed to create fund account");
            }
        } catch (Exception e) {
            log.error("Error creating Razorpay fund account: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Create a contact for a seller (required before creating fund account)
     */
    @CircuitBreaker(name = "razorpay")
    @Retry(name = "razorpay")
    public Map<String, Object> createContact(String name, String email, String phone, String type, String referenceId) {
        if (!razorpayEnabled || razorpayKeyId.isEmpty()) {
            log.warn("[RAZORPAY NOT CONFIGURED] Would create contact: {}", name);
            return Map.of(
                    "id", "cont_" + System.currentTimeMillis(),
                    "name", name,
                    "type", type
            );
        }

        String url = RAZORPAY_API_BASE + "/contacts";

        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("email", email);
        payload.put("contact", phone);
        payload.put("type", type); // vendor, customer, employee, self
        payload.put("reference_id", referenceId);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Razorpay contact created: {}", response.getBody().get("id"));
                return response.getBody();
            } else {
                log.error("Razorpay contact creation failed: {}", response.getBody());
                throw new RuntimeException("Failed to create contact");
            }
        } catch (Exception e) {
            log.error("Error creating Razorpay contact: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Fetch payout status
     */
    @CircuitBreaker(name = "razorpay")
    public Map<String, Object> fetchPayout(String payoutId) {
        if (!razorpayEnabled || razorpayKeyId.isEmpty()) {
            return Map.of("id", payoutId, "status", "processed");
        }

        String url = RAZORPAY_API_BASE + "/payouts/" + payoutId;

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching Razorpay payout: {}", e.getMessage());
            throw e;
        }
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(razorpayKeyId, razorpayKeySecret);
        return headers;
    }

    private String computeHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    @Value("${external.razorpay.account-number:}")
    private String payoutAccountNumber;

    private String getPayoutAccountNumber() {
        return payoutAccountNumber;
    }

    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }
}
