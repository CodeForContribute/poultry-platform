package com.poultry.admin.service;

import com.poultry.admin.dto.OrderListDto;
import com.poultry.common.exception.BusinessException;
import com.poultry.order.entity.Order;
import com.poultry.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService
{

  @Transactional(readOnly = true)
  public Page<OrderListDto> getOrders(String search, String status, UUID sellerId, UUID buyerId,
                                      Instant startDate, Instant endDate, Pageable pageable)
  {
    log.info("Fetching orders with filters - status: {}, sellerId: {}, buyerId: {}", status, sellerId, buyerId);

    // Build query based on filters
    if (status != null && !status.isEmpty())
    {
      Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status);
      return orderRepository.findByStatus(orderStatus, pageable).map(this::toOrderListDto);
    }

    return orderRepository.findAll(pageable).map(this::toOrderListDto);
  }

  @Transactional(readOnly = true)
  public Order getOrderById(UUID orderId)
  {
    return orderRepository.findById(orderId)
                          .orElseThrow(() -> new BusinessException("Order not found", "ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));
  }

  @Transactional
  public Order cancelOrder(UUID orderId, String reason, UUID adminId)
  {
    log.info("Admin {} cancelling order {} with reason: {}", adminId, orderId, reason);

    Order order = getOrderById(orderId);

    // Validate order can be cancelled
    if (order.getStatus() == Order.OrderStatus.DELIVERED ||
        order.getStatus() == Order.OrderStatus.SETTLED ||
        order.getStatus() == Order.OrderStatus.REFUNDED)
    {
      throw new BusinessException("Order cannot be cancelled in current status", "INVALID_STATUS", HttpStatus.BAD_REQUEST);
    }

    order.setStatus(Order.OrderStatus.CANCELLED_BY_BUYER); // Using existing status
    order.setCancellationReason(reason);
    order = orderRepository.save(order);

    log.info("Order {} cancelled by admin {}", orderId, adminId);
    return order;
  }

  @Transactional
  public Order initiateRefund(UUID orderId, String reason, UUID adminId)
  {
    log.info("Admin {} initiating refund for order {} with reason: {}", adminId, orderId, reason);

    Order order = getOrderById(orderId);

    // Validate order status for refund
    if (order.getStatus() != Order.OrderStatus.PAID &&
        order.getStatus() != Order.OrderStatus.DISPATCHED &&
        order.getStatus() != Order.OrderStatus.DELIVERED)
    {
      throw new BusinessException("Order is not eligible for refund", "INVALID_STATUS", HttpStatus.BAD_REQUEST);
    }

    order.setStatus(Order.OrderStatus.REFUND_INITIATED);
    order = orderRepository.save(order);

    // TODO: Trigger actual refund process via payment service

    log.info("Refund initiated for order {} by admin {}", orderId, adminId);
    return order;
  }

  @Transactional
  public Order resolveDispute(UUID orderId, String resolution, UUID adminId)
  {
    log.info("Admin {} resolving dispute for order {} with resolution: {}", adminId, orderId, resolution);

    Order order = getOrderById(orderId);

    // Add dispute resolution logic here
    // For now, just log it
    log.info("Dispute resolved for order {} by admin {}: {}", orderId, adminId, resolution);

    return order;
  }

  @Transactional
  public Order forceUpdateStatus(UUID orderId, Order.OrderStatus newStatus, String reason, UUID adminId)
  {
    log.info("Admin {} force updating order {} to status {} with reason: {}", adminId, orderId, newStatus, reason);

    Order order = getOrderById(orderId);
    Order.OrderStatus oldStatus = order.getStatus();

    order.setStatus(newStatus);
    order = orderRepository.save(order);

    log.info("Order {} status force updated from {} to {} by admin {}", orderId, oldStatus, newStatus, adminId);
    return order;
  }

  private OrderListDto toOrderListDto(Order order)
  {
    return OrderListDto.builder()
                       .id(order.getId())
                       .orderNumber(order.getOrderNumber())
                       .sellerId(order.getSellerId())
                       .buyerId(order.getBuyerId())
                       .status(order.getStatus().name())
                       .orderType(order.getType() != null ? order.getType().name() : null)
                       .totalAmount(order.getTotalAmount())
                       .platformFee(order.getPlatformFee())
                       .createdAt(order.getCreatedAt())
                       .updatedAt(order.getUpdatedAt())
                       .build();
  }
  private final OrderRepository orderRepository;
}
