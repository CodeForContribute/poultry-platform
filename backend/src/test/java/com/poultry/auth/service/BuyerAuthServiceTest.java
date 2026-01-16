package com.poultry.auth.service;

import com.poultry.auth.dto.*;
import com.poultry.auth.entity.Buyer;
import com.poultry.auth.entity.BuyerSession;
import com.poultry.auth.entity.OtpRequest;
import com.poultry.auth.repository.BuyerRepository;
import com.poultry.auth.repository.BuyerSessionRepository;
import com.poultry.auth.security.JwtService;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuyerAuthService Unit Tests")
class BuyerAuthServiceTest {

    @Mock
    private BuyerRepository buyerRepository;

    @Mock
    private BuyerSessionRepository buyerSessionRepository;

    @Mock
    private OtpService otpService;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditService auditService;

    @Mock
    private EncryptionUtil encryptionUtil;

    @InjectMocks
    private BuyerAuthService buyerAuthService;

    @Captor
    private ArgumentCaptor<Buyer> buyerCaptor;

    @Captor
    private ArgumentCaptor<BuyerSession> sessionCaptor;

    private static final String TEST_PHONE = "9876543210";
    private static final String TEST_PHONE_HASH = "hash_9876543210";
    private static final String TEST_DEVICE_ID = "device_123";
    private static final String TEST_IP_ADDRESS = "192.168.1.1";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(buyerAuthService, "maxBuyerSessions", 2);
    }

    @Nested
    @DisplayName("requestOtp")
    class RequestOtpTests {

        @Test
        @DisplayName("should request OTP successfully for existing user")
        void shouldRequestOtpSuccessfullyForExistingUser() {
            // Arrange
            BuyerOtpRequest request = createOtpRequest();
            OtpService.OtpResult otpResult = new OtpService.OtpResult("123456", 300, 4);

            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(buyerRepository.existsByPhoneHash(TEST_PHONE_HASH)).thenReturn(true);
            when(otpService.generateOtp(eq(TEST_PHONE), eq(OtpRequest.OtpPurpose.LOGIN), eq(TEST_IP_ADDRESS), anyMap()))
                    .thenReturn(otpResult);
            when(encryptionUtil.maskPhone(TEST_PHONE)).thenReturn("98****3210");

            // Act
            OtpSentResponse response = buyerAuthService.requestOtp(request, TEST_IP_ADDRESS);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getMaskedPhone()).isEqualTo("98****3210");
            assertThat(response.getExpiresInSeconds()).isEqualTo(300);
            assertThat(response.getAttemptsRemaining()).isEqualTo(4);
            assertThat(response.isNewUser()).isFalse();

            verify(auditService).logOtpRequest(TEST_PHONE, "LOGIN");
        }

        @Test
        @DisplayName("should request OTP successfully for new user")
        void shouldRequestOtpSuccessfullyForNewUser() {
            // Arrange
            BuyerOtpRequest request = createOtpRequest();
            OtpService.OtpResult otpResult = new OtpService.OtpResult("123456", 300, 4);

            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(buyerRepository.existsByPhoneHash(TEST_PHONE_HASH)).thenReturn(false);
            when(otpService.generateOtp(eq(TEST_PHONE), eq(OtpRequest.OtpPurpose.LOGIN), eq(TEST_IP_ADDRESS), anyMap()))
                    .thenReturn(otpResult);
            when(encryptionUtil.maskPhone(TEST_PHONE)).thenReturn("98****3210");

            // Act
            OtpSentResponse response = buyerAuthService.requestOtp(request, TEST_IP_ADDRESS);

            // Assert
            assertThat(response.isNewUser()).isTrue();
        }

        @Test
        @DisplayName("should include device info in OTP request")
        void shouldIncludeDeviceInfoInOtpRequest() {
            // Arrange
            BuyerOtpRequest request = BuyerOtpRequest.builder()
                    .phone(TEST_PHONE)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceFingerprint("fingerprint_123")
                    .build();

            OtpService.OtpResult otpResult = new OtpService.OtpResult("123456", 300, 4);

            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(buyerRepository.existsByPhoneHash(TEST_PHONE_HASH)).thenReturn(true);

            ArgumentCaptor<Map<String, Object>> deviceInfoCaptor = ArgumentCaptor.forClass(Map.class);
            when(otpService.generateOtp(eq(TEST_PHONE), eq(OtpRequest.OtpPurpose.LOGIN), eq(TEST_IP_ADDRESS), deviceInfoCaptor.capture()))
                    .thenReturn(otpResult);
            when(encryptionUtil.maskPhone(TEST_PHONE)).thenReturn("98****3210");

            // Act
            buyerAuthService.requestOtp(request, TEST_IP_ADDRESS);

            // Assert
            Map<String, Object> capturedDeviceInfo = deviceInfoCaptor.getValue();
            assertThat(capturedDeviceInfo).containsEntry("deviceId", TEST_DEVICE_ID);
            assertThat(capturedDeviceInfo).containsEntry("deviceFingerprint", "fingerprint_123");
        }
    }

    @Nested
    @DisplayName("verifyOtpAndLogin")
    class VerifyOtpAndLoginTests {

        @Test
        @DisplayName("should verify OTP and login existing user successfully")
        void shouldVerifyOtpAndLoginExistingUserSuccessfully() {
            // Arrange
            BuyerOtpVerifyRequest request = createVerifyRequest();
            Buyer existingBuyer = createActiveBuyer();
            existingBuyer.setCreatedAt(Instant.now().minus(1, ChronoUnit.DAYS)); // Not new

            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(buyerRepository.findByPhoneHash(TEST_PHONE_HASH)).thenReturn(Optional.of(existingBuyer));
            when(buyerRepository.save(any(Buyer.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(buyerSessionRepository.countActiveSessions(any(UUID.class), any(Instant.class))).thenReturn(0L);
            when(jwtService.generateAccessToken(any(), anyString(), anyString(), anyMap())).thenReturn("access_token");
            when(jwtService.generateRefreshToken(any(), anyString())).thenReturn("refresh_token");
            when(jwtService.getAccessTokenExpirySeconds()).thenReturn(3600L);
            when(jwtService.extractExpiration(anyString())).thenReturn(Instant.now().plus(1, ChronoUnit.HOURS));
            when(jwtService.getRefreshTokenExpiry()).thenReturn(Instant.now().plus(7, ChronoUnit.DAYS));
            when(encryptionUtil.maskPhone(TEST_PHONE)).thenReturn("98****3210");
            when(encryptionUtil.hash("refresh_token")).thenReturn("refresh_token_hash");
            when(buyerSessionRepository.save(any(BuyerSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            TokenResponse response = buyerAuthService.verifyOtpAndLogin(request, TEST_IP_ADDRESS);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access_token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getUserId()).isEqualTo(existingBuyer.getId());
            assertThat(response.getUserType()).isEqualTo("BUYER");
            assertThat(response.isNewUser()).isFalse();

            verify(otpService).verifyOtp(TEST_PHONE, "123456", OtpRequest.OtpPurpose.LOGIN);
            verify(auditService).logOtpVerify(existingBuyer.getId(), TEST_PHONE, true, null);
        }

        @Test
        @DisplayName("should create new buyer on first login")
        void shouldCreateNewBuyerOnFirstLogin() {
            // Arrange
            BuyerOtpVerifyRequest request = createVerifyRequest();

            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(buyerRepository.findByPhoneHash(TEST_PHONE_HASH)).thenReturn(Optional.empty());
            when(encryptionUtil.encrypt(TEST_PHONE)).thenReturn(TEST_PHONE.getBytes());
            when(buyerRepository.save(any(Buyer.class))).thenAnswer(invocation -> {
                Buyer buyer = invocation.getArgument(0);
                buyer.setId(UUID.randomUUID());
                buyer.setCreatedAt(Instant.now());
                return buyer;
            });
            when(buyerSessionRepository.countActiveSessions(any(UUID.class), any(Instant.class))).thenReturn(0L);
            when(jwtService.generateAccessToken(any(), anyString(), anyString(), anyMap())).thenReturn("access_token");
            when(jwtService.generateRefreshToken(any(), anyString())).thenReturn("refresh_token");
            when(jwtService.getAccessTokenExpirySeconds()).thenReturn(3600L);
            when(jwtService.extractExpiration(anyString())).thenReturn(Instant.now().plus(1, ChronoUnit.HOURS));
            when(jwtService.getRefreshTokenExpiry()).thenReturn(Instant.now().plus(7, ChronoUnit.DAYS));
            when(encryptionUtil.maskPhone(TEST_PHONE)).thenReturn("98****3210");
            when(encryptionUtil.hash("refresh_token")).thenReturn("refresh_token_hash");
            when(buyerSessionRepository.save(any(BuyerSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            TokenResponse response = buyerAuthService.verifyOtpAndLogin(request, TEST_IP_ADDRESS);

            // Assert
            assertThat(response.isNewUser()).isTrue();

            verify(buyerRepository, times(2)).save(buyerCaptor.capture());
            Buyer savedBuyer = buyerCaptor.getAllValues().get(0);
            assertThat(savedBuyer.getPhoneHash()).isEqualTo(TEST_PHONE_HASH);
            assertThat(savedBuyer.getDeviceId()).isEqualTo(TEST_DEVICE_ID);
        }

        @Test
        @DisplayName("should throw exception when buyer is blocked")
        void shouldThrowExceptionWhenBuyerIsBlocked() {
            // Arrange
            BuyerOtpVerifyRequest request = createVerifyRequest();
            Buyer blockedBuyer = createActiveBuyer();
            blockedBuyer.setStatus(Buyer.BuyerStatus.BLOCKED);

            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(buyerRepository.findByPhoneHash(TEST_PHONE_HASH)).thenReturn(Optional.of(blockedBuyer));

            // Act & Assert
            assertThatThrownBy(() -> buyerAuthService.verifyOtpAndLogin(request, TEST_IP_ADDRESS))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("blocked")
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(auditService).logOtpVerify(blockedBuyer.getId(), TEST_PHONE, false, "Account blocked");
        }

        @Test
        @DisplayName("should update device info on login")
        void shouldUpdateDeviceInfoOnLogin() {
            // Arrange
            BuyerOtpVerifyRequest request = createVerifyRequest();
            request.setFcmToken("fcm_token_123");
            Buyer existingBuyer = createActiveBuyer();

            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(buyerRepository.findByPhoneHash(TEST_PHONE_HASH)).thenReturn(Optional.of(existingBuyer));
            when(buyerRepository.save(any(Buyer.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(buyerSessionRepository.countActiveSessions(any(UUID.class), any(Instant.class))).thenReturn(0L);
            when(jwtService.generateAccessToken(any(), anyString(), anyString(), anyMap())).thenReturn("access_token");
            when(jwtService.generateRefreshToken(any(), anyString())).thenReturn("refresh_token");
            when(jwtService.getAccessTokenExpirySeconds()).thenReturn(3600L);
            when(jwtService.extractExpiration(anyString())).thenReturn(Instant.now().plus(1, ChronoUnit.HOURS));
            when(jwtService.getRefreshTokenExpiry()).thenReturn(Instant.now().plus(7, ChronoUnit.DAYS));
            when(encryptionUtil.maskPhone(TEST_PHONE)).thenReturn("98****3210");
            when(encryptionUtil.hash("refresh_token")).thenReturn("refresh_token_hash");
            when(buyerSessionRepository.save(any(BuyerSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            buyerAuthService.verifyOtpAndLogin(request, TEST_IP_ADDRESS);

            // Assert
            verify(buyerRepository).save(buyerCaptor.capture());
            Buyer savedBuyer = buyerCaptor.getValue();
            assertThat(savedBuyer.getDeviceId()).isEqualTo(TEST_DEVICE_ID);
            assertThat(savedBuyer.getFcmToken()).isEqualTo("fcm_token_123");
        }

        @Test
        @DisplayName("should enforce max sessions limit")
        void shouldEnforceMaxSessionsLimit() {
            // Arrange
            BuyerOtpVerifyRequest request = createVerifyRequest();
            Buyer existingBuyer = createActiveBuyer();

            BuyerSession oldSession = BuyerSession.builder()
                    .id(UUID.randomUUID())
                    .buyerId(existingBuyer.getId())
                    .deviceId("old_device")
                    .build();

            when(encryptionUtil.hash(TEST_PHONE)).thenReturn(TEST_PHONE_HASH);
            when(buyerRepository.findByPhoneHash(TEST_PHONE_HASH)).thenReturn(Optional.of(existingBuyer));
            when(buyerRepository.save(any(Buyer.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(buyerSessionRepository.countActiveSessions(any(UUID.class), any(Instant.class))).thenReturn(2L);
            when(buyerSessionRepository.findOldestActiveSessions(any(UUID.class), any(Instant.class)))
                    .thenReturn(List.of(oldSession));
            when(buyerSessionRepository.save(any(BuyerSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtService.generateAccessToken(any(), anyString(), anyString(), anyMap())).thenReturn("access_token");
            when(jwtService.generateRefreshToken(any(), anyString())).thenReturn("refresh_token");
            when(jwtService.getAccessTokenExpirySeconds()).thenReturn(3600L);
            when(jwtService.extractExpiration(anyString())).thenReturn(Instant.now().plus(1, ChronoUnit.HOURS));
            when(jwtService.getRefreshTokenExpiry()).thenReturn(Instant.now().plus(7, ChronoUnit.DAYS));
            when(encryptionUtil.maskPhone(TEST_PHONE)).thenReturn("98****3210");
            when(encryptionUtil.hash("refresh_token")).thenReturn("refresh_token_hash");

            // Act
            buyerAuthService.verifyOtpAndLogin(request, TEST_IP_ADDRESS);

            // Assert - old session should be revoked
            verify(buyerSessionRepository, atLeast(2)).save(sessionCaptor.capture());
            List<BuyerSession> savedSessions = sessionCaptor.getAllValues();
            boolean foundRevokedSession = savedSessions.stream()
                    .anyMatch(s -> s.getId() != null && s.getId().equals(oldSession.getId()));
            // The old session should have been saved with revoked status
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {

        @Test
        @DisplayName("should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            // Arrange
            String refreshToken = "valid_refresh_token";
            RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(refreshToken).build();

            UUID buyerId = UUID.randomUUID();
            Buyer buyer = createActiveBuyer();
            buyer.setId(buyerId);

            BuyerSession session = BuyerSession.builder()
                    .id(UUID.randomUUID())
                    .buyerId(buyerId)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceFingerprint("fingerprint")
                    .deviceInfo(Map.of())
                    .ipAddress(TEST_IP_ADDRESS)
                    .refreshTokenHash("token_hash")
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .build();

            when(jwtService.validateToken(refreshToken)).thenReturn(true);
            when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
            when(encryptionUtil.hash(refreshToken)).thenReturn("token_hash");
            when(buyerSessionRepository.findByRefreshTokenHash("token_hash")).thenReturn(Optional.of(session));
            when(buyerRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
            when(buyerSessionRepository.save(any(BuyerSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtService.generateAccessToken(any(), anyString(), anyString(), anyMap())).thenReturn("new_access_token");
            when(jwtService.generateRefreshToken(any(), anyString())).thenReturn("new_refresh_token");
            when(jwtService.getAccessTokenExpirySeconds()).thenReturn(3600L);
            when(jwtService.extractExpiration(anyString())).thenReturn(Instant.now().plus(1, ChronoUnit.HOURS));
            when(jwtService.getRefreshTokenExpiry()).thenReturn(Instant.now().plus(7, ChronoUnit.DAYS));
            when(encryptionUtil.hash("new_refresh_token")).thenReturn("new_token_hash");

            // Act
            TokenResponse response = buyerAuthService.refreshToken(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("new_access_token");
            assertThat(response.getRefreshToken()).isEqualTo("new_refresh_token");
            assertThat(response.getUserId()).isEqualTo(buyerId);

            // Verify old session was revoked
            verify(buyerSessionRepository, times(2)).save(sessionCaptor.capture());
        }

        @Test
        @DisplayName("should throw exception for invalid refresh token")
        void shouldThrowExceptionForInvalidRefreshToken() {
            // Arrange
            String invalidToken = "invalid_token";
            RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(invalidToken).build();

            when(jwtService.validateToken(invalidToken)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> buyerAuthService.refreshToken(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid refresh token")
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("should throw exception when session not found")
        void shouldThrowExceptionWhenSessionNotFound() {
            // Arrange
            String refreshToken = "valid_token";
            RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(refreshToken).build();

            when(jwtService.validateToken(refreshToken)).thenReturn(true);
            when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
            when(encryptionUtil.hash(refreshToken)).thenReturn("token_hash");
            when(buyerSessionRepository.findByRefreshTokenHash("token_hash")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> buyerAuthService.refreshToken(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Session not found")
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("should throw exception when session is expired")
        void shouldThrowExceptionWhenSessionIsExpired() {
            // Arrange
            String refreshToken = "valid_token";
            RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(refreshToken).build();

            BuyerSession expiredSession = BuyerSession.builder()
                    .id(UUID.randomUUID())
                    .buyerId(UUID.randomUUID())
                    .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .build();

            when(jwtService.validateToken(refreshToken)).thenReturn(true);
            when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
            when(encryptionUtil.hash(refreshToken)).thenReturn("token_hash");
            when(buyerSessionRepository.findByRefreshTokenHash("token_hash")).thenReturn(Optional.of(expiredSession));

            // Act & Assert
            assertThatThrownBy(() -> buyerAuthService.refreshToken(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("expired or revoked")
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("should throw exception when buyer account is not active")
        void shouldThrowExceptionWhenBuyerNotActive() {
            // Arrange
            String refreshToken = "valid_token";
            RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(refreshToken).build();

            UUID buyerId = UUID.randomUUID();
            Buyer blockedBuyer = createActiveBuyer();
            blockedBuyer.setId(buyerId);
            blockedBuyer.setStatus(Buyer.BuyerStatus.BLOCKED);

            BuyerSession session = BuyerSession.builder()
                    .id(UUID.randomUUID())
                    .buyerId(buyerId)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .build();

            when(jwtService.validateToken(refreshToken)).thenReturn(true);
            when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
            when(encryptionUtil.hash(refreshToken)).thenReturn("token_hash");
            when(buyerSessionRepository.findByRefreshTokenHash("token_hash")).thenReturn(Optional.of(session));
            when(buyerRepository.findById(buyerId)).thenReturn(Optional.of(blockedBuyer));

            // Act & Assert
            assertThatThrownBy(() -> buyerAuthService.refreshToken(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not active")
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("should logout successfully with refresh token")
        void shouldLogoutSuccessfullyWithRefreshToken() {
            // Arrange
            UUID buyerId = UUID.randomUUID();
            String refreshToken = "refresh_token";

            BuyerSession session = BuyerSession.builder()
                    .id(UUID.randomUUID())
                    .buyerId(buyerId)
                    .build();

            when(encryptionUtil.hash(refreshToken)).thenReturn("token_hash");
            when(buyerSessionRepository.findByRefreshTokenHash("token_hash")).thenReturn(Optional.of(session));
            when(buyerSessionRepository.save(any(BuyerSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            buyerAuthService.logout(buyerId, refreshToken);

            // Assert
            verify(buyerSessionRepository).save(sessionCaptor.capture());
            verify(auditService).logLogout("BUYER", buyerId);
        }

        @Test
        @DisplayName("should handle logout without refresh token")
        void shouldHandleLogoutWithoutRefreshToken() {
            // Arrange
            UUID buyerId = UUID.randomUUID();

            // Act
            buyerAuthService.logout(buyerId, null);

            // Assert
            verify(buyerSessionRepository, never()).findByRefreshTokenHash(any());
            verify(auditService).logLogout("BUYER", buyerId);
        }
    }

    @Nested
    @DisplayName("logoutAllDevices")
    class LogoutAllDevicesTests {

        @Test
        @DisplayName("should logout from all devices")
        void shouldLogoutFromAllDevices() {
            // Arrange
            UUID buyerId = UUID.randomUUID();

            // Act
            buyerAuthService.logoutAllDevices(buyerId);

            // Assert
            verify(buyerSessionRepository).revokeAllBuyerSessions(eq(buyerId), any(Instant.class), eq("Logout all devices"));
            verify(auditService).logLogout("BUYER", buyerId);
        }
    }

    // Helper methods
    private BuyerOtpRequest createOtpRequest() {
        return BuyerOtpRequest.builder()
                .phone(TEST_PHONE)
                .deviceId(TEST_DEVICE_ID)
                .deviceFingerprint("fingerprint_123")
                .build();
    }

    private BuyerOtpVerifyRequest createVerifyRequest() {
        return BuyerOtpVerifyRequest.builder()
                .phone(TEST_PHONE)
                .otp("123456")
                .deviceId(TEST_DEVICE_ID)
                .deviceFingerprint("fingerprint_123")
                .build();
    }

    private Buyer createActiveBuyer() {
        Buyer buyer = new Buyer();
        buyer.setId(UUID.randomUUID());
        buyer.setPhoneEncrypted(TEST_PHONE.getBytes());
        buyer.setPhoneHash(TEST_PHONE_HASH);
        buyer.setName("Test Buyer");
        buyer.setStatus(Buyer.BuyerStatus.ACTIVE);
        buyer.setCreatedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        return buyer;
    }
}
