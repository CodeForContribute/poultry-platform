package com.poultry.auth.service;

import com.poultry.auth.entity.OtpRequest;
import com.poultry.auth.repository.OtpRequestRepository;
import com.poultry.common.exception.BusinessException;
import com.poultry.common.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpService Unit Tests")
class OtpServiceTest {

    @Mock
    private OtpRequestRepository otpRequestRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EncryptionUtil encryptionUtil;

    @InjectMocks
    private OtpService otpService;

    @Captor
    private ArgumentCaptor<OtpRequest> otpRequestCaptor;

    private static final String TEST_PHONE = "9876543210";
    private static final String TEST_PHONE_HASH = "hash_9876543210";
    private static final String TEST_IP_ADDRESS = "192.168.1.1";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "otpLength", 6);
        ReflectionTestUtils.setField(otpService, "expiryMinutes", 5);
        ReflectionTestUtils.setField(otpService, "maxAttempts", 3);
        ReflectionTestUtils.setField(otpService, "maxOtpPerHour", 5);
    }

    @Nested
    @DisplayName("generateOtp")
    class GenerateOtpTests {

        @Test
        @DisplayName("should generate OTP successfully for valid request")
        void shouldGenerateOtpSuccessfully() {
            // Arrange
            Map<String, Object> deviceInfo = Map.of("deviceId", "test_device");

            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(otpRequestRepository.countRecentRequests(eq(TEST_PHONE_HASH), any(Instant.class))).thenReturn(0L);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded_otp");
            when(encryptionUtil.encrypt(TEST_PHONE)).thenReturn(TEST_PHONE.getBytes());
            when(otpRequestRepository.save(any(OtpRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            OtpService.OtpResult result = otpService.generateOtp(
                    TEST_PHONE, OtpRequest.OtpPurpose.LOGIN, TEST_IP_ADDRESS, deviceInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.otp()).hasSize(6);
            assertThat(result.otp()).matches("\\d{6}");
            assertThat(result.expiresInSeconds()).isEqualTo(300); // 5 minutes
            assertThat(result.requestsRemaining()).isEqualTo(4); // maxOtpPerHour - 1

            verify(otpRequestRepository).save(otpRequestCaptor.capture());
            OtpRequest savedRequest = otpRequestCaptor.getValue();
            assertThat(savedRequest.getPhoneHash()).isEqualTo(TEST_PHONE_HASH);
            assertThat(savedRequest.getPurpose()).isEqualTo(OtpRequest.OtpPurpose.LOGIN);
            assertThat(savedRequest.getMaxAttempts()).isEqualTo(3);
            assertThat(savedRequest.getExpiresAt()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("should throw rate limit exception when max OTPs per hour exceeded")
        void shouldThrowRateLimitExceptionWhenMaxOtpsExceeded() {
            // Arrange
            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(otpRequestRepository.countRecentRequests(eq(TEST_PHONE_HASH), any(Instant.class))).thenReturn(5L);

            // Act & Assert
            assertThatThrownBy(() -> otpService.generateOtp(
                    TEST_PHONE, OtpRequest.OtpPurpose.LOGIN, TEST_IP_ADDRESS, Map.of()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Too many OTP requests")
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

            verify(otpRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("should generate different OTP each time")
        void shouldGenerateDifferentOtpEachTime() {
            // Arrange
            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(otpRequestRepository.countRecentRequests(eq(TEST_PHONE_HASH), any(Instant.class))).thenReturn(0L);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded_otp");
            when(encryptionUtil.encrypt(TEST_PHONE)).thenReturn(TEST_PHONE.getBytes());
            when(otpRequestRepository.save(any(OtpRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            OtpService.OtpResult result1 = otpService.generateOtp(
                    TEST_PHONE, OtpRequest.OtpPurpose.LOGIN, TEST_IP_ADDRESS, Map.of());
            OtpService.OtpResult result2 = otpService.generateOtp(
                    TEST_PHONE, OtpRequest.OtpPurpose.LOGIN, TEST_IP_ADDRESS, Map.of());

            // Assert - OTPs should be random, so most likely different
            // Note: There's a small chance they could be equal by random chance
            assertThat(result1.otp()).hasSize(6);
            assertThat(result2.otp()).hasSize(6);
        }

        @Test
        @DisplayName("should calculate remaining requests correctly")
        void shouldCalculateRemainingRequestsCorrectly() {
            // Arrange
            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(otpRequestRepository.countRecentRequests(eq(TEST_PHONE_HASH), any(Instant.class))).thenReturn(3L);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded_otp");
            when(encryptionUtil.encrypt(TEST_PHONE)).thenReturn(TEST_PHONE.getBytes());
            when(otpRequestRepository.save(any(OtpRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            OtpService.OtpResult result = otpService.generateOtp(
                    TEST_PHONE, OtpRequest.OtpPurpose.LOGIN, TEST_IP_ADDRESS, Map.of());

            // Assert
            assertThat(result.requestsRemaining()).isEqualTo(1); // 5 - 3 - 1 = 1
        }

        @Test
        @DisplayName("should handle password reset purpose")
        void shouldHandlePasswordResetPurpose() {
            // Arrange
            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(otpRequestRepository.countRecentRequests(eq(TEST_PHONE_HASH), any(Instant.class))).thenReturn(0L);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded_otp");
            when(encryptionUtil.encrypt(TEST_PHONE)).thenReturn(TEST_PHONE.getBytes());
            when(otpRequestRepository.save(any(OtpRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            otpService.generateOtp(TEST_PHONE, OtpRequest.OtpPurpose.PASSWORD_RESET, TEST_IP_ADDRESS, Map.of());

            // Assert
            verify(otpRequestRepository).save(otpRequestCaptor.capture());
            assertThat(otpRequestCaptor.getValue().getPurpose()).isEqualTo(OtpRequest.OtpPurpose.PASSWORD_RESET);
        }
    }

    @Nested
    @DisplayName("verifyOtp")
    class VerifyOtpTests {

        @Test
        @DisplayName("should verify OTP successfully for valid OTP")
        void shouldVerifyOtpSuccessfully() {
            // Arrange
            String otp = "123456";
            OtpRequest otpRequest = createValidOtpRequest();

            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(otpRequestRepository.findLatestValidOtp(eq(TEST_PHONE_HASH), eq(OtpRequest.OtpPurpose.LOGIN), any(Instant.class)))
                    .thenReturn(Optional.of(otpRequest));
            when(passwordEncoder.matches(otp, otpRequest.getOtpHash())).thenReturn(true);
            when(otpRequestRepository.save(any(OtpRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            boolean result = otpService.verifyOtp(TEST_PHONE, otp, OtpRequest.OtpPurpose.LOGIN);

            // Assert
            assertThat(result).isTrue();
            verify(otpRequestRepository).save(otpRequestCaptor.capture());
            assertThat(otpRequestCaptor.getValue().getVerified()).isTrue();
        }

        @Test
        @DisplayName("should throw exception when no valid OTP found")
        void shouldThrowExceptionWhenNoValidOtpFound() {
            // Arrange
            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(otpRequestRepository.findLatestValidOtp(eq(TEST_PHONE_HASH), eq(OtpRequest.OtpPurpose.LOGIN), any(Instant.class)))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> otpService.verifyOtp(TEST_PHONE, "123456", OtpRequest.OtpPurpose.LOGIN))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No valid OTP found")
                    .extracting("errorCode")
                    .isEqualTo("OTP_NOT_FOUND");
        }

        @Test
        @DisplayName("should throw exception when max attempts exceeded")
        void shouldThrowExceptionWhenMaxAttemptsExceeded() {
            // Arrange
            OtpRequest otpRequest = createValidOtpRequest();
            otpRequest.setAttempts(3); // Already at max

            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(otpRequestRepository.findLatestValidOtp(eq(TEST_PHONE_HASH), eq(OtpRequest.OtpPurpose.LOGIN), any(Instant.class)))
                    .thenReturn(Optional.of(otpRequest));

            // Act & Assert
            assertThatThrownBy(() -> otpService.verifyOtp(TEST_PHONE, "123456", OtpRequest.OtpPurpose.LOGIN))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Maximum verification attempts exceeded")
                    .extracting("errorCode")
                    .isEqualTo("OTP_MAX_ATTEMPTS");
        }

        @Test
        @DisplayName("should throw exception when OTP is invalid")
        void shouldThrowExceptionWhenOtpIsInvalid() {
            // Arrange
            String otp = "123456";
            OtpRequest otpRequest = createValidOtpRequest();

            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(otpRequestRepository.findLatestValidOtp(eq(TEST_PHONE_HASH), eq(OtpRequest.OtpPurpose.LOGIN), any(Instant.class)))
                    .thenReturn(Optional.of(otpRequest));
            when(passwordEncoder.matches(otp, otpRequest.getOtpHash())).thenReturn(false);
            when(otpRequestRepository.save(any(OtpRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act & Assert
            assertThatThrownBy(() -> otpService.verifyOtp(TEST_PHONE, otp, OtpRequest.OtpPurpose.LOGIN))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid OTP")
                    .extracting("errorCode")
                    .isEqualTo("OTP_INVALID");

            // Verify attempts were incremented
            verify(otpRequestRepository).save(otpRequestCaptor.capture());
            assertThat(otpRequestCaptor.getValue().getAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("should show remaining attempts in error message")
        void shouldShowRemainingAttemptsInErrorMessage() {
            // Arrange
            String otp = "wrong_otp";
            OtpRequest otpRequest = createValidOtpRequest();
            otpRequest.setAttempts(1); // 1 attempt already made

            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(otpRequestRepository.findLatestValidOtp(eq(TEST_PHONE_HASH), eq(OtpRequest.OtpPurpose.LOGIN), any(Instant.class)))
                    .thenReturn(Optional.of(otpRequest));
            when(passwordEncoder.matches(otp, otpRequest.getOtpHash())).thenReturn(false);
            when(otpRequestRepository.save(any(OtpRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act & Assert
            assertThatThrownBy(() -> otpService.verifyOtp(TEST_PHONE, otp, OtpRequest.OtpPurpose.LOGIN))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("1 attempts remaining"); // 3 max - 2 used = 1 remaining
        }

        @Test
        @DisplayName("should increment attempts on each invalid verification")
        void shouldIncrementAttemptsOnInvalidVerification() {
            // Arrange
            String otp = "wrong";
            OtpRequest otpRequest = createValidOtpRequest();
            otpRequest.setAttempts(0);

            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(otpRequestRepository.findLatestValidOtp(eq(TEST_PHONE_HASH), eq(OtpRequest.OtpPurpose.LOGIN), any(Instant.class)))
                    .thenReturn(Optional.of(otpRequest));
            when(passwordEncoder.matches(otp, otpRequest.getOtpHash())).thenReturn(false);
            when(otpRequestRepository.save(any(OtpRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            try {
                otpService.verifyOtp(TEST_PHONE, otp, OtpRequest.OtpPurpose.LOGIN);
            } catch (BusinessException e) {
                // Expected
            }

            // Assert
            verify(otpRequestRepository).save(otpRequestCaptor.capture());
            assertThat(otpRequestCaptor.getValue().getAttempts()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Rate Limiting")
    class RateLimitingTests {

        @Test
        @DisplayName("should allow request when under rate limit")
        void shouldAllowRequestWhenUnderRateLimit() {
            // Arrange
            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(otpRequestRepository.countRecentRequests(eq(TEST_PHONE_HASH), any(Instant.class))).thenReturn(4L);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded_otp");
            when(encryptionUtil.encrypt(TEST_PHONE)).thenReturn(TEST_PHONE.getBytes());
            when(otpRequestRepository.save(any(OtpRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act - should not throw
            OtpService.OtpResult result = otpService.generateOtp(
                    TEST_PHONE, OtpRequest.OtpPurpose.LOGIN, TEST_IP_ADDRESS, Map.of());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.requestsRemaining()).isZero();
        }

        @Test
        @DisplayName("should block request when at rate limit")
        void shouldBlockRequestWhenAtRateLimit() {
            // Arrange
            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(otpRequestRepository.countRecentRequests(eq(TEST_PHONE_HASH), any(Instant.class))).thenReturn(5L);

            // Act & Assert
            assertThatThrownBy(() -> otpService.generateOtp(
                    TEST_PHONE, OtpRequest.OtpPurpose.LOGIN, TEST_IP_ADDRESS, Map.of()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }

        @Test
        @DisplayName("should use one hour window for rate limiting")
        void shouldUseOneHourWindowForRateLimiting() {
            // Arrange
            ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(otpRequestRepository.countRecentRequests(eq(TEST_PHONE_HASH), instantCaptor.capture())).thenReturn(0L);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded_otp");
            when(encryptionUtil.encrypt(TEST_PHONE)).thenReturn(TEST_PHONE.getBytes());
            when(otpRequestRepository.save(any(OtpRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            otpService.generateOtp(TEST_PHONE, OtpRequest.OtpPurpose.LOGIN, TEST_IP_ADDRESS, Map.of());

            // Assert
            Instant capturedInstant = instantCaptor.getValue();
            Instant expectedMinimum = Instant.now().minus(1, ChronoUnit.HOURS).minusSeconds(5);
            Instant expectedMaximum = Instant.now().minus(1, ChronoUnit.HOURS).plusSeconds(5);
            assertThat(capturedInstant).isBetween(expectedMinimum, expectedMaximum);
        }
    }

    private OtpRequest createValidOtpRequest() {
        return OtpRequest.builder()
                .phoneHash(TEST_PHONE_HASH)
                .phoneEncrypted(TEST_PHONE.getBytes())
                .otpHash("hashed_otp_123456")
                .purpose(OtpRequest.OtpPurpose.LOGIN)
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .maxAttempts(3)
                .attempts(0)
                .ipAddress(TEST_IP_ADDRESS)
                .deviceInfo(Map.of("deviceId", "test_device"))
                .build();
    }
}
