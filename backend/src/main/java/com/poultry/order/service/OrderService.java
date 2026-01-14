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
import com.poultry.product.entity.PriceHistory;
import com.poultry.product.entity.Product;
import com.poultry.product.entity.Seller;
import com.poultry.product.repository.PriceHistoryRepository;
import com.poultry.product.repository.ProductRepository;
import com.poultry.product.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final SellerRepository sellerRepository;
    private final OrderStateMachine stateMachine;

    @Value("${platform.fee-percent:2.0}")
    private BigDecimal platformFeePercent;

    @Value("${platform.payment-timeout-minutes:30}")
    private int paymentTimeoutMinutes;

    @Transactional
    public OrderDto createOrder(UUID buyerId, CreateOrderRequest request) {
        // Check idempotency
        Optional<Order> existing = orderRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            log.info("Returning existing order for idempotency key: {}", request.getIdempotencyKey());
            return mapToDto(existing.get());
        }

        // Validate seller
        Seller seller = sellerRepository.findByIdAndStatus(request.getSellerId(), Seller.SellerStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.notFound("Seller", request.getSellerId()));

        // Create order
        Order order = Order.builder()
                .buyerId(buyerId)
                .sellerId(request.getSellerId())
                .idempotencyKey(request.getIdempotencyKey())
                .type(request.getType())
                .status(Order.OrderStatus.DRAFT)
                .deliveryAddress(request.getDeliveryAddress().toMap())
                .deliveryDate(request.getDeliveryDate())
                .deliverySlot(request.getDeliverySlot())
                .deliveryInstructions(request.getDeliveryInstructions())
                .build();

        // Process items
        Map<String, Object> priceSnapshot = new HashMap<>();
        List<Map<String, Object>> itemSnapshots = new ArrayList<>();

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findByIdAndNotDeleted(itemRequest.getProductId())
                    .orElseThrow(() -> BusinessException.notFound("Product", itemRequest.getProductId()));

            if (!product.getSellerId().equals(request.getSellerId())) {
                throw new BusinessException(
                        "Product does not belong to the selected seller",
                        "INVALID_PRODUCT_SELLER",
                        HttpStatus.BAD_REQUEST
                );
            }

            // Validate quantity
            if (itemRequest.getQuantity().compareTo(product.getMinOrderQty()) < 0) {
                throw new BusinessException(
                        "Quantity below minimum for " + product.getName(),
                        "MIN_QTY_NOT_MET",
                        HttpStatus.BAD_REQUEST
                );
            }
            if (product.getMaxOrderQty() != null
                    && itemRequest.getQuantity().compareTo(product.getMaxOrderQty()) > 0) {
                throw new BusinessException(
                        "Quantity exceeds maximum for " + product.getName(),
                        "MAX_QTY_EXCEEDED",
                        HttpStatus.BAD_REQUEST
                );
            }

            // Get current price
            PriceHistory price = priceHistoryRepository.findCurrentPrice(product.getId(), Instant.now())
                    .orElseThrow(() -> new BusinessException(
                            "No price available for " + product.getName(),
                            "NO_PRICE",
                            HttpStatus.BAD_REQUEST
                    ));

            BigDecimal unitPrice = price.calculatePrice(itemRequest.getQuantity());
            BigDecimal gstPercent = product.getCategory().getGstRate();

            // Create order item
            OrderItem item = OrderItem.builder()
                    .productId(product.getId())
                    .productSnapshot(Map.of(
                            "name", product.getName(),
                            "sku", product.getSku(),
                            "unit", product.getUnit().name(),
                            "categoryCode", product.getCategory().getCode().name()
                    ))
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .gstPercent(gstPercent)
                    .build();

            item.calculateLineTotal();
            order.addItem(item);

            // Store in snapshot
            itemSnapshots.add(Map.of(
                    "productId", product.getId().toString(),
                    "productName", product.getName(),
                    "quantity", itemRequest.getQuantity(),
                    "unitPrice", unitPrice,
                    "basePrice", price.getBasePrice(),
                    "bulkSlabs", price.getBulkDiscountSlabs()
            ));
        }

        // Calculate totals
        order.calculateTotals();

        // Calculate platform fee
        order.setPlatformFee(order.getTotalAmount()
                .multiply(platformFeePercent)
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP));

        // Store price snapshot
        priceSnapshot.put("items", itemSnapshots);
        priceSnapshot.put("capturedAt", Instant.now().toString());
        order.setPriceSnapshot(priceSnapshot);

        order = orderRepository.save(order);

        // Log history
        logOrderHistory(order.getId(), null, Order.OrderStatus.DRAFT, "BUYER", buyerId, "Order created");

        log.info("Order created: {} for buyer: {}", order.getId(), buyerId);

        return mapToDto(order);
    }

    @Transactional
    public OrderDto placeOrder(UUID buyerId, UUID orderId) {
        Order order = getOrderForBuyer(orderId, buyerId);

        stateMachine.transition(order, Order.OrderStatus.PLACED);
        order = orderRepository.save(order);

        logOrderHistory(orderId, Order.OrderStatus.DRAFT, Order.OrderStatus.PLACED, "BUYER", buyerId, null);

        log.info("Order placed: {}", orderId);
        return mapToDto(order);
    }

    @Transactional
    public OrderDto updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request,
                                      String actorType, UUID actorId) {
        try {
            Order order = orderRepository.findByIdWithLock(orderId)
                    .orElseThrow(() -> BusinessException.notFound("Order", orderId));

            // Version check for optimistic locking
            if (request.getExpectedVersion() != null
                    && !request.getExpectedVersion().equals(order.getVersion())) {
                throw new BusinessException(
                        "Order has been modified. Please refresh and try again.",
                        "OPTIMISTIC_LOCK_FAILURE",
                        HttpStatus.CONFLICT
                );
            }

            Order.OrderStatus fromStatus = order.getStatus();
            stateMachine.transition(order, request.getStatus());

            // Handle specific transitions
            if (request.getStatus() == Order.OrderStatus.SELLER_CONFIRMED) {
                // Set payment expiry
                order.setExpiresAt(Instant.now().plus(paymentTimeoutMinutes, ChronoUnit.MINUTES));
            } else if (request.getStatus() == Order.OrderStatus.CANCELLED_BY_BUYER) {
                order.setCancelledAt(Instant.now());
                order.setCancelledBy(actorType);
                order.setCancellationReason(request.getReason());
            }

            order = orderRepository.save(order);

            logOrderHistory(orderId, fromStatus, request.getStatus(), actorType, actorId, request.getReason());

            log.info("Order {} status changed from {} to {} by {}",
                    orderId, fromStatus, request.getStatus(), actorType);

            return mapToDto(order);

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException(
                    "Order has been modified. Please refresh and try again.",
                    "OPTIMISTIC_LOCK_FAILURE",
                    HttpStatus.CONFLICT
            );
        }
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("Order", orderId));
        return mapToDto(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getBuyerOrders(UUID buyerId, Pageable pageable) {
        return orderRepository.findByBuyerId(buyerId, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getSellerOrders(UUID sellerId, Pageable pageable) {
        return orderRepository.findBySellerId(sellerId, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getSellerOrdersByStatus(UUID sellerId, Order.OrderStatus status, Pageable pageable) {
        return orderRepository.findBySellerIdAndStatus(sellerId, status, pageable).map(this::mapToDto);
    }

    private Order getOrderForBuyer(UUID orderId, UUID buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("Order", orderId));

        if (!order.getBuyerId().equals(buyerId)) {
            throw BusinessException.forbidden("You don't have access to this order");
        }

        return order;
    }

    private void logOrderHistory(UUID orderId, Order.OrderStatus from, Order.OrderStatus to,
                                 String actorType, UUID actorId, String reason) {
        OrderHistory history = OrderHistory.builder()
                .orderId(orderId)
                .fromStatus(from)
                .toStatus(to)
                .changedByType(actorType)
                .changedById(actorId)
                .reason(reason)
                .build();
        orderHistoryRepository.save(history);
    }

    private OrderDto mapToDto(Order order) {
        List<OrderDto.OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> OrderDto.OrderItemDto.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName((String) item.getProductSnapshot().get("name"))
                        .productSku((String) item.getProductSnapshot().get("sku"))
                        .productSnapshot(item.getProductSnapshot())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .discountPercent(item.getDiscountPercent())
                        .discountAmount(item.getDiscountAmount())
                        .gstPercent(item.getGstPercent())
                        .gstAmount(item.getGstAmount())
                        .lineTotal(item.getLineTotal())
                        .deliveredQuantity(item.getDeliveredQuantity())
                        .status(item.getStatus().name())
                        .build())
                .toList();

        Set<Order.OrderStatus> nextStates = stateMachine.getValidNextStates(order.getStatus());

        return OrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .buyerId(order.getBuyerId())
                .sellerId(order.getSellerId())
                .type(order.getType())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .gstAmount(order.getGstAmount())
                .deliveryCharge(order.getDeliveryCharge())
                .totalAmount(order.getTotalAmount())
                .platformFee(order.getPlatformFee())
                .deliveryDate(order.getDeliveryDate())
                .deliverySlot(order.getDeliverySlot())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryInstructions(order.getDeliveryInstructions())
                .items(itemDtos)
                .priceSnapshot(order.getPriceSnapshot())
                .version(order.getVersion())
                .expiresAt(order.getExpiresAt())
                .cancelledAt(order.getCancelledAt())
                .cancelledBy(order.getCancelledBy())
                .cancellationReason(order.getCancellationReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .allowedNextStates(new ArrayList<>(nextStates))
                .canCancel(stateMachine.canCancel(order.getStatus()))
                .build();
    }
}
