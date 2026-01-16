package com.poultry.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.common.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String IDEMPOTENCY_KEY_HEADER_ALT = "X-Idempotency-Key";

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = getIdempotencyKey(request);
        if (key == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String endpoint = normalizeEndpoint(request);
        Instant now = Instant.now();

        Optional<IdempotencyKey> existingOpt = repository.findById(key);
        if (existingOpt.isPresent()) {
            IdempotencyKey existing = existingOpt.get();

            if (existing.isExpired(now)) {
                repository.delete(existing);
            } else {
                if (!existing.getEndpoint().equals(endpoint)) {
                    throw BusinessException.conflict("Idempotency key already used for a different endpoint");
                }
                if (existing.hasStoredResponse()) {
                    writeStoredResponse(existing, response);
                    return;
                }
                throw BusinessException.conflict("A request with this idempotency key is already being processed");
            }
        }

        IdempotencyKey created = IdempotencyKey.builder()
                .key(key)
                .endpoint(endpoint)
                .method("POST")
                .expiresAt(now.plus(DEFAULT_TTL))
                .build();

        try {
            repository.save(created);
        } catch (DataIntegrityViolationException e) {
            IdempotencyKey existing = repository.findById(key)
                    .orElseThrow(() -> BusinessException.conflict("Idempotency key conflict"));

            if (existing.isExpired(now)) {
                repository.delete(existing);
                repository.save(created);
            } else if (existing.hasStoredResponse()) {
                writeStoredResponse(existing, response);
                return;
            } else {
                throw BusinessException.conflict("A request with this idempotency key is already being processed");
            }
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, wrappedResponse);
        } finally {
            storeResponse(key, endpoint, wrappedResponse);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void storeResponse(String key, String endpoint, ContentCachingResponseWrapper response) {
        int status = response.getStatus();
        if (status >= 500) {
            repository.findById(key).ifPresent(repository::delete);
            return;
        }

        byte[] bodyBytes = response.getContentAsByteArray();
        String contentType = response.getContentType();

        Object responseBody = null;
        if (bodyBytes != null && bodyBytes.length > 0) {
            responseBody = parseBodyForStorage(contentType, bodyBytes);
        }
        final Object finalResponseBody = responseBody;

        Map<String, String> headers = new HashMap<>();
        if (contentType != null) {
            headers.put("Content-Type", contentType);
        }
        final Map<String, String> finalHeaders = headers;

        repository.findById(key).ifPresentOrElse(existing -> {
            existing.setEndpoint(endpoint);
            existing.setMethod("POST");
            existing.setResponseStatus(status);
            existing.setResponseBody(finalResponseBody);
            existing.setResponseHeaders(finalHeaders);
            repository.save(existing);
        }, () -> log.debug("Idempotency key {} missing when storing response", key));
    }

    private Object parseBodyForStorage(String contentType, byte[] bodyBytes) {
        try {
            if (contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE)) {
                return objectMapper.readValue(bodyBytes, Object.class);
            }
        } catch (Exception e) {
            log.debug("Failed to parse JSON response for idempotency storage: {}", e.getMessage());
        }
        return Map.of("raw", new String(bodyBytes, StandardCharsets.UTF_8));
    }

    private void writeStoredResponse(IdempotencyKey existing, HttpServletResponse response) throws IOException {
        response.setStatus(existing.getResponseStatus());
        if (existing.getResponseHeaders() != null) {
            existing.getResponseHeaders().forEach((k, v) -> {
                if (k != null && v != null) response.setHeader(k, v);
            });
        }

        if (existing.getResponseBody() != null) {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(existing.getResponseBody()));
        }
    }

    private String getIdempotencyKey(HttpServletRequest request) {
        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (key == null || key.isBlank()) {
            key = request.getHeader(IDEMPOTENCY_KEY_HEADER_ALT);
        }
        if (key == null || key.isBlank()) return null;
        if (key.length() > 64) {
            throw new BusinessException("Idempotency-Key must be <= 64 characters", "INVALID_IDEMPOTENCY_KEY");
        }
        return key;
    }

    private String normalizeEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return uri;
    }
}
