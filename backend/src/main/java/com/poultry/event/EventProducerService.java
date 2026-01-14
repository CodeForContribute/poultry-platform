package com.poultry.event;

import com.poultry.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProducerService
{

  public void publishOrderEvent(OrderEvent event)
  {
    log.info("Publishing order event: {} for order {}", event.getEventType(), event.getOrderId());
    sendMessage(KafkaConfig.ORDER_EVENTS_TOPIC, event.getOrderId().toString(), event);
  }

  public void publishPaymentEvent(PaymentEvent event)
  {
    log.info("Publishing payment event: {} for payment {}", event.getEventType(), event.getPaymentId());
    sendMessage(KafkaConfig.PAYMENT_EVENTS_TOPIC, event.getPaymentId().toString(), event);
  }

  public void publishDeliveryEvent(DeliveryEvent event)
  {
    log.info("Publishing delivery event: {} for delivery {}", event.getEventType(), event.getDeliveryId());
    sendMessage(KafkaConfig.DELIVERY_EVENTS_TOPIC, event.getDeliveryId().toString(), event);
  }

  public void publishSettlementEvent(SettlementEvent event)
  {
    log.info("Publishing settlement event: {} for settlement {}", event.getEventType(), event.getSettlementId());
    sendMessage(KafkaConfig.SETTLEMENT_EVENTS_TOPIC, event.getSettlementId().toString(), event);
  }

  private void sendMessage(String topic, String key, Object event)
  {
    CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

    future.whenComplete((result, ex) ->
                        {
                          if (ex != null)
                          {
                            log.error("Failed to send event to topic {}: {}", topic, ex.getMessage());
                          }
                          else
                          {
                            log.debug("Event sent to topic {} partition {} offset {}",
                                      topic,
                                      result.getRecordMetadata().partition(),
                                      result.getRecordMetadata().offset());
                          }
                        });
  }
  private final KafkaTemplate<String, Object> kafkaTemplate;
}
