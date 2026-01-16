package com.poultry.testutil;

import com.poultry.auth.dto.*;
import com.poultry.auth.entity.*;
import com.poultry.cart.dto.AddToCartRequest;
import com.poultry.cart.dto.UpdateCartItemRequest;
import com.poultry.cart.entity.Cart;
import com.poultry.cart.entity.CartItem;
import com.poultry.order.dto.CreateOrderRequest;
import com.poultry.order.dto.UpdateOrderStatusRequest;
import com.poultry.order.entity.Order;
import com.poultry.order.entity.OrderItem;
import com.poultry.payment.dto.CreatePaymentRequest;
import com.poultry.payment.entity.Payment;
import com.poultry.product.entity.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Factory class for creating test data entities and DTOs.
 * Provides builder methods with sensible defaults for testing.
 */
public final class TestDataFactory {

    private static final Random RANDOM = new Random();

    private TestDataFactory() {
        // Utility class - no instantiation
    }

    // ============ Auth Entities ============

    public static Buyer createBuyer() {
        String phone = generatePhoneNumber();
        return Buyer.builder()
                .phoneEncrypted(phone.getBytes())
                .phoneHash("hash_" + phone)
                .name("Test Buyer " + RANDOM.nextInt(1000))
                .status(Buyer.BuyerStatus.ACTIVE)
                .deviceId("device_" + UUID.randomUUID().toString().substring(0, 8))
                .build();
    }

    public static Buyer createBuyer(Buyer.BuyerStatus status) {
        Buyer buyer = createBuyer();
        buyer.setStatus(status);
        return buyer;
    }

    public static SellerUser createSellerUser() {
        return SellerUser.builder()
                .email("seller" + RANDOM.nextInt(10000) + "@test.com")
                .passwordHash("hashedPassword123")
                .name("Test Seller User")
                .phone(generatePhoneNumber())
                .role(SellerUser.SellerUserRole.SELLER_ADMIN)
                .status(SellerUser.SellerStatus.ACTIVE)
                .build();
    }

    public static SellerUser createSellerUser(UUID sellerId) {
        SellerUser user = createSellerUser();
        user.setSellerId(sellerId);
        return user;
    }

    public static OtpRequest createOtpRequest(String phone, OtpRequest.OtpPurpose purpose) {
        return OtpRequest.builder()
                .phoneHash("hash_" + phone)
                .phoneEncrypted(phone.getBytes())
                .otpHash("otp_hash_123456")
                .purpose(purpose)
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .maxAttempts(3)
                .ipAddress("127.0.0.1")
                .deviceInfo(Map.of("deviceId", "test_device"))
                .build();
    }

    public static BuyerSession createBuyerSession(UUID buyerId) {
        return BuyerSession.builder()
                .buyerId(buyerId)
                .deviceId("device_" + UUID.randomUUID().toString().substring(0, 8))
                .deviceFingerprint("fingerprint_" + RANDOM.nextInt(10000))
                .deviceInfo(Map.of("os", "Android", "version", "14"))
                .ipAddress("192.168.1." + RANDOM.nextInt(255))
                .refreshTokenHash("refresh_token_hash_" + UUID.randomUUID())
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
    }

    // ============ Auth DTOs ============

    public static BuyerOtpRequest createBuyerOtpRequest() {
        return BuyerOtpRequest.builder()
                .phone(generatePhoneNumber())
                .deviceId("device_" + UUID.randomUUID().toString().substring(0, 8))
                .deviceFingerprint("fingerprint_" + RANDOM.nextInt(10000))
                .build();
    }

    public static BuyerOtpVerifyRequest createBuyerOtpVerifyRequest(String phone, String otp) {
        return BuyerOtpVerifyRequest.builder()
                .phone(phone)
                .otp(otp)
                .deviceId("device_" + UUID.randomUUID().toString().substring(0, 8))
                .deviceFingerprint("fingerprint_" + RANDOM.nextInt(10000))
                .build();
    }

    public static RefreshTokenRequest createRefreshTokenRequest(String refreshToken) {
        return RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();
    }

    // ============ Product Entities ============

    public static Seller createSeller() {
        return Seller.builder()
                .businessName("Test Poultry Farm " + RANDOM.nextInt(1000))
                .phoneEncrypted(generatePhoneNumber().getBytes())
                .email("farm" + RANDOM.nextInt(10000) + "@test.com")
                .gstin("29ABCDE" + String.format("%04d", RANDOM.nextInt(10000)) + "F1Z5")
                .pan("ABCDE" + String.format("%04d", RANDOM.nextInt(10000)) + "F")
                .status(Seller.SellerStatus.ACTIVE)
                .platformFeePercent(BigDecimal.valueOf(2))
                .settlementCycle(Seller.SettlementCycle.T_PLUS_1)
                .build();
    }

    public static Seller createSeller(Seller.SellerStatus status) {
        Seller seller = createSeller();
        seller.setStatus(status);
        return seller;
    }

