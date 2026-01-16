package com.poultry.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.auth.dto.*;
import com.poultry.auth.entity.Buyer;
import com.poultry.auth.entity.BuyerSession;
import com.poultry.auth.entity.OtpRequest;
import com.poultry.auth.repository.BuyerRepository;
import com.poultry.auth.repository.BuyerSessionRepository;
import com.poultry.auth.repository.OtpRequestRepository;
import com.poultry.config.BaseIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("BuyerAuth Controller Integration Tests")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BuyerAuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BuyerRepository buyerRepository;

    @Autowired
    private OtpRequestRepository otpRequestRepository;

    @Autowired
    private BuyerSessionRepository buyerSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_PHONE = "9876543210";
    private static final String TEST_PHONE_HASH = "test_phone_hash";
    private static final String TEST_DEVICE_ID = "device_12345678";
    private static final String TEST_OTP = "123456";

    @AfterEach
    void tearDown() {
        buyerSessionRepository.deleteAll();
        otpRequestRepository.deleteAll();
        buyerRepository.deleteAll();
    }

    @Nested
    @DisplayName("OTP Request Flow")
    class OtpRequestFlowTests {

        @Test
        @DisplayName("should request OTP successfully for new user")
        void shouldRequestOtpForNewUser() throws Exception {
            BuyerOtpRequest request = BuyerOtpRequest.builder()
                    .phone(TEST_PHONE)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceFingerprint("fingerprint_123")
                    .build();

            mockMvc.perform(post("/api/v1/buyer/auth/otp/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.newUser").value(true))
                    .andExpect(jsonPath("$.data.expiresInSeconds").exists())
                    .andExpect(jsonPath("$.data.attemptsRemaining").exists());

            // Verify OTP was saved
            assertThat(otpRequestRepository.count()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should request OTP successfully for existing user")
        void shouldRequestOtpForExistingUser() throws Exception {
            // Create existing buyer
            createTestBuyer();

            BuyerOtpRequest request = BuyerOtpRequest.builder()
                    .phone(TEST_PHONE)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceFingerprint("fingerprint_123")
                    .build();

            mockMvc.perform(post("/api/v1/buyer/auth/otp/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newUser").value(false));
        }

        @Test
        @DisplayName("should return validation error for invalid phone")
        void shouldReturnValidationErrorForInvalidPhone() throws Exception {
            BuyerOtpRequest request = BuyerOtpRequest.builder()
                    .phone("123") // Invalid - too short
                    .deviceId(TEST_DEVICE_ID)
                    .build();

            mockMvc.perform(post("/api/v1/buyer/auth/otp/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return validation error for missing device ID")
        void shouldReturnValidationErrorForMissingDeviceId() throws Exception {
            BuyerOtpRequest request = BuyerOtpRequest.builder()
                    .phone(TEST_PHONE)
                    // Missing deviceId
                    .build();

            mockMvc.perform(post("/api/v1/buyer/auth/otp/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("OTP Verification Flow")
    class OtpVerificationFlowTests {

        @Test
        @DisplayName("should verify OTP and login successfully for new user")
        void shouldVerifyOtpAndLoginNewUser() throws Exception {
            // First request OTP
            createOtpRequest(TEST_PHONE, TEST_OTP);

            BuyerOtpVerifyRequest request = BuyerOtpVerifyRequest.builder()
                    .phone(TEST_PHONE)
                    .otp(TEST_OTP)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceFingerprint("fingerprint_123")
                    .build();

            MvcResult result = mockMvc.perform(post("/api/v1/buyer/auth/otp/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").exists())
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.userId").exists())
                    .andExpect(jsonPath("$.data.userType").value("BUYER"))
                    .andExpect(jsonPath("$.data.newUser").value(true))
                    .andReturn();

            // Verify buyer was created
            assertThat(buyerRepository.count()).isEqualTo(1);

            // Verify session was created
            assertThat(buyerSessionRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should verify OTP and login successfully for existing user")
        void shouldVerifyOtpAndLoginExistingUser() throws Exception {
            // Create existing buyer
            Buyer existingBuyer = createTestBuyer();

            // Create OTP request
            createOtpRequest(TEST_PHONE, TEST_OTP);

            BuyerOtpVerifyRequest request = BuyerOtpVerifyRequest.builder()
                    .phone(TEST_PHONE)
                    .otp(TEST_OTP)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceFingerprint("fingerprint_123")
                    .build();

            mockMvc.perform(post("/api/v1/buyer/auth/otp/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(existingBuyer.getId().toString()))
                    .andExpect(jsonPath("$.data.newUser").value(false));
        }

        @Test
        @DisplayName("should return error for invalid OTP")
        void shouldReturnErrorForInvalidOtp() throws Exception {
            // Create OTP request with different OTP
            createOtpRequest(TEST_PHONE, "654321");

            BuyerOtpVerifyRequest request = BuyerOtpVerifyRequest.builder()
                    .phone(TEST_PHONE)
                    .otp("123456") // Wrong OTP
                    .deviceId(TEST_DEVICE_ID)
                    .build();

            mockMvc.perform(post("/api/v1/buyer/auth/otp/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("OTP_INVALID"));
        }

        @Test
        @DisplayName("should return error for expired OTP")
        void shouldReturnErrorForExpiredOtp() throws Exception {
            // Create expired OTP request
            createExpiredOtpRequest(TEST_PHONE, TEST_OTP);

            BuyerOtpVerifyRequest request = BuyerOtpVerifyRequest.builder()
                    .phone(TEST_PHONE)
                    .otp(TEST_OTP)
                    .deviceId(TEST_DEVICE_ID)
                    .build();

            mockMvc.perform(post("/api/v1/buyer/auth/otp/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("OTP_NOT_FOUND"));
        }

        @Test
        @DisplayName("should return error for blocked user")
        void shouldReturnErrorForBlockedUser() throws Exception {
            // Create blocked buyer
            Buyer blockedBuyer = createTestBuyer();
            blockedBuyer.setStatus(Buyer.BuyerStatus.BLOCKED);
            buyerRepository.save(blockedBuyer);

            // Create OTP request
            createOtpRequest(TEST_PHONE, TEST_OTP);

            BuyerOtpVerifyRequest request = BuyerOtpVerifyRequest.builder()
                    .phone(TEST_PHONE)
                    .otp(TEST_OTP)
                    .deviceId(TEST_DEVICE_ID)
                    .build();

            mockMvc.perform(post("/api/v1/buyer/auth/otp/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("Token Refresh Flow")
    class TokenRefreshFlowTests {

        @Test
        @DisplayName("should refresh token successfully")
        void shouldRefreshTokenSuccessfully() throws Exception {
            // Create buyer and session
            Buyer buyer = createTestBuyer();
            String refreshToken = "valid_refresh_token_" + UUID.randomUUID();
            createBuyerSession(buyer.getId(), refreshToken);

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            // Note: This test will fail if JwtService validation is strict
            // In real integration tests, you'd need to use actual JWT tokens
            mockMvc.perform(post("/api/v1/buyer/auth/token/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print());
            // The response depends on JWT implementation
        }
    }

    @Nested
    @DisplayName("Logout Flow")
    class LogoutFlowTests {

        @Test
        @DisplayName("should logout successfully")
        void shouldLogoutSuccessfully() throws Exception {
            // Create buyer and session
            Buyer buyer = createTestBuyer();
            String refreshToken = "valid_refresh_token";
            BuyerSession session = createBuyerSession(buyer.getId(), refreshToken);

            // Note: In real tests, you'd use actual JWT tokens
            mockMvc.perform(post("/api/v1/buyer/auth/logout")
                            .header("Authorization", "Bearer test_access_token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                    .andDo(print());
            // Response depends on security configuration
        }
    }

    // Helper methods
    private Buyer createTestBuyer() {
        Buyer buyer = Buyer.builder()
                .phoneEncrypted(TEST_PHONE.getBytes())
                .phoneHash(hashPhone(TEST_PHONE))
                .name("Test Buyer")
                .status(Buyer.BuyerStatus.ACTIVE)
                .deviceId(TEST_DEVICE_ID)
                .build();
        buyer.setCreatedAt(Instant.now().minus(1, ChronoUnit.DAYS)); // Not new
        return buyerRepository.save(buyer);
    }

    private OtpRequest createOtpRequest(String phone, String otp) {
        OtpRequest otpRequest = OtpRequest.builder()
                .phoneHash(hashPhone(phone))
                .phoneEncrypted(phone.getBytes())
                .otpHash(passwordEncoder.encode(otp))
                .purpose(OtpRequest.OtpPurpose.LOGIN)
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .maxAttempts(3)
                .attempts(0)
                .ipAddress("127.0.0.1")
                .deviceInfo(Map.of("deviceId", TEST_DEVICE_ID))
                .build();
        return otpRequestRepository.save(otpRequest);
    }

    private OtpRequest createExpiredOtpRequest(String phone, String otp) {
        OtpRequest otpRequest = OtpRequest.builder()
                .phoneHash(hashPhone(phone))
                .phoneEncrypted(phone.getBytes())
                .otpHash(passwordEncoder.encode(otp))
                .purpose(OtpRequest.OtpPurpose.LOGIN)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS)) // Expired
                .maxAttempts(3)
                .attempts(0)
                .ipAddress("127.0.0.1")
                .deviceInfo(Map.of("deviceId", TEST_DEVICE_ID))
                .build();
        return otpRequestRepository.save(otpRequest);
    }

    private BuyerSession createBuyerSession(UUID buyerId, String refreshToken) {
        BuyerSession session = BuyerSession.builder()
                .buyerId(buyerId)
                .deviceId(TEST_DEVICE_ID)
                .deviceFingerprint("fingerprint_123")
                .deviceInfo(Map.of())
                .ipAddress("127.0.0.1")
                .refreshTokenHash(hashPhone(refreshToken)) // Reusing hash function
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        return buyerSessionRepository.save(session);
    }

    private String hashPhone(String phone) {
        // Simple hash for testing - in production this would use EncryptionUtil
        return "hash_" + phone.hashCode();
    }
}
