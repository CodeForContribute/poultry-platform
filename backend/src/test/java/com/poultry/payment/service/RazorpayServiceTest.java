package com.poultry.payment.service;

import com.poultry.common.exception.BusinessException;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RazorpayService Unit Tests")
class RazorpayServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private RazorpayService razorpayService;

    @Captor
    private ArgumentCaptor<HttpEntity<Map<String, Object>>> requestCaptor;

    private static final String RAZORPAY_API_BASE = "https://api.razorpay.com/v1";

    @BeforeEach
    void setUp() {
        // Set up service configuration through reflection
        ReflectionTestUtils.setField(razorpayService, "razorpayEnabled", true);
        ReflectionTestUtils.setField(razorpayService, "razorpayKeyId", "rzp_test_key123");
        ReflectionTestUtils.setField(razorpayService, "razorpayKeySecret", "test_secret_456");
        ReflectionTestUtils.setField(razorpayService, "razorpayWebhookSecret", "webhook_secret_789");
        ReflectionTestUtils.setField(razorpayService, "payoutAccountNumber", "1234567890");
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("should create order successfully when razorpay is enabled")
        void shouldCreateOrderSuccessfully() {
            // Arrange
            BigDecimal amount = new BigDecimal("1500.00");
            String currency = "INR";
            String receipt = "ORD-123456";
            Map<String, String> notes = Map.of("order_id", "test-uuid");

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("id", "order_razorpay123");
            responseBody.put("amount", 150000);
            responseBody.put("currency", "INR");
            responseBody.put("status", "created");

            ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

            when(restTemplate.exchange(
                    eq(RAZORPAY_API_BASE + "/orders"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(response);

            // Act
            Map<String, Object> result = razorpayService.createOrder(amount, currency, receipt, notes);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.get("id")).isEqualTo("order_razorpay123");
            assertThat(result.get("amount")).isEqualTo(150000);
            assertThat(result.get("currency")).isEqualTo("INR");

            verify(restTemplate).exchange(
                    eq(RAZORPAY_API_BASE + "/orders"),
                    eq(HttpMethod.POST),
                    requestCaptor.capture(),
                    eq(Map.class)
            );

            HttpEntity<Map<String, Object>> capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getBody()).containsEntry("amount", 150000);
            assertThat(capturedRequest.getBody()).containsEntry("currency", "INR");
            assertThat(capturedRequest.getBody()).containsEntry("receipt", "ORD-123456");
        }

        @Test
        @DisplayName("should return mock order when razorpay is not configured")
        void shouldReturnMockOrderWhenNotConfigured() {
            // Arrange
            ReflectionTestUtils.setField(razorpayService, "razorpayEnabled", false);

            BigDecimal amount = new BigDecimal("1500.00");
            String currency = "INR";
            String receipt = "ORD-123456";

            // Act
            Map<String, Object> result = razorpayService.createOrder(amount, currency, receipt, null);

            // Assert
            assertThat(result).isNotNull();
            assertThat((String) result.get("id")).startsWith("order_");
            assertThat(result.get("amount")).isEqualTo(150000);
            assertThat(result.get("currency")).isEqualTo("INR");
            assertThat(result.get("status")).isEqualTo("created");

            // RestTemplate should not be called
            verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(), eq(Map.class));
        }

        @Test
        @DisplayName("should return mock order when razorpay key is empty")
        void shouldReturnMockOrderWhenKeyEmpty() {
            // Arrange
            ReflectionTestUtils.setField(razorpayService, "razorpayKeyId", "");

            BigDecimal amount = new BigDecimal("1500.00");
            String currency = "INR";
            String receipt = "ORD-123456";

            // Act
            Map<String, Object> result = razorpayService.createOrder(amount, currency, receipt, null);

            // Assert
            assertThat(result).isNotNull();
            assertThat((String) result.get("id")).startsWith("order_");

            verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(), eq(Map.class));
        }

        @Test
        @DisplayName("should handle null notes")
        void shouldHandleNullNotes() {
            // Arrange
            BigDecimal amount = new BigDecimal("1500.00");
            String currency = "INR";
            String receipt = "ORD-123456";

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("id", "order_razorpay123");
            responseBody.put("amount", 150000);
            responseBody.put("currency", "INR");
            responseBody.put("status", "created");

            ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

            when(restTemplate.exchange(
                    eq(RAZORPAY_API_BASE + "/orders"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(response);

            // Act
            Map<String, Object> result = razorpayService.createOrder(amount, currency, receipt, null);

            // Assert
            assertThat(result).isNotNull();

            verify(restTemplate).exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    requestCaptor.capture(),
                    eq(Map.class)
            );

            HttpEntity<Map<String, Object>> capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getBody()).doesNotContainKey("notes");
        }

        @Test
        @DisplayName("should throw exception when API returns non-2xx status")
        void shouldThrowExceptionWhenApiReturnsNon2xxStatus() {
            // Arrange
            BigDecimal amount = new BigDecimal("1500.00");
            String currency = "INR";
            String receipt = "ORD-123456";

            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("error", "Invalid amount");

            ResponseEntity<Map> response = new ResponseEntity<>(errorBody, HttpStatus.BAD_REQUEST);

            when(restTemplate.exchange(
                    eq(RAZORPAY_API_BASE + "/orders"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(response);

            // Act & Assert
            assertThatThrownBy(() -> razorpayService.createOrder(amount, currency, receipt, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to create Razorpay order");
        }

        @Test
        @DisplayName("should propagate exception when REST call fails")
        void shouldPropagateExceptionWhenRestCallFails() {
            // Arrange
            BigDecimal amount = new BigDecimal("1500.00");
            String currency = "INR";
            String receipt = "ORD-123456";

            when(restTemplate.exchange(
                    eq(RAZORPAY_API_BASE + "/orders"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenThrow(new RestClientException("Connection timeout"));

            // Act & Assert
            assertThatThrownBy(() -> razorpayService.createOrder(amount, currency, receipt, null))
                    .isInstanceOf(RestClientException.class)
                    .hasMessageContaining("Connection timeout");
        }

        @Test
        @DisplayName("should convert amount to paise correctly")
        void shouldConvertAmountToPaiseCorrectly() {
            // Arrange
            BigDecimal amount = new BigDecimal("99.99");
            String currency = "INR";
            String receipt = "ORD-123456";

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("id", "order_razorpay123");
            responseBody.put("amount", 9999);
            responseBody.put("currency", "INR");

            ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

            when(restTemplate.exchange(
                    eq(RAZORPAY_API_BASE + "/orders"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(response);

            // Act
            razorpayService.createOrder(amount, currency, receipt, null);

            // Assert
            verify(restTemplate).exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    requestCaptor.capture(),
                    eq(Map.class)
            );

            HttpEntity<Map<String, Object>> capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getBody().get("amount")).isEqualTo(9999);
        }
    }

    @Nested
    @DisplayName("createOrderFallback")
    class CreateOrderFallbackTests {

        @Test
        @DisplayName("should throw SERVICE_UNAVAILABLE exception in fallback")
        void shouldThrowServiceUnavailableInFallback() {
            // Act & Assert
            assertThatThrownBy(() -> razorpayService.createOrderFallback(
                    new BigDecimal("1500.00"),
                    "INR",
                    "ORD-123",
                    null,
                    new RuntimeException("Gateway error")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Payment service temporarily unavailable")
                    .extracting("errorCode")
                    .isEqualTo("PAYMENT_SERVICE_UNAVAILABLE");
        }
    }

    @Nested
    @DisplayName("verifyPaymentSignature")
    class VerifyPaymentSignatureTests {

        @Test
        @DisplayName("should return true for valid signature")
        void shouldReturnTrueForValidSignature() {
            // Arrange
            String orderId = "order_123";
            String paymentId = "pay_456";
            // This is a pre-computed HMAC-SHA256 hash
            // For "order_123|pay_456" with secret "test_secret_456"
            String validSignature = computeExpectedSignature(orderId + "|" + paymentId);

            // Act
            boolean result = razorpayService.verifyPaymentSignature(orderId, paymentId, validSignature);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for invalid signature")
        void shouldReturnFalseForInvalidSignature() {
            // Arrange
            String orderId = "order_123";
            String paymentId = "pay_456";
            String invalidSignature = "invalid_signature_here";

            // Act
            boolean result = razorpayService.verifyPaymentSignature(orderId, paymentId, invalidSignature);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return true when secret is empty")
        void shouldReturnTrueWhenSecretEmpty() {
            // Arrange
            ReflectionTestUtils.setField(razorpayService, "razorpayKeySecret", "");

            String orderId = "order_123";
            String paymentId = "pay_456";
            String anySignature = "any_signature";

            // Act
            boolean result = razorpayService.verifyPaymentSignature(orderId, paymentId, anySignature);

            // Assert
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("verifyWebhookSignature")
    class VerifyWebhookSignatureTests {

        @Test
        @DisplayName("should return true for valid webhook signature")
        void shouldReturnTrueForValidWebhookSignature() {
            // Arrange
            String payload = "{\"event\":\"payment.captured\"}";
            String validSignature = computeExpectedWebhookSignature(payload);

            // Act
            boolean result = razorpayService.verifyWebhookSignature(payload, validSignature);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for invalid webhook signature")
        void shouldReturnFalseForInvalidWebhookSignature() {
            // Arrange
            String payload = "{\"event\":\"payment.captured\"}";
            String invalidSignature = "invalid_webhook_signature";

            // Act
            boolean result = razorpayService.verifyWebhookSignature(payload, invalidSignature);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return true when webhook secret is empty")
        void shouldReturnTrueWhenWebhookSecretEmpty() {
            // Arrange
            ReflectionTestUtils.setField(razorpayService, "razorpayWebhookSecret", "");

            String payload = "{\"event\":\"payment.captured\"}";
            String anySignature = "any_signature";

            // Act
            boolean result = razorpayService.verifyWebhookSignature(payload, anySignature);

            // Assert
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("fetchPayment")
    class FetchPaymentTests {

        @Test
        @DisplayName("should fetch payment successfully")
        void shouldFetchPaymentSuccessfully() {
            // Arrange
            String paymentId = "pay_razorpay456";

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("id", paymentId);
            responseBody.put("status", "captured");
            responseBody.put("amount", 150000);

            ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

            when(restTemplate.exchange(
                    eq(RAZORPAY_API_BASE + "/payments/" + paymentId),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(response);

            // Act
            Map<String, Object> result = razorpayService.fetchPayment(paymentId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.get("id")).isEqualTo(paymentId);
            assertThat(result.get("status")).isEqualTo("captured");
        }

        @Test
        @DisplayName("should return mock payment when razorpay is not enabled")
        void shouldReturnMockPaymentWhenNotEnabled() {
            // Arrange
            ReflectionTestUtils.setField(razorpayService, "razorpayEnabled", false);
            String paymentId = "pay_razorpay456";

            // Act
            Map<String, Object> result = razorpayService.fetchPayment(paymentId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.get("id")).isEqualTo(paymentId);
            assertThat(result.get("status")).isEqualTo("captured");

            verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(), eq(Map.class));
        }

        @Test
        @DisplayName("should propagate exception when fetch fails")
        void shouldPropagateExceptionWhenFetchFails() {
            // Arrange
            String paymentId = "pay_razorpay456";

            when(restTemplate.exchange(
                    eq(RAZORPAY_API_BASE + "/payments/" + paymentId),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenThrow(new RestClientException("Connection refused"));

            // Act & Assert
            assertThatThrownBy(() -> razorpayService.fetchPayment(paymentId))
                    .isInstanceOf(RestClientException.class)
                    .hasMessageContaining("Connection refused");
        }
    }

    @Nested
    @DisplayName("createRefund")
    class CreateRefundTests {

        @Test
        @DisplayName("should create refund successfully")
        void shouldCreateRefundSuccessfully() {
            // Arrange
            String paymentId = "pay_razorpay456";
            BigDecimal amount = new BigDecimal("1500.00");
            String reason = "Customer request";
            Map<String, String> notes = Map.of("order_id", "test-uuid");

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("id", "rfnd_123456");
            responseBody.put("payment_id", paymentId);
            responseBody.put("amount", 150000);
            responseBody.put("status", "processed");

            ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

            when(restTemplate.exchange(
                    eq(RAZORPAY_API_BASE + "/payments/" + paymentId + "/refund"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(response);

            // Act
            Map<String, Object> result = razorpayService.createRefund(paymentId, amount, reason, notes);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.get("id")).isEqualTo("rfnd_123456");
            assertThat(result.get("status")).isEqualTo("processed");
        }

        @Test
        @DisplayName("should return mock refund when razorpay is not configured")
        void shouldReturnMockRefundWhenNotConfigured() {
            // Arrange
            ReflectionTestUtils.setField(razorpayService, "razorpayEnabled", false);

            String paymentId = "pay_razorpay456";
            BigDecimal amount = new BigDecimal("1500.00");

            // Act
            Map<String, Object> result = razorpayService.createRefund(paymentId, amount, null, null);

            // Assert
            assertThat(result).isNotNull();
            assertThat((String) result.get("id")).startsWith("rfnd_");
            assertThat(result.get("payment_id")).isEqualTo(paymentId);
            assertThat(result.get("status")).isEqualTo("processed");

            verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(), eq(Map.class));
        }

        @Test
        @DisplayName("should throw exception when refund API returns error")
        void shouldThrowExceptionWhenRefundApiFails() {
            // Arrange
            String paymentId = "pay_razorpay456";
            BigDecimal amount = new BigDecimal("1500.00");

            ResponseEntity<Map> response = new ResponseEntity<>(Map.of("error", "Insufficient funds"), HttpStatus.BAD_REQUEST);

            when(restTemplate.exchange(
                    eq(RAZORPAY_API_BASE + "/payments/" + paymentId + "/refund"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(response);

            // Act & Assert
            assertThatThrownBy(() -> razorpayService.createRefund(paymentId, amount, null, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to create refund");
        }
    }

    @Nested
    @DisplayName("createRefundFallback")
    class CreateRefundFallbackTests {

        @Test
        @DisplayName("should throw SERVICE_UNAVAILABLE exception in refund fallback")
        void shouldThrowServiceUnavailableInRefundFallback() {
            // Act & Assert
            assertThatThrownBy(() -> razorpayService.createRefundFallback(
                    "pay_123",
                    new BigDecimal("1500.00"),
                    "Customer request",
                    null,
                    new RuntimeException("Gateway error")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Refund service temporarily unavailable")
                    .extracting("errorCode")
                    .isEqualTo("REFUND_SERVICE_UNAVAILABLE");
        }
    }

    @Nested
    @DisplayName("createPayout")
    class CreatePayoutTests {

        @Test
        @DisplayName("should create payout successfully")
        void shouldCreatePayoutSuccessfully() {
            // Arrange
            String fundAccountId = "fa_123456";
            BigDecimal amount = new BigDecimal("5000.00");
            String currency = "INR";
            String mode = "NEFT";
            String purpose = "payout";
            String referenceId = "settlement_123";
            String narration = "Seller settlement";

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("id", "pout_789");
            responseBody.put("fund_account_id", fundAccountId);
            responseBody.put("amount", 500000);
            responseBody.put("status", "processing");
            responseBody.put("utr", "UTR123456");

            ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

            when(restTemplate.exchange(
                    eq(RAZORPAY_API_BASE + "/payouts"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(response);

            // Act
            Map<String, Object> result = razorpayService.createPayout(
                    fundAccountId, amount, currency, mode, purpose, referenceId, narration);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.get("id")).isEqualTo("pout_789");
            assertThat(result.get("status")).isEqualTo("processing");
        }

        @Test
        @DisplayName("should return mock payout when razorpay is not configured")
        void shouldReturnMockPayoutWhenNotConfigured() {
            // Arrange
            ReflectionTestUtils.setField(razorpayService, "razorpayEnabled", false);

            String fundAccountId = "fa_123456";
            BigDecimal amount = new BigDecimal("5000.00");

            // Act
            Map<String, Object> result = razorpayService.createPayout(
                    fundAccountId, amount, "INR", "NEFT", "payout", "ref_123", "Settlement");

            // Assert
            assertThat(result).isNotNull();
            assertThat((String) result.get("id")).startsWith("pout_");
            assertThat(result.get("fund_account_id")).isEqualTo(fundAccountId);
            assertThat(result.get("status")).isEqualTo("processing");

            verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(), eq(Map.class));
        }
    }

    @Nested
    @DisplayName("createPayoutFallback")
    class CreatePayoutFallbackTests {

        @Test
        @DisplayName("should throw SERVICE_UNAVAILABLE exception in payout fallback")
        void shouldThrowServiceUnavailableInPayoutFallback() {
            // Act & Assert
            assertThatThrownBy(() -> razorpayService.createPayoutFallback(
                    "fa_123",
                    new BigDecimal("5000.00"),
                    "INR",
                    "NEFT",
                    "payout",
                    "ref_123",
                    "Settlement",
                    new RuntimeException("Gateway error")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Payout service temporarily unavailable")
                    .extracting("errorCode")
                    .isEqualTo("PAYOUT_SERVICE_UNAVAILABLE");
        }
    }

    @Nested
    @DisplayName("createFundAccount")
    class CreateFundAccountTests {

        @Test
        @DisplayName("should create fund account successfully")
        void shouldCreateFundAccountSuccessfully() {
            // Arrange
            String contactId = "cont_123";
            String accountNumber = "1234567890";
            String ifscCode = "HDFC0001234";
            String beneficiaryName = "Test Seller";

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("id", "fa_123456");
            responseBody.put("contact_id", contactId);
            responseBody.put("account_type", "bank_account");

            ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

            when(restTemplate.exchange(
                    eq(RAZORPAY_API_BASE + "/fund_accounts"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(response);

            // Act
            Map<String, Object> result = razorpayService.createFundAccount(
                    contactId, accountNumber, ifscCode, beneficiaryName);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.get("id")).isEqualTo("fa_123456");
            assertThat(result.get("contact_id")).isEqualTo(contactId);
        }

        @Test
        @DisplayName("should return mock fund account when razorpay is not configured")
        void shouldReturnMockFundAccountWhenNotConfigured() {
            // Arrange
            ReflectionTestUtils.setField(razorpayService, "razorpayEnabled", false);

            String contactId = "cont_123";

            // Act
            Map<String, Object> result = razorpayService.createFundAccount(
                    contactId, "1234567890", "HDFC0001234", "Test Seller");

            // Assert
            assertThat(result).isNotNull();
            assertThat((String) result.get("id")).startsWith("fa_");
            assertThat(result.get("contact_id")).isEqualTo(contactId);

            verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(), eq(Map.class));
        }
    }

    @Nested
    @DisplayName("createContact")
    class CreateContactTests {

        @Test
        @DisplayName("should create contact successfully")
        void shouldCreateContactSuccessfully() {
            // Arrange
            String name = "Test Seller";
            String email = "seller@test.com";
            String phone = "9876543210";
            String type = "vendor";
            String referenceId = "seller_123";

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("id", "cont_123");
            responseBody.put("name", name);
            responseBody.put("type", type);

            ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

            when(restTemplate.exchange(
                    eq(RAZORPAY_API_BASE + "/contacts"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(response);

            // Act
            Map<String, Object> result = razorpayService.createContact(name, email, phone, type, referenceId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.get("id")).isEqualTo("cont_123");
            assertThat(result.get("name")).isEqualTo(name);
        }

        @Test
        @DisplayName("should return mock contact when razorpay is not configured")
        void shouldReturnMockContactWhenNotConfigured() {
            // Arrange
            ReflectionTestUtils.setField(razorpayService, "razorpayEnabled", false);

            // Act
            Map<String, Object> result = razorpayService.createContact(
                    "Test Seller", "seller@test.com", "9876543210", "vendor", "seller_123");

            // Assert
            assertThat(result).isNotNull();
            assertThat((String) result.get("id")).startsWith("cont_");
            assertThat(result.get("name")).isEqualTo("Test Seller");
            assertThat(result.get("type")).isEqualTo("vendor");

            verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(), eq(Map.class));
        }
    }

    @Nested
    @DisplayName("fetchPayout")
    class FetchPayoutTests {

        @Test
        @DisplayName("should fetch payout successfully")
        void shouldFetchPayoutSuccessfully() {
            // Arrange
            String payoutId = "pout_123456";

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("id", payoutId);
            responseBody.put("status", "processed");
            responseBody.put("utr", "UTR789");

            ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

            when(restTemplate.exchange(
                    eq(RAZORPAY_API_BASE + "/payouts/" + payoutId),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(response);

            // Act
            Map<String, Object> result = razorpayService.fetchPayout(payoutId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.get("id")).isEqualTo(payoutId);
            assertThat(result.get("status")).isEqualTo("processed");
        }

        @Test
        @DisplayName("should return mock payout when razorpay is not enabled")
        void shouldReturnMockPayoutWhenNotEnabled() {
            // Arrange
            ReflectionTestUtils.setField(razorpayService, "razorpayEnabled", false);
            String payoutId = "pout_123456";

            // Act
            Map<String, Object> result = razorpayService.fetchPayout(payoutId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.get("id")).isEqualTo(payoutId);
            assertThat(result.get("status")).isEqualTo("processed");

            verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(), eq(Map.class));
        }
    }

    @Nested
    @DisplayName("getRazorpayKeyId")
    class GetRazorpayKeyIdTests {

        @Test
        @DisplayName("should return configured key id")
        void shouldReturnConfiguredKeyId() {
            // Act
            String result = razorpayService.getRazorpayKeyId();

            // Assert
            assertThat(result).isEqualTo("rzp_test_key123");
        }
    }

    // Helper method to compute expected signature for payment verification
    private String computeExpectedSignature(String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    "test_secret_456".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Helper method to compute expected signature for webhook verification
    private String computeExpectedWebhookSignature(String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    "webhook_secret_789".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