    public static Category createCategory() {
        return Category.builder()
                .code(Category.CategoryCode.BROILERS)
                .name("Broilers")
                .hsnCode("0105")
                .description("Fresh chicken products")
                .gstRate(BigDecimal.valueOf(5))
                .status(Category.ProductStatus.ACTIVE)
                .build();
    }

    public static Category createCategory(Category.CategoryCode code) {
        Category category = createCategory();
        category.setCode(code);
        category.setName(code.name());
        return category;
    }

    public static Product createProduct(UUID sellerId, Category category) {
        return Product.builder()
                .sellerId(sellerId)
                .category(category)
                .name("Fresh Chicken " + RANDOM.nextInt(1000))
                .sku("PRD-" + String.format("%06d", RANDOM.nextInt(1000000)))
                .description("High-quality farm-raised chicken")
                .unit(Product.ProductUnit.KG)
                .minOrderQty(BigDecimal.ONE)
                .maxOrderQty(BigDecimal.valueOf(100))
                .status(Product.ProductStatus.ACTIVE)
                .build();
    }

    public static Product createProduct(UUID sellerId) {
        return createProduct(sellerId, createCategory());
    }

    public static PriceHistory createPriceHistory(UUID productId) {
        return PriceHistory.builder()
                .productId(productId)
                .basePrice(BigDecimal.valueOf(200 + RANDOM.nextInt(100)))
                .effectiveFrom(Instant.now().minus(1, ChronoUnit.HOURS))
                .bulkDiscountSlabs(List.of())
                .build();
    }

    public static PriceHistory createPriceHistory(UUID productId, BigDecimal price) {
        PriceHistory priceHistory = createPriceHistory(productId);
        priceHistory.setBasePrice(price);
        return priceHistory;
    }

    // ============ Cart Entities ============

    public static Cart createCart(UUID buyerId, UUID sellerId) {
        Cart cart = Cart.builder()
                .buyerId(buyerId)
                .sellerId(sellerId)
                .build();
        cart.setItems(new ArrayList<>());
        return cart;
    }

    public static CartItem createCartItem(Cart cart, UUID productId) {
        return CartItem.builder()
                .cart(cart)
                .productId(productId)
                .quantity(BigDecimal.valueOf(5))
                .unitPrice(BigDecimal.valueOf(250))
                .notes("Test item notes")
                .build();
    }

    // ============ Cart DTOs ============

    public static AddToCartRequest createAddToCartRequest(UUID productId) {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(productId);
        request.setQuantity(BigDecimal.valueOf(5));
        request.setNotes("Test cart item");
        return request;
    }

    public static AddToCartRequest createAddToCartRequest(UUID productId, BigDecimal quantity) {
        AddToCartRequest request = createAddToCartRequest(productId);
        request.setQuantity(quantity);
        return request;
    }

    public static UpdateCartItemRequest createUpdateCartItemRequest(BigDecimal quantity) {
        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(quantity);
        return request;
    }

    // ============ Order Entities ============

    public static Order createOrder(UUID buyerId, UUID sellerId) {
        return Order.builder()
                .buyerId(buyerId)
                .sellerId(sellerId)
                .idempotencyKey(UUID.randomUUID().toString())
                .orderNumber("ORD-" + String.format("%06d", RANDOM.nextInt(1000000)))
                .type(Order.OrderType.INSTANT)
                .status(Order.OrderStatus.DRAFT)
                .subtotal(BigDecimal.valueOf(1250))
                .gstAmount(BigDecimal.valueOf(62.50))
                .totalAmount(BigDecimal.valueOf(1312.50))
                .platformFee(BigDecimal.valueOf(26.25))
                .deliveryAddress(createDeliveryAddressMap())
                .priceSnapshot(Map.of("capturedAt", Instant.now().toString()))
                .items(new ArrayList<>())
                .version(1)
                .build();
    }

    public static Order createOrder(UUID buyerId, UUID sellerId, Order.OrderStatus status) {
        Order order = createOrder(buyerId, sellerId);
        order.setStatus(status);
        return order;
    }

    public static OrderItem createOrderItem(UUID productId) {
        return OrderItem.builder()
                .productId(productId)
                .productSnapshot(Map.of(
                        "name", "Fresh Chicken",
                        "sku", "CHK-001",
                        "unit", "KG",
                        "categoryCode", "BROILERS"
                ))
                .quantity(BigDecimal.valueOf(5))
                .unitPrice(BigDecimal.valueOf(250))
                .lineTotal(BigDecimal.valueOf(1250))
                .gstPercent(BigDecimal.valueOf(5))
                .gstAmount(BigDecimal.valueOf(62.50))
                .status(OrderItem.OrderItemStatus.PENDING)
                .build();
    }

    // ============ Order DTOs ============

    public static CreateOrderRequest createCreateOrderRequest(UUID sellerId, UUID productId) {
        return CreateOrderRequest.builder()
                .idempotencyKey(UUID.randomUUID().toString())
                .sellerId(sellerId)
                .type(Order.OrderType.INSTANT)
                .items(List.of(
                        CreateOrderRequest.OrderItemRequest.builder()
                                .productId(productId)
                                .quantity(BigDecimal.valueOf(5))
                                .build()
                ))
                .deliveryAddress(createDeliveryAddress())
                .deliveryDate(LocalDate.now().plusDays(1))
                .deliverySlot("9AM-12PM")
                .build();
    }

