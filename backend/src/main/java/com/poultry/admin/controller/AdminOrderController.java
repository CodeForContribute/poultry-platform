package com.poultry.admin.controller;

import com.poultry.admin.dto.OrderListDto;
import com.poultry.admin.service.AdminOrderService;
import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
import com.poultry.order.entity.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Orders", description = "Admin order management APIs")
public class AdminOrderController
{

  @GetMapping
  @Operation(summary = "List orders", description = "Get paginated list of orders with filters")
  public ResponseEntity<ApiResponse<Page<OrderListDto>>> getOrders(
      @Parameter(description = "Search by order number")
      @RequestParam(required = false)
      String search,
      @Parameter(description = "Filter by status")
      @RequestParam(required = false)
      String status,
      @Parameter(description = "Filter by seller")
      @RequestParam(required = false)
      UUID sellerId,
      @Parameter(description = "Filter by buyer")
      @RequestParam(required = false)
      UUID buyerId,
      @Parameter(description = "Filter by start date")
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      Instant startDate,
      @Parameter(description = "Filter by end date")
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      Instant endDate,
      @PageableDefault(size = 20)
      Pageable pageable)
  {

    Page<OrderListDto> orders = orderService.getOrders(search, status, sellerId, buyerId, startDate, endDate, pageable);
    return ResponseEntity.ok(ApiResponse.success(orders));
  }

  @GetMapping("/{orderId}")
  @Operation(summary = "Get order details", description = "Get detailed order information")
  public ResponseEntity<ApiResponse<Order>> getOrder(
      @PathVariable
      UUID orderId)
  {

    Order order = orderService.getOrderById(orderId);
    return ResponseEntity.ok(ApiResponse.success(order));
  }

  @PostMapping("/{orderId}/cancel")
  @Operation(summary = "Cancel order", description = "Admin cancel an order")
  public ResponseEntity<ApiResponse<Order>> cancelOrder(
      @PathVariable
      UUID orderId,
      @RequestParam
      String reason,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    Order order = orderService.cancelOrder(orderId, reason, principal.getId());
    return ResponseEntity.ok(ApiResponse.success(order, "Order cancelled"));
  }

  @PostMapping("/{orderId}/refund")
  @Operation(summary = "Initiate refund", description = "Initiate refund for an order")
  public ResponseEntity<ApiResponse<Order>> initiateRefund(
      @PathVariable
      UUID orderId,
      @RequestParam
      String reason,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    Order order = orderService.initiateRefund(orderId, reason, principal.getId());
    return ResponseEntity.ok(ApiResponse.success(order, "Refund initiated"));
  }

  @PostMapping("/{orderId}/resolve-dispute")
  @Operation(summary = "Resolve dispute", description = "Resolve a dispute on an order")
  public ResponseEntity<ApiResponse<Order>> resolveDispute(
      @PathVariable
      UUID orderId,
      @RequestParam
      String resolution,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    Order order = orderService.resolveDispute(orderId, resolution, principal.getId());
    return ResponseEntity.ok(ApiResponse.success(order, "Dispute resolved"));
  }

  @PatchMapping("/{orderId}/status")
  @Operation(summary = "Force update status", description = "Force update order status (use with caution)")
  public ResponseEntity<ApiResponse<Order>> forceUpdateStatus(
      @PathVariable
      UUID orderId,
      @RequestParam
      Order.OrderStatus status,
      @RequestParam
      String reason,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    Order order = orderService.forceUpdateStatus(orderId, status, reason, principal.getId());
    return ResponseEntity.ok(ApiResponse.success(order, "Order status updated"));
  }

  private final AdminOrderService orderService;
}
