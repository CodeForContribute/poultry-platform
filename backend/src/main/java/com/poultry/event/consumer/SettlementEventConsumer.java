package com.poultry.event.consumer;

import com.poultry.cache.CacheService;
import com.poultry.config.KafkaConfig;
import com.poultry.event.SettlementEvent;
import com.poultry.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementEventConsumer
{

  @KafkaListener(topics = KafkaConfig.SETTLEMENT_EVENTS_TOPIC, groupId = "settlement-events-consumer")
  public void handleSettlementEvent(SettlementEvent event)
  {
    log.info("Received settlement event: {} for settlement {}", event.getEventType(), event.getSettlementId());

    try
    {
      switch (event.getEventType())
      {
        case "CREATED" -> handleSettlementCreated(event);
        case "APPROVED" -> handleSettlementApproved(event);
        case "PROCESSING" -> handleSettlementProcessing(event);
        case "SUCCESS" -> handleSettlementSuccess(event);
        case "FAILED" -> handleSettlementFailed(event);
        default -> log.warn("Unknown settlement event type: {}", event.getEventType());
      }

      // Evict dashboard cache on settlement events
      cacheService.evictDashboardCache();

    }
    catch (Exception e)
    {
      log.error("Error processing settlement event {} for settlement {}: {}",
                event.getEventType(), event.getSettlementId(), e.getMessage(), e);
      throw e;
    }
  }

  private void handleSettlementCreated(SettlementEvent event)
  {
    log.info("Processing settlement created event for seller {}", event.getSellerId());
    // Settlement created, pending approval
    notificationService.sendSettlementNotification(
        event.getSellerId(),
        "SETTLEMENT_CREATED",
        event.getAmount()
    );
  }

  private void handleSettlementApproved(SettlementEvent event)
  {
    log.info("Processing settlement approved event for seller {}", event.getSellerId());
    notificationService.sendSettlementNotification(
        event.getSellerId(),
        "SETTLEMENT_APPROVED",
        event.getAmount()
    );
  }

  private void handleSettlementProcessing(SettlementEvent event)
  {
    log.info("Processing settlement processing event for seller {}", event.getSellerId());
    notificationService.sendSettlementNotification(
        event.getSellerId(),
        "PAYOUT_INITIATED",
        event.getAmount()
    );
  }

  private void handleSettlementSuccess(SettlementEvent event)
  {
    log.info("Processing settlement success event for seller {}", event.getSellerId());
    notificationService.sendSettlementNotification(
        event.getSellerId(),
        "PAYOUT_COMPLETED",
        event.getAmount()
    );
  }

  private void handleSettlementFailed(SettlementEvent event)
  {
    log.info("Processing settlement failed event for seller {}", event.getSellerId());
    notificationService.sendSettlementNotification(
        event.getSellerId(),
        "PAYOUT_FAILED",
        event.getAmount()
    );
  }
  private final NotificationService notificationService;
  private final CacheService cacheService;
}