    public static CreateOrderRequest.DeliveryAddress createDeliveryAddress() {
        return CreateOrderRequest.DeliveryAddress.builder()
                .line1("123 Test Street")
                .line2("Near Test Park")
                .city("Bangalore")
                .state("Karnataka")
                .pincode("560001")
                .contactName("Test Contact")
                .contactPhone(generatePhoneNumber())
                .build();
    }

    public static Map<String, Object> createDeliveryAddressMap() {
        Map<String, Object> address = new HashMap<>();
        address.put("line1", "123 Test Street");
        address.put("city", "Bangalore");
        address.put("state", "Karnataka");
        address.put("pincode", "560001");
        address.put("contactName", "Test Contact");
        address.put("contactPhone", generatePhoneNumber());
        return address;
    }

    public static UpdateOrderStatusRequest createUpdateOrderStatusRequest(Order.OrderStatus status) {
        return UpdateOrderStatusRequest.builder()
                .status(status)
                .build();
    }

    public static UpdateOrderStatusRequest createUpdateOrderStatusRequest(Order.OrderStatus status, Integer expectedVersion) {
        return UpdateOrderStatusRequest.builder()
                .status(status)
                .expectedVersion(expectedVersion)
                .build();
    }

    public static UpdateOrderStatusRequest createCancellationRequest(Order.OrderStatus cancelStatus, String reason) {
        return UpdateOrderStatusRequest.builder()
                .status(cancelStatus)
                .reason(reason)
                .build();
    }

    // ============ Payment Entities ============

    public static Payment createPayment(UUID orderId) {
        return Payment.builder()
                .orderId(orderId)
                .gateway(Payment.PaymentGateway.RAZORPAY)
                .gatewayOrderId("order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14))
                .amount(BigDecimal.valueOf(1312.50))
                .currency("INR")
                .status(Payment.PaymentStatus.CREATED)
                .build();
    }

    public static Payment createSuccessfulPayment(UUID orderId) {
        Payment payment = createPayment(orderId);
        payment.setGatewayPaymentId("pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.now());
        payment.setSignatureVerified(true);
        return payment;
    }

    public static Payment createFailedPayment(UUID orderId) {
        Payment payment = createPayment(orderId);
        payment.setGatewayPaymentId("pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setErrorCode("BAD_REQUEST_ERROR");
        payment.setErrorDescription("Payment failed due to insufficient funds");
        return payment;
    }

    // ============ Payment DTOs ============

    public static CreatePaymentRequest createCreatePaymentRequest(UUID orderId) {
        return CreatePaymentRequest.builder()
                .orderId(orderId)
                .build();
    }

    // ============ Webhook Payloads ============

    public static Map<String, Object> createRazorpaySuccessWebhookPayload(String gatewayOrderId, String gatewayPaymentId) {
        Map<String, Object> entity = new HashMap<>();
        entity.put("id", gatewayPaymentId);
        entity.put("order_id", gatewayOrderId);
        entity.put("amount", 131250);
        entity.put("currency", "INR");
        entity.put("status", "captured");

        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("entity", entity);

        Map<String, Object> payload = new HashMap<>();
        payload.put("payment", paymentData);

        Map<String, Object> webhookPayload = new HashMap<>();
        webhookPayload.put("event", "payment.captured");
        webhookPayload.put("payload", payload);

        return webhookPayload;
    }

    public static Map<String, Object> createRazorpayFailureWebhookPayload(String gatewayOrderId, String gatewayPaymentId) {
        Map<String, Object> entity = new HashMap<>();
        entity.put("id", gatewayPaymentId);
        entity.put("order_id", gatewayOrderId);
        entity.put("amount", 131250);
        entity.put("currency", "INR");
        entity.put("status", "failed");
        entity.put("error_code", "BAD_REQUEST_ERROR");
        entity.put("error_description", "Card declined by bank");

        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("entity", entity);

        Map<String, Object> payload = new HashMap<>();
        payload.put("payment", paymentData);

        Map<String, Object> webhookPayload = new HashMap<>();
        webhookPayload.put("event", "payment.failed");
        webhookPayload.put("payload", payload);

        return webhookPayload;
    }

    // ============ Utility Methods ============

    public static String generatePhoneNumber() {
        return "98" + String.format("%08d", RANDOM.nextInt(100000000));
    }

    public static String generateOtp() {
        return String.format("%06d", RANDOM.nextInt(1000000));
    }

    public static String generateIdempotencyKey() {
        return UUID.randomUUID().toString();
    }

    public static BigDecimal randomAmount(int min, int max) {
        return BigDecimal.valueOf(min + RANDOM.nextInt(max - min));
    }

    public static BigDecimal randomQuantity(int min, int max) {
        return BigDecimal.valueOf(min + RANDOM.nextInt(max - min));
    }
}
