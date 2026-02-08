package com.poultry.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Configuration and filter for HTTP security headers.
 *
 * Adds the following security headers to all responses:
 * - X-Content-Type-Options: Prevents MIME-sniffing attacks
 * - X-Frame-Options: Prevents clickjacking attacks
 * - X-XSS-Protection: Enables XSS filtering in browsers
 * - Strict-Transport-Security: Enforces HTTPS connections
 * - Content-Security-Policy: Restricts resource loading
 * - Referrer-Policy: Controls referrer information
 * - Permissions-Policy: Controls browser features
 * - Cache-Control: Prevents caching of sensitive data
 */
@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "security.headers")
public class SecurityHeadersConfig {

    /**
     * Whether security headers are enabled
     */
    private boolean enabled = true;

    /**
     * X-Frame-Options value (DENY, SAMEORIGIN, or ALLOW-FROM uri)
     */
    private String frameOptions = "DENY";

    /**
     * Whether to enable Strict-Transport-Security (HSTS)
     */
    private boolean hstsEnabled = true;

    /**
     * HSTS max-age in seconds (default: 1 year)
     */
    private long hstsMaxAge = 31536000;

    /**
     * Whether HSTS should include subdomains
     */
    private boolean hstsIncludeSubdomains = true;

    /**
     * Whether to include HSTS preload directive
     */
    private boolean hstsPreload = false;

    /**
     * Content-Security-Policy value
     */
    private String contentSecurityPolicy = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'; connect-src 'self'; frame-ancestors 'none';";

    /**
     * Referrer-Policy value
     */
    private String referrerPolicy = "strict-origin-when-cross-origin";

    /**
     * Permissions-Policy value (formerly Feature-Policy)
     */
    private String permissionsPolicy = "geolocation=(), microphone=(), camera=(), payment=()";

    /**
     * Whether to add Cache-Control headers for API responses
     */
    private boolean noCacheEnabled = true;

    /**
     * Filter component that adds security headers to responses
     */
    @Component
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public static class SecurityHeadersFilter extends OncePerRequestFilter {

        private final SecurityHeadersConfig config;

        public SecurityHeadersFilter(SecurityHeadersConfig config) {
            this.config = config;
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String path = request.getServletPath();
            return path.contains("/swagger") || path.contains("/api-docs") || path.contains("/webjars");
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            if (config.isEnabled()) {
                addSecurityHeaders(request, response);
            }

            filterChain.doFilter(request, response);
        }

        private void addSecurityHeaders(HttpServletRequest request, HttpServletResponse response) {
            // Prevent MIME-sniffing attacks
            response.setHeader("X-Content-Type-Options", "nosniff");

            // Prevent clickjacking
            response.setHeader("X-Frame-Options", config.getFrameOptions());

            // Enable XSS filtering (legacy but still useful for older browsers)
            response.setHeader("X-XSS-Protection", "1; mode=block");

            // HSTS - Force HTTPS connections
            if (config.isHstsEnabled()) {
                StringBuilder hsts = new StringBuilder();
                hsts.append("max-age=").append(config.getHstsMaxAge());
                if (config.isHstsIncludeSubdomains()) {
                    hsts.append("; includeSubDomains");
                }
                if (config.isHstsPreload()) {
                    hsts.append("; preload");
                }
                response.setHeader("Strict-Transport-Security", hsts.toString());
            }

            // Content Security Policy
            if (config.getContentSecurityPolicy() != null && !config.getContentSecurityPolicy().isEmpty()) {
                response.setHeader("Content-Security-Policy", config.getContentSecurityPolicy());
            }

            // Referrer Policy
            response.setHeader("Referrer-Policy", config.getReferrerPolicy());

            // Permissions Policy (formerly Feature-Policy)
            if (config.getPermissionsPolicy() != null && !config.getPermissionsPolicy().isEmpty()) {
                response.setHeader("Permissions-Policy", config.getPermissionsPolicy());
            }

            // Cache control for API endpoints (prevent caching of sensitive data)
            if (config.isNoCacheEnabled() && isApiEndpoint(request)) {
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
            }

            // Additional security headers
            response.setHeader("X-Permitted-Cross-Domain-Policies", "none");

            // Only add restrictive cross-origin headers for same-origin requests
            // Skip for CORS requests to allow cross-origin API access
            String origin = request.getHeader("Origin");
            if (origin == null || origin.isEmpty()) {
                response.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
                response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
                response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
            } else {
                // For cross-origin requests, use more permissive policy
                response.setHeader("Cross-Origin-Resource-Policy", "cross-origin");
            }
        }

        private boolean isApiEndpoint(HttpServletRequest request) {
            String path = request.getRequestURI();
            return path.startsWith("/v1/") || path.startsWith("/api/");
        }
    }
}
