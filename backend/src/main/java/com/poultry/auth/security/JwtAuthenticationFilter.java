package com.poultry.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromRequest(request);

        if (StringUtils.hasText(token)) {
            try {
                if (jwtService.validateToken(token) && jwtService.isAccessToken(token)) {
                    UUID userId = jwtService.extractUserId(token);
                    String userType = jwtService.extractUserType(token);
                    String role = jwtService.extractRole(token);

                    Map<String, Object> claims = jwtService.extractAllClaims(token);
                    UUID sellerId = claims.containsKey("sellerId")
                            ? UUID.fromString((String) claims.get("sellerId"))
                            : null;
                    boolean mustChangePassword = claims.containsKey("mustChangePassword")
                            && (Boolean) claims.get("mustChangePassword");

                    UserPrincipal principal = UserPrincipal.builder()
                            .id(userId)
                            .userType(userType)
                            .role(role)
                            .sellerId(sellerId)
                            .email(claims.containsKey("email") ? (String) claims.get("email") : null)
                            .enabled(true)
                            .mustChangePassword(mustChangePassword)
                            .build();

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    principal.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Add correlation ID for request tracing
                    String correlationId = request.getHeader("X-Correlation-ID");
                    if (!StringUtils.hasText(correlationId)) {
                        correlationId = UUID.randomUUID().toString();
                    }
                    request.setAttribute("correlationId", correlationId);
                }
            } catch (Exception e) {
                log.debug("JWT authentication failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
