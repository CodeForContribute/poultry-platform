package com.poultry.event.consumer;

import com.poultry.cache.CacheService;
import com.poultry.config.KafkaConfig;
import com.poultry.event.DeliveryEvent;
import com.poultry.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryEventConsumer
{

  @KafkaListener(topics = KafkaConfig.DELIVERY_EVENTS_TOPIC, groupId = "delivery-events-consumer")
  public void handleDeliveryEvent(DeliveryEvent event)
  {
    log.info("Received delivery event: {} for delivery {}", event.getEventType(), event.getDeliveryId());

    try
    {
      switch (event.getEventType())
      {
        case "ASSIGNED" -> handleDeliveryAssigned(event);
        case "PICKED_UP" -> handleDeliveryPickedUp(event);
        case "IN_TRANSIT" -> handleDeliveryInTransit(event);
        case "OUT_FOR_DELIVERY" -> handleOutForDelivery(event);
        case "DELIVERED" -> handleDeliveryCompleted(event);
        case "FAILED" -> handleDeliveryFailed(event);
        default -> log.warn("Unknown delivery event type: {}", event.getEventType());
      }

      // Evict dashboard cache on delivery events
      cacheService.evictDashboardCache();

    }
    catch (Exception e)
    {
      log.error("Error processing delivery event {} for delivery {}: {}",
                event.getEventType(), event.getDeliveryId(), e.getMessage(), e);
      throw e;
    }
  }

  private void handleDeliveryAssigned(DeliveryEvent event)
  {
    log.info("Processing delivery assigned event for order {}", event.getOrderId());
    notificationService.sendDeliveryNotification(
        event.getOrderId(),
        "DELIVERY_ASSIGNED",
        event.getStatus()
    );
  }

  private void handleDeliveryPickedUp(DeliveryEvent event)
  {
    log.info("Processing delivery picked up event for order {}", event.getOrderId());
    notificationService.sendDeliveryNotification(
        event.getOrderId(),
        "ORDER_PICKED_UP",
        event.getStatus()
    );
  }

  private void handleDeliveryInTransit(DeliveryEvent event)
  {
    log.info("Processing delivery in transit event for order {}", event.getOrderId());
    notificationService.sendDeliveryNotification(
        event.getOrderId(),
        "IN_TRANSIT",
        event.getStatus()
    );
  }

  private void handleOutForDelivery(DeliveryEvent event)
  {
    log.info("Processing out for delivery event for order {}", event.getOrderId());
    notificationService.sendDeliveryNotification(
        event.getOrderId(),
        "OUT_FOR_DELIVERY",
        event.getStatus()
    );
  }

  private void handleDeliveryCompleted(DeliveryEvent event)
  {
    log.info("Processing delivery completed event for order {}", event.getOrderId());
    notificationService.sendDeliveryNotification(
        event.getOrderId(),
        "DELIVERED",
        event.getStatus()
    );
  }

  private void handleDeliveryFailed(DeliveryEvent event)
  {
    log.info("Processing delivery failed event for order {}", event.getOrderId());
    notificationService.sendDeliveryNotification(
        event.getOrderId(),
        "DELIVERY_FAILED",
        event.getStatus()
    );
  }
  private final NotificationService notificationService;
  private final CacheService cacheService;
}
