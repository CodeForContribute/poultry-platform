package com.poultry.cache;

import com.poultry.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing cache operations across the platform.
 * Provides methods for cache eviction, retrieval, and manual cache management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService
{

  /**
   * Evicts a specific product from the products cache.
   *
   * @param productId the product ID to evict
   */
  public void evictProductCache(UUID productId)
  {
    evictCache(CacheConfig.PRODUCTS_CACHE, productId.toString());
    log.debug("Evicted product cache for productId: {}", productId);
  }

  /**
   * Evicts a specific seller from the sellers cache.
   *
   * @param sellerId the seller ID to evict
   */
  public void evictSellerCache(UUID sellerId)
  {
    evictCache(CacheConfig.SELLERS_CACHE, sellerId.toString());
    // Also evict seller ratings
    evictCache(CacheConfig.SELLER_RATINGS_CACHE, sellerId.toString());
    log.debug("Evicted seller cache for sellerId: {}", sellerId);
  }

  /**
   * Evicts pricing cache for a specific product.
   *
   * @param productId the product ID to evict pricing for
   */
  public void evictPricingCache(UUID productId)
  {
    evictCache(CacheConfig.PRICING_CACHE, productId.toString());
    log.debug("Evicted pricing cache for productId: {}", productId);
  }

  /**
   * Evicts all product caches for a specific seller.
   * Uses pattern matching to find and delete all related keys.
   *
   * @param sellerId the seller ID whose products should be evicted
   */
  public void evictAllProductsForSeller(UUID sellerId)
  {
    String pattern = CacheConfig.PRODUCTS_CACHE + "::seller:" + sellerId + ":*";
    Set<String> keys = redisTemplate.keys(pattern);
    if (keys != null && !keys.isEmpty())
    {
      redisTemplate.delete(keys);
      log.info("Evicted {} product cache entries for seller: {}", keys.size(), sellerId);
    }

    // Also evict the seller's product list cache if it exists
    evictCache(CacheConfig.PRODUCTS_CACHE, "seller:" + sellerId);
  }

  /**
   * Evicts the dashboard cache.
   * Should be called when significant data changes occur.
   */
  public void evictDashboardCache()
  {
    evictAllCache(CacheConfig.DASHBOARD_CACHE);
    log.info("Dashboard cache evicted");
  }

  /**
   * Evicts a specific key from a cache.
   *
   * @param cacheName the name of the cache
   * @param key       the key to evict
   */
  public void evictCache(String cacheName, String key)
  {
    try
    {
      Cache cache = cacheManager.getCache(cacheName);
      if (cache != null)
      {
        cache.evict(key);
        log.debug("Evicted key '{}' from cache '{}'", key, cacheName);
      }
      else
      {
        log.warn("Cache '{}' not found", cacheName);
      }
    }
    catch (Exception e)
    {
      log.error("Error evicting cache '{}' key '{}': {}", cacheName, key, e.getMessage());
    }
  }

  /**
   * Evicts all entries from a specific cache.
   *
   * @param cacheName the name of the cache to clear
   */
  public void evictAllCache(String cacheName)
  {
    try
    {
      Cache cache = cacheManager.getCache(cacheName);
      if (cache != null)
      {
        cache.clear();
        log.info("Cleared all entries from cache '{}'", cacheName);
      }
      else
      {
        log.warn("Cache '{}' not found", cacheName);
      }
    }
    catch (Exception e)
    {
      log.error("Error clearing cache '{}': {}", cacheName, e.getMessage());
    }
  }

  /**
   * Retrieves a value from cache.
   *
   * @param cacheName the name of the cache
   * @param key       the key to retrieve
   * @param type      the expected type of the value
   * @param <T>       the type parameter
   *
   * @return the cached value, or null if not found
   */
  public <T> T getFromCache(String cacheName, String key, Class<T> type)
  {
    try
    {
      Cache cache = cacheManager.getCache(cacheName);
      if (cache != null)
      {
        Cache.ValueWrapper wrapper = cache.get(key);
        if (wrapper != null)
        {
          Object value = wrapper.get();
          if (type.isInstance(value))
          {
            log.debug("Cache HIT for cache '{}', key '{}'", cacheName, key);
            return type.cast(value);
          }
        }
      }
      log.debug("Cache MISS for cache '{}', key '{}'", cacheName, key);
      return null;
    }
    catch (Exception e)
    {
      log.warn("Error getting from cache '{}' key '{}': {}", cacheName, key, e.getMessage());
      return null;
    }
  }

  /**
   * Puts a value into cache.
   *
   * @param cacheName the name of the cache
   * @param key       the key to store under
   * @param value     the value to store
   */
  public void putInCache(String cacheName, String key, Object value)
  {
    try
    {
      Cache cache = cacheManager.getCache(cacheName);
      if (cache != null)
      {
        cache.put(key, value);
        log.debug("Cached value in '{}' with key '{}'", cacheName, key);
      }
      else
      {
        log.warn("Cache '{}' not found", cacheName);
      }
    }
    catch (Exception e)
    {
      log.error("Error putting in cache '{}' key '{}': {}", cacheName, key, e.getMessage());
    }
  }

  /**
   * Puts a value into cache with a custom TTL using RedisTemplate directly.
   *
   * @param key      the key to store under
   * @param value    the value to store
   * @param ttl      the time-to-live value
   * @param timeUnit the time unit for TTL
   */
  public void putWithTtl(String key, Object value, long ttl, TimeUnit timeUnit)
  {
    try
    {
      redisTemplate.opsForValue().set(key, value, ttl, timeUnit);
      log.debug("Cached value with key '{}' and TTL {} {}", key, ttl, timeUnit);
    }
    catch (Exception e)
    {
      log.error("Error setting value with TTL for key '{}': {}", key, e.getMessage());
    }
  }

  /**
   * Gets a value directly from Redis.
   *
   * @param key  the key to retrieve
   * @param type the expected type
   * @param <T>  the type parameter
   *
   * @return the value, or null if not found
   */
  public <T> T getFromRedis(String key, Class<T> type)
  {
    try
    {
      Object value = redisTemplate.opsForValue().get(key);
      if (type.isInstance(value))
      {
        return type.cast(value);
      }
      return null;
    }
    catch (Exception e)
    {
      log.warn("Error getting from Redis key '{}': {}", key, e.getMessage());
      return null;
    }
  }

  /**
   * Deletes a key directly from Redis.
   *
   * @param key the key to delete
   *
   * @return true if the key was deleted
   */
  public boolean deleteFromRedis(String key)
  {
    try
    {
      Boolean result = redisTemplate.delete(key);
      return Boolean.TRUE.equals(result);
    }
    catch (Exception e)
    {
      log.error("Error deleting Redis key '{}': {}", key, e.getMessage());
      return false;
    }
  }

  /**
   * Checks if a key exists in Redis.
   *
   * @param key the key to check
   *
   * @return true if the key exists
   */
  public boolean existsInRedis(String key)
  {
    try
    {
      Boolean result = redisTemplate.hasKey(key);
      return Boolean.TRUE.equals(result);
    }
    catch (Exception e)
    {
      log.warn("Error checking Redis key '{}': {}", key, e.getMessage());
      return false;
    }
  }

  /**
   * Evicts categories cache.
   * Should be called when category data is updated.
   */
  public void evictCategoriesCache()
  {
    evictAllCache(CacheConfig.CATEGORIES_CACHE);
    log.info("Categories cache evicted");
  }

  /**
   * Evicts all caches. Use with caution in production.
   */
  public void evictAllCaches()
  {
    Collection<String> cacheNames = cacheManager.getCacheNames();
    cacheNames.forEach(cacheName ->
                       {
                         Cache cache = cacheManager.getCache(cacheName);
                         if (cache != null)
                         {
                           cache.clear();
                         }
                       });
    log.warn("All caches evicted - {} cache regions cleared", cacheNames.size());
  }

  /**
   * Gets cache statistics for monitoring.
   *
   * @return a string representation of cache statistics
   */
  public String getCacheStats()
  {
    StringBuilder stats = new StringBuilder();
    Collection<String> cacheNames = cacheManager.getCacheNames();
    stats.append("Active caches: ").append(cacheNames.size()).append("\n");
    cacheNames.forEach(name -> stats.append("  - ").append(name).append("\n"));
    return stats.toString();
  }

  /**
   * Increments a counter in Redis atomically.
   * Useful for rate limiting.
   *
   * @param key the key to increment
   *
   * @return the new value after increment
   */
  public Long incrementCounter(String key)
  {
    try
    {
      return redisTemplate.opsForValue().increment(key);
    }
    catch (Exception e)
    {
      log.error("Error incrementing counter '{}': {}", key, e.getMessage());
      return null;
    }
  }

  /**
   * Increments a counter with expiry.
   * Useful for rate limiting with automatic reset.
   *
   * @param key      the key to increment
   * @param ttl      time-to-live
   * @param timeUnit time unit for TTL
   *
   * @return the new value after increment
   */
  public Long incrementCounterWithExpiry(String key, long ttl, TimeUnit timeUnit)
  {
    try
    {
      Long value = redisTemplate.opsForValue().increment(key);
      if (value != null && value == 1L)
      {
        // First increment, set expiry
        redisTemplate.expire(key, ttl, timeUnit);
      }
      return value;
    }
    catch (Exception e)
    {
      log.error("Error incrementing counter with expiry '{}': {}", key, e.getMessage());
      return null;
    }
  }
  private final CacheManager cacheManager;
  private final RedisTemplate<String, Object> redisTemplate;
}
