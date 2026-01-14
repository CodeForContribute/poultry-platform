package com.poultry.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig
{

  @Bean
  public PostgreSQLContainer<?> postgresContainer()
  {
    PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    container.start();

    // Set system properties for Spring to pick up
    System.setProperty("spring.datasource.url", container.getJdbcUrl());
    System.setProperty("spring.datasource.username", container.getUsername());
    System.setProperty("spring.datasource.password", container.getPassword());

    return container;
  }

  @Bean
  public KafkaContainer kafkaContainer()
  {
    KafkaContainer container = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
    container.start();

    // Set system property for Spring to pick up
    System.setProperty("spring.kafka.bootstrap-servers", container.getBootstrapServers());

    return container;
  }
}
