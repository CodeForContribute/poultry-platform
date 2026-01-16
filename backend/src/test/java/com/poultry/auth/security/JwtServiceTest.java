package com.poultry.auth.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    // Must be at least 256 bits (32 bytes) for HMAC-SHA256
    private static final String TEST_SECRET = "this-is-a-test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256";
    private static final Duration ACCESS_TOKEN_EXPIRY = Duration.ofHours(1);
    private static final Duration REFRESH_TOKEN_EXPIRY = Duration.ofDays(7);

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, ACCESS_TOKEN_EXPIRY, REFRESH_TOKEN_EXPIRY);
    }

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessTokenTests {

        @Test
        @DisplayName("should generate access token with correct claims")
        void shouldGenerateAccessTokenWithCorrectClaims() {
            UUID userId = UUID.randomUUID();
            String userType = "BUYER";
            String role = "BUYER";
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("phone", "98****3210");

            String token = jwtService.generateAccessToken(userId, userType, role, extraClaims);

            assertThat(token).isNotNull().isNotEmpty();
            assertThat(jwtService.validateToken(token)).isTrue();
            assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
            assertThat(jwtService.extractUserType(token)).isEqualTo(userType);
            assertThat(jwtService.extractRole(token)).isEqualTo(role);
            assertThat(jwtService.isAccessToken(token)).isTrue();
            assertThat(jwtService.isRefreshToken(token)).isFalse();
        }

        @Test
        @DisplayName("should include extra claims in token")
        void shouldIncludeExtraClaims() {
            UUID userId = UUID.randomUUID();
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("customField", "customValue");
            extraClaims.put("numericField", 123);

            String token = jwtService.generateAccessToken(userId, "BUYER", "BUYER", extraClaims);

            Claims claims = jwtService.extractAllClaims(token);
            assertThat(claims.get("customField", String.class)).isEqualTo("customValue");
            assertThat(claims.get("numericField", Integer.class)).isEqualTo(123);
        }

        @Test
        @DisplayName("should set correct expiration time")
        void shouldSetCorrectExpirationTime() {
            UUID userId = UUID.randomUUID();
            Instant beforeGeneration = Instant.now();

            String token = jwtService.generateAccessToken(userId, "BUYER", "BUYER", Map.of());

            Instant expiration = jwtService.extractExpiration(token);
            Instant expectedMinExpiry = beforeGeneration.plus(ACCESS_TOKEN_EXPIRY).minusSeconds(5);
            Instant expectedMaxExpiry = beforeGeneration.plus(ACCESS_TOKEN_EXPIRY).plusSeconds(5);

            assertThat(expiration).isBetween(expectedMinExpiry, expectedMaxExpiry);
        }

        @Test
        @DisplayName("should handle empty extra claims")
        void shouldHandleEmptyExtraClaims() {
            UUID userId = UUID.randomUUID();

            String token = jwtService.generateAccessToken(userId, "SELLER", "OWNER", Map.of());

            assertThat(token).isNotNull();
            assertThat(jwtService.validateToken(token)).isTrue();
        }
    }

    @Nested
    @DisplayName("generateRefreshToken")
    class GenerateRefreshTokenTests {

        @Test
        @DisplayName("should generate refresh token with correct claims")
        void shouldGenerateRefreshTokenWithCorrectClaims() {
            UUID userId = UUID.randomUUID();
            String userType = "BUYER";

            String token = jwtService.generateRefreshToken(userId, userType);

            assertThat(token).isNotNull().isNotEmpty();
            assertThat(jwtService.validateToken(token)).isTrue();
            assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
            assertThat(jwtService.extractUserType(token)).isEqualTo(userType);
            assertThat(jwtService.isRefreshToken(token)).isTrue();
            assertThat(jwtService.isAccessToken(token)).isFalse();
        }

        @Test
        @DisplayName("should generate unique token IDs")
        void shouldGenerateUniqueTokenIds() {
            UUID userId = UUID.randomUUID();

            String token1 = jwtService.generateRefreshToken(userId, "BUYER");
            String token2 = jwtService.generateRefreshToken(userId, "BUYER");

            Claims claims1 = jwtService.extractAllClaims(token1);
            Claims claims2 = jwtService.extractAllClaims(token2);

            assertThat(claims1.getId()).isNotEqualTo(claims2.getId());
        }

        @Test
        @DisplayName("should have longer expiry than access token")
        void shouldHaveLongerExpiryThanAccessToken() {
            UUID userId = UUID.randomUUID();

            String accessToken = jwtService.generateAccessToken(userId, "BUYER", "BUYER", Map.of());
            String refreshToken = jwtService.generateRefreshToken(userId, "BUYER");

            Instant accessExpiry = jwtService.extractExpiration(accessToken);
            Instant refreshExpiry = jwtService.extractExpiration(refreshToken);

            assertThat(refreshExpiry).isAfter(accessExpiry);
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateTokenTests {

        @Test
        @DisplayName("should validate correct token")
        void shouldValidateCorrectToken() {
            UUID userId = UUID.randomUUID();
            String token = jwtService.generateAccessToken(userId, "BUYER", "BUYER", Map.of());

            assertThat(jwtService.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("should reject invalid token")
        void shouldRejectInvalidToken() {
            assertThat(jwtService.validateToken("invalid.token.here")).isFalse();
        }

        @Test
        @DisplayName("should reject null token")
        void shouldRejectNullToken() {
            assertThat(jwtService.validateToken(null)).isFalse();
        }

        @Test
        @DisplayName("should reject empty token")
        void shouldRejectEmptyToken() {
            assertThat(jwtService.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("should reject token with wrong secret")
        void shouldRejectTokenWithWrongSecret() {
            // Create token with different secret
            JwtService otherService = new JwtService(
                    "different-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256",
                    ACCESS_TOKEN_EXPIRY,
                    REFRESH_TOKEN_EXPIRY
            );
            String token = otherService.generateAccessToken(UUID.randomUUID(), "BUYER", "BUYER", Map.of());

            assertThat(jwtService.validateToken(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("extractClaims")
    class ExtractClaimsTests {

        @Test
        @DisplayName("should extract user ID correctly")
        void shouldExtractUserIdCorrectly() {
            UUID userId = UUID.randomUUID();
            String token = jwtService.generateAccessToken(userId, "BUYER", "BUYER", Map.of());

            UUID extractedUserId = jwtService.extractUserId(token);

            assertThat(extractedUserId).isEqualTo(userId);
        }

        @Test
        @DisplayName("should extract user type correctly")
        void shouldExtractUserTypeCorrectly() {
            String token = jwtService.generateAccessToken(UUID.randomUUID(), "SELLER", "OWNER", Map.of());

            String userType = jwtService.extractUserType(token);

            assertThat(userType).isEqualTo("SELLER");
        }

        @Test
        @DisplayName("should extract role correctly")
        void shouldExtractRoleCorrectly() {
            String token = jwtService.generateAccessToken(UUID.randomUUID(), "SELLER", "ADMIN", Map.of());

            String role = jwtService.extractRole(token);

            assertThat(role).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("should extract token type correctly for access token")
        void shouldExtractTokenTypeForAccessToken() {
            String token = jwtService.generateAccessToken(UUID.randomUUID(), "BUYER", "BUYER", Map.of());

            assertThat(jwtService.extractTokenType(token)).isEqualTo("access");
        }

        @Test
        @DisplayName("should extract token type correctly for refresh token")
        void shouldExtractTokenTypeForRefreshToken() {
            String token = jwtService.generateRefreshToken(UUID.randomUUID(), "BUYER");

            assertThat(jwtService.extractTokenType(token)).isEqualTo("refresh");
        }

        @Test
        @DisplayName("should extract all claims")
        void shouldExtractAllClaims() {
            UUID userId = UUID.randomUUID();
            Map<String, Object> extraClaims = Map.of("customKey", "customValue");
            String token = jwtService.generateAccessToken(userId, "BUYER", "BUYER", extraClaims);

            Claims claims = jwtService.extractAllClaims(token);

            assertThat(claims.getSubject()).isEqualTo(userId.toString());
            assertThat(claims.get("type", String.class)).isEqualTo("access");
            assertThat(claims.get("userType", String.class)).isEqualTo("BUYER");
            assertThat(claims.get("role", String.class)).isEqualTo("BUYER");
            assertThat(claims.get("customKey", String.class)).isEqualTo("customValue");
        }

        @Test
        @DisplayName("should throw exception for invalid token")
        void shouldThrowExceptionForInvalidToken() {
            assertThatThrownBy(() -> jwtService.extractAllClaims("invalid.token"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Token Type Checks")
    class TokenTypeCheckTests {

        @Test
        @DisplayName("isAccessToken should return true for access token")
        void isAccessTokenShouldReturnTrueForAccessToken() {
            String token = jwtService.generateAccessToken(UUID.randomUUID(), "BUYER", "BUYER", Map.of());

            assertThat(jwtService.isAccessToken(token)).isTrue();
        }

        @Test
        @DisplayName("isAccessToken should return false for refresh token")
        void isAccessTokenShouldReturnFalseForRefreshToken() {
            String token = jwtService.generateRefreshToken(UUID.randomUUID(), "BUYER");

            assertThat(jwtService.isAccessToken(token)).isFalse();
        }

        @Test
        @DisplayName("isRefreshToken should return true for refresh token")
        void isRefreshTokenShouldReturnTrueForRefreshToken() {
            String token = jwtService.generateRefreshToken(UUID.randomUUID(), "BUYER");

            assertThat(jwtService.isRefreshToken(token)).isTrue();
        }

        @Test
        @DisplayName("isRefreshToken should return false for access token")
        void isRefreshTokenShouldReturnFalseForAccessToken() {
            String token = jwtService.generateAccessToken(UUID.randomUUID(), "BUYER", "BUYER", Map.of());

            assertThat(jwtService.isRefreshToken(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("Expiry Methods")
    class ExpiryMethodsTests {

        @Test
        @DisplayName("should return correct access token expiry in seconds")
        void shouldReturnCorrectAccessTokenExpirySeconds() {
            long expirySeconds = jwtService.getAccessTokenExpirySeconds();

            assertThat(expirySeconds).isEqualTo(ACCESS_TOKEN_EXPIRY.toSeconds());
        }

        @Test
        @DisplayName("should return refresh token expiry in future")
        void shouldReturnRefreshTokenExpiryInFuture() {
            Instant beforeCall = Instant.now();

            Instant expiry = jwtService.getRefreshTokenExpiry();

            assertThat(expiry).isAfter(beforeCall);
            assertThat(expiry).isBefore(beforeCall.plus(REFRESH_TOKEN_EXPIRY).plusSeconds(5));
        }

        @Test
        @DisplayName("should extract expiration from token")
        void shouldExtractExpirationFromToken() {
            String token = jwtService.generateAccessToken(UUID.randomUUID(), "BUYER", "BUYER", Map.of());

            Instant expiration = jwtService.extractExpiration(token);

            assertThat(expiration).isAfter(Instant.now());
            assertThat(expiration).isBefore(Instant.now().plus(ACCESS_TOKEN_EXPIRY).plusSeconds(5));
        }
    }

    @Nested
    @DisplayName("Custom Claim Extraction")
    class CustomClaimExtractionTests {

        @Test
        @DisplayName("should extract custom claim using function")
        void shouldExtractCustomClaimUsingFunction() {
            Map<String, Object> extraClaims = Map.of("sellerId", "12345");
            String token = jwtService.generateAccessToken(UUID.randomUUID(), "SELLER", "OWNER", extraClaims);

            String sellerId = jwtService.extractClaim(token, claims -> claims.get("sellerId", String.class));

            assertThat(sellerId).isEqualTo("12345");
        }

        @Test
        @DisplayName("should handle missing custom claim")
        void shouldHandleMissingCustomClaim() {
            String token = jwtService.generateAccessToken(UUID.randomUUID(), "BUYER", "BUYER", Map.of());

            String missingClaim = jwtService.extractClaim(token, claims -> claims.get("nonExistent", String.class));

            assertThat(missingClaim).isNull();
        }
    }
}
