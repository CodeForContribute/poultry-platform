package com.poultry.event.consumer;

import com.poultry.cache.CacheService;
import com.poultry.config.KafkaConfig;
import com.poultry.event.PaymentEvent;
import com.poultry.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer
{

  @KafkaListener(topics = KafkaConfig.PAYMENT_EVENTS_TOPIC, groupId = "payment-events-consumer")
  public void handlePaymentEvent(PaymentEvent event)
  {
    log.info("Received payment event: {} for payment {}", event.getEventType(), event.getPaymentId());

    try
    {
      switch (event.getEventType())
      {
        case "CREATED" -> handlePaymentCreated(event);
        case "SUCCESS" -> handlePaymentSuccess(event);
        case "FAILED" -> handlePaymentFailed(event);
        case "REFUNDED" -> handlePaymentRefunded(event);
        default -> log.warn("Unknown payment event type: {}", event.getEventType());
      }

      // Evict dashboard cache on payment events
      cacheService.evictDashboardCache();

    }
    catch (Exception e)
    {
      log.error("Error processing payment event {} for payment {}: {}",
                event.getEventType(), event.getPaymentId(), e.getMessage(), e);
      throw e;
    }
  }

  private void handlePaymentCreated(PaymentEvent event)
  {
    log.info("Processing payment created event for order {}", event.getOrderId());
    // Payment created, waiting for completion
  }

  private void handlePaymentSuccess(PaymentEvent event)
  {
    log.info("Processing payment success event for order {}", event.getOrderId());
    // Notify buyer about successful payment
    notificationService.sendPaymentNotification(
        event.getBuyerId(),
        "PAYMENT_SUCCESS",
        event.getAmount(),
        event.getGatewayReference()
    );
  }

  private void handlePaymentFailed(PaymentEvent event)
  {
    log.info("Processing payment failed event for order {}", event.getOrderId());
    // Notify buyer about failed payment
    notificationService.sendPaymentNotification(
        event.getBuyerId(),
        "PAYMENT_FAILED",
        event.getAmount(),
        event.getGatewayReference()
    );
  }

  private void handlePaymentRefunded(PaymentEvent event)
  {
    log.info("Processing payment refunded event for order {}", event.getOrderId());
    // Notify buyer about refund
    notificationService.sendPaymentNotification(
        event.getBuyerId(),
        "REFUND_PROCESSED",
        event.getAmount(),
        event.getGatewayReference()
    );
  }
  private final NotificationService notificationService;
  private final CacheService cacheService;
}
