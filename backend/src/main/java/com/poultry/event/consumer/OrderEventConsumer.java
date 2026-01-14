package com.poultry.event.consumer;

import com.poultry.cache.CacheService;
import com.poultry.config.KafkaConfig;
import com.poultry.event.OrderEvent;
import com.poultry.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer
{

  @KafkaListener(topics = KafkaConfig.ORDER_EVENTS_TOPIC, groupId = "order-events-consumer")
  public void handleOrderEvent(OrderEvent event)
  {
    log.info("Received order event: {} for order {}", event.getEventType(), event.getOrderId());

    try
    {
      switch (event.getEventType())
      {
        case "CREATED" -> handleOrderCreated(event);
        case "CONFIRMED" -> handleOrderConfirmed(event);
        case "PAID" -> handleOrderPaid(event);
        case "DISPATCHED" -> handleOrderDispatched(event);
        case "DELIVERED" -> handleOrderDelivered(event);
        case "CANCELLED" -> handleOrderCancelled(event);
        default -> log.warn("Unknown order event type: {}", event.getEventType());
      }

      // Evict dashboard cache on any order status change
      cacheService.evictDashboardCache();

    }
    catch (Exception e)
    {
      log.error("Error processing order event {} for order {}: {}",
                event.getEventType(), event.getOrderId(), e.getMessage(), e);
      throw e; // Re-throw for Kafka error handling
    }
  }

  private void handleOrderCreated(OrderEvent event)
  {
    log.info("Processing order created event for order {}", event.getOrderId());
    // Notify seller about new order
    notificationService.sendOrderNotification(
        event.getSellerId(),
        "SELLER",
        "NEW_ORDER",
        event.getOrderNumber(),
        event.getAmount()
    );
  }

  private void handleOrderConfirmed(OrderEvent event)
  {
    log.info("Processing order confirmed event for order {}", event.getOrderId());
    // Notify buyer about order confirmation
    notificationService.sendOrderNotification(
        event.getBuyerId(),
        "BUYER",
        "ORDER_CONFIRMED",
        event.getOrderNumber(),
        event.getAmount()
    );
  }

  private void handleOrderPaid(OrderEvent event)
  {
    log.info("Processing order paid event for order {}", event.getOrderId());
    // Notify seller about payment received
    notificationService.sendOrderNotification(
        event.getSellerId(),
        "SELLER",
        "PAYMENT_RECEIVED",
        event.getOrderNumber(),
        event.getAmount()
    );
  }

  private void handleOrderDispatched(OrderEvent event)
  {
    log.info("Processing order dispatched event for order {}", event.getOrderId());
    // Notify buyer about dispatch
    notificationService.sendOrderNotification(
        event.getBuyerId(),
        "BUYER",
        "ORDER_DISPATCHED",
        event.getOrderNumber(),
        event.getAmount()
    );
  }

  private void handleOrderDelivered(OrderEvent event)
  {
    log.info("Processing order delivered event for order {}", event.getOrderId());
    // Notify both parties about successful delivery
    notificationService.sendOrderNotification(
        event.getBuyerId(),
        "BUYER",
        "ORDER_DELIVERED",
        event.getOrderNumber(),
        event.getAmount()
    );
    notificationService.sendOrderNotification(
        event.getSellerId(),
        "SELLER",
        "ORDER_DELIVERED",
        event.getOrderNumber(),
        event.getAmount()
    );
  }

  private void handleOrderCancelled(OrderEvent event)
  {
    log.info("Processing order cancelled event for order {}", event.getOrderId());
    // Notify both parties about cancellation
    notificationService.sendOrderNotification(
        event.getBuyerId(),
        "BUYER",
        "ORDER_CANCELLED",
        event.getOrderNumber(),
        event.getAmount()
    );
    notificationService.sendOrderNotification(
        event.getSellerId(),
        "SELLER",
        "ORDER_CANCELLED",
        event.getOrderNumber(),
        event.getAmount()
    );
  }
  private final NotificationService notificationService;
  private final CacheService cacheService;
}
