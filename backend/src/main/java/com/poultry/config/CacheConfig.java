package com.poultry.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis caching configuration for the poultry platform.
 * Configures cache managers with specific TTL settings for different cache regions.
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig
    implements CachingConfigurer
{

  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory)
  {
    log.info("Initializing Redis cache manager");

    // Configure Jackson ObjectMapper for Redis serialization
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.activateDefaultTyping(
        LaissezFaireSubTypeValidator.instance,
        ObjectMapper.DefaultTyping.NON_FINAL,
        JsonTypeInfo.As.PROPERTY
    );

    GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

    // Default cache configuration with 10-minute TTL
    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                                                   .entryTtl(Duration.ofMinutes(10))
                                                                   .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                                                                   .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                                                                   .disableCachingNullValues();

    // Cache-specific configurations with different TTLs
    Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

    // Products cache - 5 minute TTL (frequently updated inventory)
    cacheConfigs.put(PRODUCTS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));

    // Sellers cache - 15 minute TTL (seller info changes less frequently)
    cacheConfigs.put(SELLERS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(15)));

    // Pricing cache - 2 minute TTL (prices need to be fresh)
    cacheConfigs.put(PRICING_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(2)));

    // Categories cache - 1 hour TTL (rarely changes)
    cacheConfigs.put(CATEGORIES_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)));

    // Dashboard cache - 5 minute TTL (aggregate metrics)
    cacheConfigs.put(DASHBOARD_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));

    // Seller ratings cache - 30 minute TTL (ratings aggregated periodically)
    cacheConfigs.put(SELLER_RATINGS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)));

    // Sessions cache - 30 minute TTL (user sessions)
    cacheConfigs.put(SESSIONS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)));

    // Rate limiting cache - 1 hour TTL (rate limit windows)
    cacheConfigs.put(RATE_LIMIT_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)));

    RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                                                      .cacheDefaults(defaultConfig)
                                                      .withInitialCacheConfigurations(cacheConfigs)
                                                      .transactionAware()
                                                      .build();

    log.info("Redis cache manager initialized with {} cache configurations", cacheConfigs.size());
    return cacheManager;
  }

  /**
   * RedisTemplate for direct Redis operations.
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory)
  {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // Configure serializers
    StringRedisSerializer stringSerializer = new StringRedisSerializer();
    GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

    template.setKeySerializer(stringSerializer);
    template.setHashKeySerializer(stringSerializer);
    template.setValueSerializer(jsonSerializer);
    template.setHashValueSerializer(jsonSerializer);
    template.setEnableTransactionSupport(true);

    template.afterPropertiesSet();

    log.info("RedisTemplate configured with JSON serialization");
    return template;
  }

  /**
   * Custom cache error handler that logs errors but allows application to continue.
   * This ensures cache failures don't break the application.
   */
  @Override
  public CacheErrorHandler errorHandler()
  {
    return new CustomCacheErrorHandler();
  }

  /**
   * Custom error handler for graceful cache failure handling.
   */
  private static class CustomCacheErrorHandler
      extends SimpleCacheErrorHandler
  {

    @Override
    public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key)
    {
      log.warn("Cache GET error for cache '{}', key '{}': {}",
               cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value)
    {
      log.warn("Cache PUT error for cache '{}', key '{}': {}",
               cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key)
    {
      log.warn("Cache EVICT error for cache '{}', key '{}': {}",
               cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache)
    {
      log.warn("Cache CLEAR error for cache '{}': {}",
               cache.getName(), exception.getMessage());
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomCacheErrorHandler.class);
  }
  /**
   * Cache name constants for consistent usage across the application.
   */
  public static final String PRODUCTS_CACHE = "products";
  public static final String SELLERS_CACHE = "sellers";
  public static final String PRICING_CACHE = "pricing";
  public static final String CATEGORIES_CACHE = "categories";
  public static final String DASHBOARD_CACHE = "dashboard";
  public static final String SELLER_RATINGS_CACHE = "sellerRatings";
  public static final String SESSIONS_CACHE = "sessions";
  public static final String RATE_LIMIT_CACHE = "rateLimit";
}
