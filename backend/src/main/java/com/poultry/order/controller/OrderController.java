package com.poultry.order.controller;

import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
import com.poultry.order.dto.CreateOrderRequest;
import com.poultry.order.dto.OrderDto;
import com.poultry.order.dto.UpdateOrderStatusRequest;
import com.poultry.order.entity.Order;
import com.poultry.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management APIs")
public class OrderController {

    private final OrderService orderService;

    // ============ Buyer Endpoints ============

    @PostMapping("/buyer/orders")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Create order", description = "Create a new order (buyer only)")
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request) {

        OrderDto order = orderService.createOrder(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Order created successfully"));
    }

    @PostMapping("/buyer/orders/{orderId}/place")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Place order", description = "Submit draft order for seller confirmation")
    public ResponseEntity<ApiResponse<OrderDto>> placeOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderId) {

        OrderDto order = orderService.placeOrder(principal.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success(order, "Order placed successfully"));
    }

    @PostMapping("/buyer/orders/{orderId}/cancel")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Cancel order", description = "Cancel an order (before dispatch only)")
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderId,
            @RequestParam(required = false) String reason) {

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(Order.OrderStatus.CANCELLED_BY_BUYER)
                .reason(reason)
                .build();

        OrderDto order = orderService.updateOrderStatus(
                orderId, request, "BUYER", principal.getId());
        return ResponseEntity.ok(ApiResponse.success(order, "Order cancelled successfully"));
    }

    @GetMapping("/buyer/orders")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Get buyer's orders", description = "Get all orders for the buyer")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getBuyerOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {

        Page<OrderDto> orders = orderService.getBuyerOrders(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/buyer/orders/{orderId}")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Get order details", description = "Get order details for buyer")
    public ResponseEntity<ApiResponse<OrderDto>> getBuyerOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderId) {

        OrderDto order = orderService.getOrder(orderId);
        // Verify buyer owns this order
        if (!order.getBuyerId().equals(principal.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied", "FORBIDDEN"));
        }
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    // ============ Seller Endpoints ============

    @GetMapping("/seller/orders")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get seller's orders", description = "Get all orders for the seller")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getSellerOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Order.OrderStatus status,
            Pageable pageable) {

        Page<OrderDto> orders;
        if (status != null) {
            orders = orderService.getSellerOrdersByStatus(principal.getSellerId(), status, pageable);
        } else {
            orders = orderService.getSellerOrders(principal.getSellerId(), pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/seller/orders/{orderId}")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get order details", description = "Get order details for seller")
    public ResponseEntity<ApiResponse<OrderDto>> getSellerOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderId) {

        OrderDto order = orderService.getOrder(orderId);
        // Verify seller owns this order
        if (!order.getSellerId().equals(principal.getSellerId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied", "FORBIDDEN"));
        }
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/seller/orders/{orderId}/confirm")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Confirm order", description = "Confirm a placed order")
    public ResponseEntity<ApiResponse<OrderDto>> confirmOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderId) {

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(Order.OrderStatus.SELLER_CONFIRMED)
                .build();

        OrderDto order = orderService.updateOrderStatus(
                orderId, request, "SELLER", principal.getId());
        return ResponseEntity.ok(ApiResponse.success(order, "Order confirmed"));
    }

    @PostMapping("/seller/orders/{orderId}/reject")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Reject order", description = "Reject a placed order")
    public ResponseEntity<ApiResponse<OrderDto>> rejectOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderId,
            @RequestParam String reason) {

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(Order.OrderStatus.SELLER_REJECTED)
                .reason(reason)
                .build();

        OrderDto order = orderService.updateOrderStatus(
                orderId, request, "SELLER", principal.getId());
        return ResponseEntity.ok(ApiResponse.success(order, "Order rejected"));
    }

    @PostMapping("/seller/orders/{orderId}/dispatch")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Mark dispatched", description = "Mark order as dispatched")
    public ResponseEntity<ApiResponse<OrderDto>> dispatchOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderId) {

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(Order.OrderStatus.DISPATCHED)
                .build();

        OrderDto order = orderService.updateOrderStatus(
                orderId, request, "SELLER", principal.getId());
        return ResponseEntity.ok(ApiResponse.success(order, "Order dispatched"));
    }

    @PostMapping("/seller/orders/{orderId}/deliver")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Mark delivered", description = "Mark order as delivered")
    public ResponseEntity<ApiResponse<OrderDto>> markDelivered(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderId) {

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(Order.OrderStatus.DELIVERED)
                .build();

        OrderDto order = orderService.updateOrderStatus(
                orderId, request, "SELLER", principal.getId());
        return ResponseEntity.ok(ApiResponse.success(order, "Order marked as delivered"));
    }
}
