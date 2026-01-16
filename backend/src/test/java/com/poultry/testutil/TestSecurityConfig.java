package com.poultry.testutil;

import com.poultry.auth.security.JwtService;
import com.poultry.auth.security.UserPrincipal;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.security.web.SecurityFilterChain;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Test security configuration providing mock authentication and JWT services.
 */
@TestConfiguration
public class TestSecurityConfig {

    /**
     * Custom annotation to set up a mock buyer authentication for tests.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @WithSecurityContext(factory = WithMockBuyerSecurityContextFactory.class)
    public @interface WithMockBuyer {
        String buyerId() default "";
        String phone() default "9876543210";
        String name() default "Test Buyer";
    }

    /**
     * Custom annotation to set up a mock seller authentication for tests.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @WithSecurityContext(factory = WithMockSellerSecurityContextFactory.class)
    public @interface WithMockSeller {
        String sellerId() default "";
        String userId() default "";
        String email() default "seller@test.com";
        String role() default "OWNER";
    }

    /**
     * Custom annotation to set up a mock admin authentication for tests.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @WithSecurityContext(factory = WithMockAdminSecurityContextFactory.class)
    public @interface WithMockAdmin {
        String adminId() default "";
        String email() default "admin@poultry.com";
        String role() default "SUPER_ADMIN";
    }

    /**
     * Factory for creating mock buyer security context.
     */
    public static class WithMockBuyerSecurityContextFactory implements WithSecurityContextFactory<WithMockBuyer> {
        @Override
        public SecurityContext createSecurityContext(WithMockBuyer annotation) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();

            UUID buyerId = annotation.buyerId().isEmpty()
                    ? UUID.randomUUID()
                    : UUID.fromString(annotation.buyerId());

            UserPrincipal principal = UserPrincipal.builder()
                    .id(buyerId)
                    .userType("BUYER")
                    .role("BUYER")
                    .build();

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_BUYER"))
            );

            context.setAuthentication(auth);
            return context;
        }
    }

    /**
     * Factory for creating mock seller security context.
     */
    public static class WithMockSellerSecurityContextFactory implements WithSecurityContextFactory<WithMockSeller> {
        @Override
        public SecurityContext createSecurityContext(WithMockSeller annotation) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();

            UUID userId = annotation.userId().isEmpty()
                    ? UUID.randomUUID()
                    : UUID.fromString(annotation.userId());

            UUID sellerId = annotation.sellerId().isEmpty()
                    ? UUID.randomUUID()
                    : UUID.fromString(annotation.sellerId());

            UserPrincipal principal = UserPrincipal.builder()
                    .id(userId)
                    .userType("SELLER_USER")
                    .role(annotation.role())
                    .sellerId(sellerId)
                    .build();

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + annotation.role()))
            );

            context.setAuthentication(auth);
            return context;
        }
    }

    /**
     * Factory for creating mock admin security context.
     */
    public static class WithMockAdminSecurityContextFactory implements WithSecurityContextFactory<WithMockAdmin> {
        @Override
        public SecurityContext createSecurityContext(WithMockAdmin annotation) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();

            UUID adminId = annotation.adminId().isEmpty()
                    ? UUID.randomUUID()
                    : UUID.fromString(annotation.adminId());

            UserPrincipal principal = UserPrincipal.builder()
                    .id(adminId)
                    .userType("ADMIN")
                    .role(annotation.role())
                    .build();

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + annotation.role()))
            );

            context.setAuthentication(auth);
            return context;
        }
    }

    /**
     * Test password encoder bean.
     */
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Test security filter chain that permits all requests for integration tests.
     */
    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * Helper class for programmatically setting up authentication in tests.
     */
    public static class TestAuthHelper {

        public static void setupBuyerAuth(UUID buyerId) {
            UserPrincipal principal = UserPrincipal.builder()
                    .id(buyerId)
                    .userType("BUYER")
                    .role("BUYER")
                    .build();

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_BUYER"))
            );

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        public static void setupSellerAuth(UUID userId, UUID sellerId, String role) {
            UserPrincipal principal = UserPrincipal.builder()
                    .id(userId)
                    .userType("SELLER_USER")
                    .role(role)
                    .sellerId(sellerId)
                    .build();

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
            );

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        public static void setupAdminAuth(UUID adminId, String role) {
            UserPrincipal principal = UserPrincipal.builder()
                    .id(adminId)
                    .userType("ADMIN")
                    .role(role)
                    .build();

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
            );

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        public static void clearAuth() {
            SecurityContextHolder.clearContext();
        }

        public static UserPrincipal getCurrentUser() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
                return (UserPrincipal) auth.getPrincipal();
            }
            return null;
        }
    }

    /**
     * Mock JWT service for testing that generates predictable tokens.
     */
    public static class MockJwtService {

        private static final String TEST_SECRET = "test-secret-key-for-jwt-testing-that-is-long-enough";

        public String generateTestAccessToken(UUID userId, String userType, String role) {
            // Return a predictable test token format
            return String.format("test_access_token_%s_%s_%s", userId, userType, role);
        }

        public String generateTestRefreshToken(UUID userId, String userType) {
            // Return a predictable test token format
            return String.format("test_refresh_token_%s_%s", userId, userType);
        }

        public Map<String, Object> parseTestToken(String token) {
            Map<String, Object> claims = new HashMap<>();
            if (token.startsWith("test_access_token_") || token.startsWith("test_refresh_token_")) {
                String[] parts = token.split("_");
                if (parts.length >= 5) {
                    claims.put("userId", parts[3]);
                    claims.put("userType", parts[4]);
                    if (parts.length >= 6) {
                        claims.put("role", parts[5]);
                    }
                }
            }
            return claims;
        }

        public boolean isValidTestToken(String token) {
            return token != null &&
                   (token.startsWith("test_access_token_") || token.startsWith("test_refresh_token_"));
        }
    }
}
