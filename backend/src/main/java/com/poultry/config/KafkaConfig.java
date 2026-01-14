package com.poultry.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for producers and consumers.
 * Configures JSON serialization/deserialization and error handling with retry.
 */
@Slf4j
@Configuration
@EnableKafka
public class KafkaConfig
{

  /**
   * Producer factory configuration with JSON serializer
   */
  @Bean
  public ProducerFactory<String, Object> producerFactory()
  {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    configProps.put(ProducerConfig.ACKS_CONFIG, "all");
    configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
    configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
    return new DefaultKafkaProducerFactory<>(configProps);
  }

  /**
   * KafkaTemplate for sending messages
   */
  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate()
  {
    return new KafkaTemplate<>(producerFactory());
  }

  /**
   * Consumer factory configuration with JSON deserializer
   */
  @Bean
  public ConsumerFactory<String, Object> consumerFactory()
  {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.poultry.*");
    configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
    return new DefaultKafkaConsumerFactory<>(configProps);
  }

  /**
   * Default error handler with retry logic.
   * Retries 3 times with 1 second interval before moving to error handling.
   */
  @Bean
  public DefaultErrorHandler errorHandler()
  {
    // Retry 3 times with 1 second interval
    FixedBackOff fixedBackOff = new FixedBackOff(1000L, 3L);

    DefaultErrorHandler errorHandler = new DefaultErrorHandler((record, exception) ->
                                                               {
                                                                 // Log failed message after all retries exhausted
                                                                 log.error("Error processing Kafka message after retries. Topic: {}, Partition: {}, Offset: {}, Key: {}",
                                                                           record.topic(),
                                                                           record.partition(),
                                                                           record.offset(),
                                                                           record.key(),
                                                                           exception);
                                                               }, fixedBackOff);

    // Don't retry for these exceptions
    errorHandler.addNotRetryableExceptions(
        IllegalArgumentException.class,
        NullPointerException.class
    );

    return errorHandler;
  }

  /**
   * Kafka listener container factory with error handling and manual acknowledgment
   */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory()
  {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.setCommonErrorHandler(errorHandler());

    // Configure container properties
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

    // Set concurrency for parallel processing
    factory.setConcurrency(3);

    return factory;
  }
  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;
  @Value("${spring.kafka.consumer.group-id}")
  private String groupId;
  /**
   * Topic names as constants
   */
  public static final String ORDER_EVENTS_TOPIC = "order-events";
  public static final String PAYMENT_EVENTS_TOPIC = "payment-events";
  public static final String DELIVERY_EVENTS_TOPIC = "delivery-events";
  public static final String SETTLEMENT_EVENTS_TOPIC = "settlement-events";
}
