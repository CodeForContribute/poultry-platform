package com.poultry.order.service;

import com.poultry.common.exception.BusinessException;
import com.poultry.order.dto.CreateOrderRequest;
import com.poultry.order.dto.OrderDto;
import com.poultry.order.dto.UpdateOrderStatusRequest;
import com.poultry.order.entity.Order;
import com.poultry.order.entity.OrderHistory;
import com.poultry.order.entity.OrderItem;
import com.poultry.order.repository.OrderHistoryRepository;
import com.poultry.order.repository.OrderRepository;
import com.poultry.order.statemachine.OrderStateMachine;
import com.poultry.product.entity.Category;
import com.poultry.product.entity.PriceHistory;
import com.poultry.product.entity.Product;
import com.poultry.product.entity.Seller;
import com.poultry.product.repository.PriceHistoryRepository;
import com.poultry.product.repository.ProductRepository;
import com.poultry.product.repository.SellerRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderHistoryRepository orderHistoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private OrderStateMachine stateMachine;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<OrderHistory> orderHistoryCaptor;

    private UUID buyerId;
    private UUID sellerId;
    private UUID productId;
    private UUID orderId;
    private Seller seller;
    private Product product;
    private Category category;
    private PriceHistory priceHistory;
    private Order order;
    private CreateOrderRequest createOrderRequest;
    private CreateOrderRequest.DeliveryAddress deliveryAddress;

    @BeforeEach
    void setUp() {
        buyerId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        // Set up platform fee through reflection since @Value isn't injected in unit tests
        ReflectionTestUtils.setField(orderService, "platformFeePercent", new BigDecimal("2.0"));
        ReflectionTestUtils.setField(orderService, "paymentTimeoutMinutes", 30);

        seller = Seller.builder()
                .id(sellerId)
                .businessName("Test Poultry Farm")
                .status(Seller.SellerStatus.ACTIVE)
                .build();

        category = Category.builder()
                .id(UUID.randomUUID())
                .code(Category.CategoryCode.BROILERS)
                .name("Broilers")
                .hsnCode("0105")
                .gstRate(new BigDecimal("5.0"))
                .build();

        product = Product.builder()
                .id(productId)
                .sellerId(sellerId)
                .category(category)
                .name("Fresh Chicken")
                .sku("CHK-001")
                .unit(Product.ProductUnit.KG)
                .minOrderQty(BigDecimal.ONE)
                .maxOrderQty(new BigDecimal("100"))
                .build();

        priceHistory = PriceHistory.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .basePrice(new BigDecimal("250.00"))
                .effectiveFrom(Instant.now().minusSeconds(3600))
                .bulkDiscountSlabs(List.of())
                .build();

        deliveryAddress = CreateOrderRequest.DeliveryAddress.builder()
                .line1("123 Main St")
                .city("Bangalore")
                .state("Karnataka")
                .pincode("560001")
                .contactName("John Doe")
                .contactPhone("9876543210")
                .build();

        createOrderRequest = CreateOrderRequest.builder()
                .idempotencyKey("test-idempotency-key-" + UUID.randomUUID())
                .sellerId(sellerId)
                .type(Order.OrderType.INSTANT)
                .items(List.of(
                        CreateOrderRequest.OrderItemRequest.builder()
                                .productId(productId)
                                .quantity(new BigDecimal("5"))
                                .build()
                ))
                .deliveryAddress(deliveryAddress)
                .deliveryDate(LocalDate.now().plusDays(1))
                .deliverySlot("9AM-12PM")
                .build();

        order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-123456")
                .buyerId(buyerId)
                .sellerId(sellerId)
                .idempotencyKey("test-key")
                .type(Order.OrderType.INSTANT)
                .status(Order.OrderStatus.DRAFT)
                .subtotal(new BigDecimal("1250.00"))
                .gstAmount(new BigDecimal("62.50"))
                .totalAmount(new BigDecimal("1312.50"))
                .platformFee(new BigDecimal("26.25"))
                .deliveryAddress(deliveryAddress.toMap())
                .priceSnapshot(Map.of())
                .items(new ArrayList<>())
                .version(1)
                .build();
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("should create order successfully for valid request")
        void shouldCreateOrderSuccessfully() {
            // Arrange
            when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
            when(sellerRepository.findByIdAndStatus(sellerId, Seller.SellerStatus.ACTIVE))
                    .thenReturn(Optional.of(seller));
            when(productRepository.findByIdAndNotDeleted(productId)).thenReturn(Optional.of(product));
            when(priceHistoryRepository.findCurrentPrice(eq(productId), any(Instant.class)))
                    .thenReturn(Optional.of(priceHistory));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order o = invocation.getArgument(0);
                o.setId(orderId);
                o.setOrderNumber("ORD-123456");
                return o;
            });
            when(orderHistoryRepository.save(any(OrderHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(stateMachine.getValidNextStates(any())).thenReturn(Set.of(Order.OrderStatus.PLACED));
            when(stateMachine.canCancel(any())).thenReturn(false);

            // Act
            OrderDto result = orderService.createOrder(buyerId, createOrderRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getBuyerId()).isEqualTo(buyerId);
            assertThat(result.getSellerId()).isEqualTo(sellerId);
            assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.DRAFT);
            assertThat(result.getItems()).hasSize(1);

            verify(orderRepository).save(orderCaptor.capture());
            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getType()).isEqualTo(Order.OrderType.INSTANT);
        }

        @Test
        @DisplayName("should return existing order for duplicate idempotency key")
        void shouldReturnExistingOrderForDuplicateIdempotencyKey() {
            // Arrange
            order.setItems(List.of(OrderItem.builder()
                    .id(UUID.randomUUID())
                    .productId(productId)
                    .productSnapshot(Map.of("name", "Fresh Chicken", "sku", "CHK-001", "unit", "KG", "categoryCode", "BROILERS"))
                    .quantity(new BigDecimal("5"))
                    .unitPrice(new BigDecimal("250.00"))
                    .lineTotal(new BigDecimal("1250.00"))
                    .gstPercent(new BigDecimal("5.0"))
                    .gstAmount(new BigDecimal("62.50"))
                    .status(OrderItem.OrderItemStatus.PENDING)
                    .build()));

            when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.of(order));
            when(stateMachine.getValidNextStates(any())).thenReturn(Set.of(Order.OrderStatus.PLACED));
            when(stateMachine.canCancel(any())).thenReturn(false);

            // Act
            OrderDto result = orderService.createOrder(buyerId, createOrderRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(orderId);

            // Verify no new order was saved
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw exception when seller not found")
        void shouldThrowExceptionWhenSellerNotFound() {
            // Arrange
            when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
            when(sellerRepository.findByIdAndStatus(sellerId, Seller.SellerStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(buyerId, createOrderRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Seller not found");
        }

        @Test
        @DisplayName("should throw exception when product not found")
        void shouldThrowExceptionWhenProductNotFound() {
            // Arrange
            when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
            when(sellerRepository.findByIdAndStatus(sellerId, Seller.SellerStatus.ACTIVE))
                    .thenReturn(Optional.of(seller));
            when(productRepository.findByIdAndNotDeleted(productId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(buyerId, createOrderRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Product not found");
        }

        @Test
        @DisplayName("should throw exception when product belongs to different seller")
        void shouldThrowExceptionWhenProductBelongsToDifferentSeller() {
            // Arrange
            product.setSellerId(UUID.randomUUID()); // Different seller

            when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
            when(sellerRepository.findByIdAndStatus(sellerId, Seller.SellerStatus.ACTIVE))
                    .thenReturn(Optional.of(seller));
            when(productRepository.findByIdAndNotDeleted(productId)).thenReturn(Optional.of(product));

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(buyerId, createOrderRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("does not belong to the selected seller")
                    .extracting("errorCode")
                    .isEqualTo("INVALID_PRODUCT_SELLER");
        }

        @Test
        @DisplayName("should throw exception when quantity below minimum")
        void shouldThrowExceptionWhenQuantityBelowMinimum() {
            // Arrange
            product.setMinOrderQty(new BigDecimal("10"));
            createOrderRequest.getItems().get(0).setQuantity(new BigDecimal("5"));

            when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
            when(sellerRepository.findByIdAndStatus(sellerId, Seller.SellerStatus.ACTIVE))
                    .thenReturn(Optional.of(seller));
            when(productRepository.findByIdAndNotDeleted(productId)).thenReturn(Optional.of(product));

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(buyerId, createOrderRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Quantity below minimum")
                    .extracting("errorCode")
                    .isEqualTo("MIN_QTY_NOT_MET");
        }

        @Test
        @DisplayName("should throw exception when quantity exceeds maximum")
        void shouldThrowExceptionWhenQuantityExceedsMaximum() {
            // Arrange
            product.setMaxOrderQty(new BigDecimal("3"));
            createOrderRequest.getItems().get(0).setQuantity(new BigDecimal("5"));

            when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
            when(sellerRepository.findByIdAndStatus(sellerId, Seller.SellerStatus.ACTIVE))
                    .thenReturn(Optional.of(seller));
            when(productRepository.findByIdAndNotDeleted(productId)).thenReturn(Optional.of(product));

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(buyerId, createOrderRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Quantity exceeds maximum")
                    .extracting("errorCode")
                    .isEqualTo("MAX_QTY_EXCEEDED");
        }

        @Test
        @DisplayName("should throw exception when no price available for product")
        void shouldThrowExceptionWhenNoPriceAvailable() {
            // Arrange
            when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
            when(sellerRepository.findByIdAndStatus(sellerId, Seller.SellerStatus.ACTIVE))
                    .thenReturn(Optional.of(seller));
            when(productRepository.findByIdAndNotDeleted(productId)).thenReturn(Optional.of(product));
            when(priceHistoryRepository.findCurrentPrice(eq(productId), any(Instant.class)))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(buyerId, createOrderRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No price available")
                    .extracting("errorCode")
                    .isEqualTo("NO_PRICE");
        }

        @Test
        @DisplayName("should calculate platform fee correctly")
        void shouldCalculatePlatformFeeCorrectly() {
            // Arrange
            when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
            when(sellerRepository.findByIdAndStatus(sellerId, Seller.SellerStatus.ACTIVE))
                    .thenReturn(Optional.of(seller));
            when(productRepository.findByIdAndNotDeleted(productId)).thenReturn(Optional.of(product));
            when(priceHistoryRepository.findCurrentPrice(eq(productId), any(Instant.class)))
                    .thenReturn(Optional.of(priceHistory));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order o = invocation.getArgument(0);
                o.setId(orderId);
                return o;
            });
            when(orderHistoryRepository.save(any(OrderHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(stateMachine.getValidNextStates(any())).thenReturn(Set.of(Order.OrderStatus.PLACED));
            when(stateMachine.canCancel(any())).thenReturn(false);

            // Act
            orderService.createOrder(buyerId, createOrderRequest);

            // Assert
            verify(orderRepository).save(orderCaptor.capture());
            Order savedOrder = orderCaptor.getValue();
            // Platform fee should be 2% of total
            BigDecimal expectedFee = savedOrder.getTotalAmount()
                    .multiply(new BigDecimal("2.0"))
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            assertThat(savedOrder.getPlatformFee()).isEqualByComparingTo(expectedFee);
        }

        @Test
        @DisplayName("should log order history on creation")
        void shouldLogOrderHistoryOnCreation() {
            // Arrange
            when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
            when(sellerRepository.findByIdAndStatus(sellerId, Seller.SellerStatus.ACTIVE))
                    .thenReturn(Optional.of(seller));
            when(productRepository.findByIdAndNotDeleted(productId)).thenReturn(Optional.of(product));
            when(priceHistoryRepository.findCurrentPrice(eq(productId), any(Instant.class)))
                    .thenReturn(Optional.of(priceHistory));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order o = invocation.getArgument(0);
                o.setId(orderId);
                return o;
            });
            when(orderHistoryRepository.save(any(OrderHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(stateMachine.getValidNextStates(any())).thenReturn(Set.of(Order.OrderStatus.PLACED));
            when(stateMachine.canCancel(any())).thenReturn(false);

            // Act
            orderService.createOrder(buyerId, createOrderRequest);

            // Assert
            verify(orderHistoryRepository).save(orderHistoryCaptor.capture());
            OrderHistory history = orderHistoryCaptor.getValue();
            assertThat(history.getToStatus()).isEqualTo(Order.OrderStatus.DRAFT);
            assertThat(history.getChangedByType()).isEqualTo("BUYER");
            assertThat(history.getChangedById()).isEqualTo(buyerId);
        }
    }

    @Nested
    @DisplayName("placeOrder")
    class PlaceOrderTests {

        @Test
        @DisplayName("should place order successfully")
        void shouldPlaceOrderSuccessfully() {
            // Arrange
            order.setItems(List.of(OrderItem.builder()
                    .id(UUID.randomUUID())
                    .productId(productId)
                    .productSnapshot(Map.of("name", "Fresh Chicken", "sku", "CHK-001", "unit", "KG", "categoryCode", "BROILERS"))
                    .quantity(new BigDecimal("5"))
                    .unitPrice(new BigDecimal("250.00"))
                    .lineTotal(new BigDecimal("1250.00"))
                    .gstPercent(new BigDecimal("5.0"))
                    .gstAmount(new BigDecimal("62.50"))
                    .status(OrderItem.OrderItemStatus.PENDING)
                    .build()));

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            doAnswer(invocation -> {
                Order o = invocation.getArgument(0);
                o.setStatus(Order.OrderStatus.PLACED);
                return null;
            }).when(stateMachine).transition(any(Order.class), eq(Order.OrderStatus.PLACED));
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(orderHistoryRepository.save(any(OrderHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(stateMachine.getValidNextStates(any())).thenReturn(Set.of(Order.OrderStatus.SELLER_CONFIRMED));
            when(stateMachine.canCancel(any())).thenReturn(false);

            // Act
            OrderDto result = orderService.placeOrder(buyerId, orderId);

            // Assert
            assertThat(result).isNotNull();
            verify(stateMachine).transition(any(Order.class), eq(Order.OrderStatus.PLACED));
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.placeOrder(buyerId, orderId))
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
            assertThatThrownBy(() -> orderService.placeOrder(differentBuyerId, orderId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("don't have access to this order");
        }

        @Test
        @DisplayName("should log order history on place order")
        void shouldLogOrderHistoryOnPlaceOrder() {
            // Arrange
            order.setItems(List.of(OrderItem.builder()
                    .id(UUID.randomUUID())
                    .productId(productId)
                    .productSnapshot(Map.of("name", "Fresh Chicken", "sku", "CHK-001", "unit", "KG", "categoryCode", "BROILERS"))
                    .quantity(new BigDecimal("5"))
                    .unitPrice(new BigDecimal("250.00"))
                    .lineTotal(new BigDecimal("1250.00"))
                    .gstPercent(new BigDecimal("5.0"))
                    .gstAmount(new BigDecimal("62.50"))
                    .status(OrderItem.OrderItemStatus.PENDING)
                    .build()));

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            doAnswer(invocation -> {
                Order o = invocation.getArgument(0);
                o.setStatus(Order.OrderStatus.PLACED);
                return null;
            }).when(stateMachine).transition(any(Order.class), eq(Order.OrderStatus.PLACED));
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(orderHistoryRepository.save(any(OrderHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(stateMachine.getValidNextStates(any())).thenReturn(Set.of(Order.OrderStatus.SELLER_CONFIRMED));
            when(stateMachine.canCancel(any())).thenReturn(false);

            // Act
            orderService.placeOrder(buyerId, orderId);

            // Assert
            verify(orderHistoryRepository).save(orderHistoryCaptor.capture());
            OrderHistory history = orderHistoryCaptor.getValue();
            assertThat(history.getFromStatus()).isEqualTo(Order.OrderStatus.DRAFT);
            assertThat(history.getToStatus()).isEqualTo(Order.OrderStatus.PLACED);
        }
    }

    @Nested
    @DisplayName("updateOrderStatus")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("should update order status successfully")
        void shouldUpdateOrderStatusSuccessfully() {
            // Arrange
            order.setStatus(Order.OrderStatus.PLACED);
            order.setItems(List.of(OrderItem.builder()
                    .id(UUID.randomUUID())
                    .productId(productId)
                    .productSnapshot(Map.of("name", "Fresh Chicken", "sku", "CHK-001", "unit", "KG", "categoryCode", "BROILERS"))
                    .quantity(new BigDecimal("5"))
                    .unitPrice(new BigDecimal("250.00"))
                    .lineTotal(new BigDecimal("1250.00"))
                    .gstPercent(new BigDecimal("5.0"))
                    .gstAmount(new BigDecimal("62.50"))
                    .status(OrderItem.OrderItemStatus.PENDING)
                    .build()));

            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(Order.OrderStatus.SELLER_CONFIRMED)
                    .build();

            when(orderRepository.findByIdWithLock(orderId)).thenReturn(Optional.of(order));
            doAnswer(invocation -> {
                Order o = invocation.getArgument(0);
                o.setStatus(Order.OrderStatus.SELLER_CONFIRMED);
                return null;
            }).when(stateMachine).transition(any(Order.class), eq(Order.OrderStatus.SELLER_CONFIRMED));
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(orderHistoryRepository.save(any(OrderHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(stateMachine.getValidNextStates(any())).thenReturn(Set.of(Order.OrderStatus.PAYMENT_PENDING));
            when(stateMachine.canCancel(any())).thenReturn(false);

            // Act
            OrderDto result = orderService.updateOrderStatus(orderId, request, "SELLER", sellerId);

            // Assert
            assertThat(result).isNotNull();
            verify(orderRepository).save(orderCaptor.capture());
            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getExpiresAt()).isNotNull(); // Payment expiry should be set
        }

        @Test
        @DisplayName("should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Arrange
            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(Order.OrderStatus.SELLER_CONFIRMED)
                    .build();

            when(orderRepository.findByIdWithLock(orderId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, request, "SELLER", sellerId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Order not found");
        }

        @Test
        @DisplayName("should throw exception on version mismatch")
        void shouldThrowExceptionOnVersionMismatch() {
            // Arrange
            order.setVersion(2);
            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(Order.OrderStatus.SELLER_CONFIRMED)
                    .expectedVersion(1)
                    .build();

            when(orderRepository.findByIdWithLock(orderId)).thenReturn(Optional.of(order));

            // Act & Assert
            assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, request, "SELLER", sellerId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Order has been modified")
                    .extracting("errorCode")
                    .isEqualTo("OPTIMISTIC_LOCK_FAILURE");
        }

        @Test
        @DisplayName("should handle cancellation by buyer")
        void shouldHandleCancellationByBuyer() {
            // Arrange
            order.setStatus(Order.OrderStatus.PAID);
            order.setItems(List.of(OrderItem.builder()
                    .id(UUID.randomUUID())
                    .productId(productId)
                    .productSnapshot(Map.of("name", "Fresh Chicken", "sku", "CHK-001", "unit", "KG", "categoryCode", "BROILERS"))
                    .quantity(new BigDecimal("5"))
                    .unitPrice(new BigDecimal("250.00"))
                    .lineTotal(new BigDecimal("1250.00"))
                    .gstPercent(new BigDecimal("5.0"))
                    .gstAmount(new BigDecimal("62.50"))
                    .status(OrderItem.OrderItemStatus.PENDING)
                    .build()));

            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(Order.OrderStatus.CANCELLED_BY_BUYER)
                    .reason("Changed my mind")
                    .build();

            when(orderRepository.findByIdWithLock(orderId)).thenReturn(Optional.of(order));
            doAnswer(invocation -> {
                Order o = invocation.getArgument(0);
                o.setStatus(Order.OrderStatus.CANCELLED_BY_BUYER);
                return null;
            }).when(stateMachine).transition(any(Order.class), eq(Order.OrderStatus.CANCELLED_BY_BUYER));
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(orderHistoryRepository.save(any(OrderHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(stateMachine.getValidNextStates(any())).thenReturn(Set.of(Order.OrderStatus.REFUND_INITIATED));
            when(stateMachine.canCancel(any())).thenReturn(false);

            // Act
            OrderDto result = orderService.updateOrderStatus(orderId, request, "BUYER", buyerId);

            // Assert
            verify(orderRepository).save(orderCaptor.capture());
            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getCancelledAt()).isNotNull();
            assertThat(savedOrder.getCancelledBy()).isEqualTo("BUYER");
            assertThat(savedOrder.getCancellationReason()).isEqualTo("Changed my mind");
        }

        @Test
        @DisplayName("should handle optimistic locking failure from database")
        void shouldHandleOptimisticLockingFailureFromDatabase() {
            // Arrange
            order.setStatus(Order.OrderStatus.PLACED);
            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(Order.OrderStatus.SELLER_CONFIRMED)
                    .build();

            when(orderRepository.findByIdWithLock(orderId)).thenReturn(Optional.of(order));
            doNothing().when(stateMachine).transition(any(Order.class), eq(Order.OrderStatus.SELLER_CONFIRMED));
            when(orderRepository.save(any(Order.class)))
                    .thenThrow(new ObjectOptimisticLockingFailureException(Order.class, orderId));

            // Act & Assert
            assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, request, "SELLER", sellerId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Order has been modified")
                    .extracting("errorCode")
                    .isEqualTo("OPTIMISTIC_LOCK_FAILURE");
        }
    }

    @Nested
    @DisplayName("getOrder")
    class GetOrderTests {

        @Test
        @DisplayName("should return order dto successfully")
        void shouldReturnOrderDtoSuccessfully() {
            // Arrange
            order.setItems(List.of(OrderItem.builder()
                    .id(UUID.randomUUID())
                    .productId(productId)
                    .productSnapshot(Map.of("name", "Fresh Chicken", "sku", "CHK-001", "unit", "KG", "categoryCode", "BROILERS"))
                    .quantity(new BigDecimal("5"))
                    .unitPrice(new BigDecimal("250.00"))
                    .lineTotal(new BigDecimal("1250.00"))
                    .gstPercent(new BigDecimal("5.0"))
                    .gstAmount(new BigDecimal("62.50"))
                    .status(OrderItem.OrderItemStatus.PENDING)
                    .build()));

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(stateMachine.getValidNextStates(any())).thenReturn(Set.of(Order.OrderStatus.PLACED));
            when(stateMachine.canCancel(any())).thenReturn(false);

            // Act
            OrderDto result = orderService.getOrder(orderId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(orderId);
            assertThat(result.getBuyerId()).isEqualTo(buyerId);
            assertThat(result.getSellerId()).isEqualTo(sellerId);
            assertThat(result.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.getOrder(orderId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Order not found");
        }
    }

    @Nested
    @DisplayName("getBuyerOrders")
    class GetBuyerOrdersTests {

        @Test
        @DisplayName("should return paginated buyer orders")
        void shouldReturnPaginatedBuyerOrders() {
            // Arrange
            order.setItems(List.of(OrderItem.builder()
                    .id(UUID.randomUUID())
                    .productId(productId)
                    .productSnapshot(Map.of("name", "Fresh Chicken", "sku", "CHK-001", "unit", "KG", "categoryCode", "BROILERS"))
                    .quantity(new BigDecimal("5"))
                    .unitPrice(new BigDecimal("250.00"))
                    .lineTotal(new BigDecimal("1250.00"))
                    .gstPercent(new BigDecimal("5.0"))
                    .gstAmount(new BigDecimal("62.50"))
                    .status(OrderItem.OrderItemStatus.PENDING)
                    .build()));

            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);

            when(orderRepository.findByBuyerId(buyerId, pageable)).thenReturn(orderPage);
            when(stateMachine.getValidNextStates(any())).thenReturn(Set.of(Order.OrderStatus.PLACED));
            when(stateMachine.canCancel(any())).thenReturn(false);

            // Act
            Page<OrderDto> result = orderService.getBuyerOrders(buyerId, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return empty page when buyer has no orders")
        void shouldReturnEmptyPageWhenNoOrders() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(orderRepository.findByBuyerId(buyerId, pageable)).thenReturn(emptyPage);

            // Act
            Page<OrderDto> result = orderService.getBuyerOrders(buyerId, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("getSellerOrders")
    class GetSellerOrdersTests {

        @Test
        @DisplayName("should return paginated seller orders")
        void shouldReturnPaginatedSellerOrders() {
            // Arrange
            order.setItems(List.of(OrderItem.builder()
                    .id(UUID.randomUUID())
                    .productId(productId)
                    .productSnapshot(Map.of("name", "Fresh Chicken", "sku", "CHK-001", "unit", "KG", "categoryCode", "BROILERS"))
                    .quantity(new BigDecimal("5"))
                    .unitPrice(new BigDecimal("250.00"))
                    .lineTotal(new BigDecimal("1250.00"))
                    .gstPercent(new BigDecimal("5.0"))
                    .gstAmount(new BigDecimal("62.50"))
                    .status(OrderItem.OrderItemStatus.PENDING)
                    .build()));

            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);

            when(orderRepository.findBySellerId(sellerId, pageable)).thenReturn(orderPage);
            when(stateMachine.getValidNextStates(any())).thenReturn(Set.of(Order.OrderStatus.PLACED));
            when(stateMachine.canCancel(any())).thenReturn(false);

            // Act
            Page<OrderDto> result = orderService.getSellerOrders(sellerId, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getSellerOrdersByStatus")
    class GetSellerOrdersByStatusTests {

        @Test
        @DisplayName("should return orders filtered by status")
        void shouldReturnOrdersFilteredByStatus() {
            // Arrange
            order.setStatus(Order.OrderStatus.PLACED);
            order.setItems(List.of(OrderItem.builder()
                    .id(UUID.randomUUID())
                    .productId(productId)
                    .productSnapshot(Map.of("name", "Fresh Chicken", "sku", "CHK-001", "unit", "KG", "categoryCode", "BROILERS"))
                    .quantity(new BigDecimal("5"))
                    .unitPrice(new BigDecimal("250.00"))
                    .lineTotal(new BigDecimal("1250.00"))
                    .gstPercent(new BigDecimal("5.0"))
                    .gstAmount(new BigDecimal("62.50"))
                    .status(OrderItem.OrderItemStatus.PENDING)
                    .build()));

            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);

            when(orderRepository.findBySellerIdAndStatus(sellerId, Order.OrderStatus.PLACED, pageable))
                    .thenReturn(orderPage);
            when(stateMachine.getValidNextStates(any())).thenReturn(Set.of(Order.OrderStatus.SELLER_CONFIRMED));
            when(stateMachine.canCancel(any())).thenReturn(false);

            // Act
            Page<OrderDto> result = orderService.getSellerOrdersByStatus(sellerId, Order.OrderStatus.PLACED, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(Order.OrderStatus.PLACED);
        }
    }
}
