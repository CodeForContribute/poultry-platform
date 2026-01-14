package com.poultry.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for rate limiting.
 *
 * Different rate limits can be applied to different endpoint patterns:
 * - auth: Authentication endpoints (stricter limits)
 * - api: General API endpoints
 * - admin: Admin endpoints
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitingConfig {

    /**
     * Whether rate limiting is enabled
     */
    private boolean enabled = true;

    /**
     * Default requests per minute for general API endpoints
     */
    private int defaultRequestsPerMinute = 100;

    /**
     * Requests per minute for authentication endpoints (stricter)
     */
    private int authRequestsPerMinute = 20;

    /**
     * Requests per minute for admin endpoints
     */
    private int adminRequestsPerMinute = 200;

    /**
     * Requests per minute for webhook endpoints (more permissive)
     */
    private int webhookRequestsPerMinute = 500;

    /**
     * Whitelist of IPs that bypass rate limiting (e.g., internal services)
     */
    private String[] whitelistedIps = {};

    /**
     * Custom limits per endpoint pattern
     * Key: URL pattern (e.g., "/v1/orders/**")
     * Value: requests per minute
     */
    private Map<String, Integer> customLimits = new HashMap<>();

    /**
     * Header name for client identifier (defaults to Authorization header or IP)
     */
    private String clientIdHeader = "X-Client-Id";

    /**
     * Whether to include rate limit headers in response
     */
    private boolean includeHeaders = true;

    /**
     * Burst capacity multiplier (allows short bursts above the limit)
     */
    private double burstMultiplier = 1.5;
}
