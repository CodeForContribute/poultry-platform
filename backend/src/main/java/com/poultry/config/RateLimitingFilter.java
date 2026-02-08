package com.poultry.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting filter using Bucket4j token bucket algorithm.
 * Limits requests per client (identified by IP or client ID header).
 * Uses Caffeine cache with automatic expiration to prevent memory leaks.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingConfig config;

    // Caffeine cache with automatic expiration to prevent memory leaks
    private Cache<String, Bucket> bucketCache;

    @PostConstruct
    public void init() {
        // Initialize Caffeine cache with size limit and time-based expiration
        bucketCache = Caffeine.newBuilder()
                .maximumSize(50000)  // Max 50k entries
                .expireAfterAccess(10, TimeUnit.MINUTES)  // Expire after 10 min of inactivity
                .recordStats()  // Enable statistics for monitoring
                .build();
        log.info("Rate limiting cache initialized with max size: 50000, expiry: 10 minutes");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!config.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);

        // Check whitelist
        if (isWhitelisted(clientIp)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip rate limiting for health checks and swagger
        String path = request.getRequestURI();
        if (path.startsWith("/actuator/health") || path.startsWith("/actuator/info") ||
            path.contains("/swagger") || path.contains("/api-docs") || path.contains("/webjars")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = getClientId(request, clientIp);
        int limit = getRequestLimit(path);

        Bucket bucket = getBucket(clientId, limit);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Request allowed
            if (config.isIncludeHeaders()) {
                addRateLimitHeaders(response, probe, limit);
            }
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for client: {}, path: {}", clientId, path);

            if (config.isIncludeHeaders()) {
                addRateLimitHeaders(response, probe, limit);
            }

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                    "{\"error\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests. Please retry after %d seconds.\",\"retryAfter\":%d}",
                    probe.getNanosToWaitForRefill() / 1_000_000_000,
                    probe.getNanosToWaitForRefill() / 1_000_000_000
            ));
        }

    }

    private String getClientIp(HttpServletRequest request) {
        // Check for forwarded headers (when behind a proxy/load balancer)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in the chain (original client)
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private String getClientId(HttpServletRequest request, String clientIp) {
        // First try to get client ID from custom header
        String clientIdHeader = request.getHeader(config.getClientIdHeader());
        if (clientIdHeader != null && !clientIdHeader.isEmpty()) {
            return "header:" + clientIdHeader;
        }

        // Try to get user ID from JWT token (if authenticated)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Use a hash of the token to identify the user
            return "token:" + Integer.toHexString(authHeader.hashCode());
        }

        // Fall back to IP address
        return "ip:" + clientIp;
    }

    private int getRequestLimit(String path) {
        // Check custom limits first
        for (Map.Entry<String, Integer> entry : config.getCustomLimits().entrySet()) {
            if (pathMatches(path, entry.getKey())) {
                return entry.getValue();
            }
        }

        // Apply endpoint-specific limits
        if (path.startsWith("/v1/auth")) {
            return config.getAuthRequestsPerMinute();
        }
        if (path.startsWith("/v1/admin")) {
            return config.getAdminRequestsPerMinute();
        }
        if (path.contains("/webhook")) {
            return config.getWebhookRequestsPerMinute();
        }

        return config.getDefaultRequestsPerMinute();
    }

    private boolean pathMatches(String path, String pattern) {
        // Simple pattern matching (supports ** wildcard at end)
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return path.equals(pattern) || path.startsWith(pattern + "/");
    }

    private Bucket getBucket(String clientId, int requestsPerMinute) {
        return bucketCache.get(clientId, k -> createBucket(requestsPerMinute));
    }

    private Bucket createBucket(int requestsPerMinute) {
        // Calculate burst capacity
        int burstCapacity = (int) (requestsPerMinute * config.getBurstMultiplier());

        Bandwidth limit = Bandwidth.builder()
                .capacity(burstCapacity)
                .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private boolean isWhitelisted(String clientIp) {
        return Arrays.asList(config.getWhitelistedIps()).contains(clientIp);
    }

    private void addRateLimitHeaders(HttpServletResponse response, ConsumptionProbe probe, int limit) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));

        if (!probe.isConsumed()) {
            response.setHeader("X-RateLimit-Reset", String.valueOf(
                    System.currentTimeMillis() / 1000 + probe.getNanosToWaitForRefill() / 1_000_000_000));
            response.setHeader("Retry-After", String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
        }
    }
}
