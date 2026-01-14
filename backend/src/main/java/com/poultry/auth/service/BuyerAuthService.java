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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuyerAuthService {

    private final BuyerRepository buyerRepository;
    private final BuyerSessionRepository buyerSessionRepository;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final EncryptionUtil encryptionUtil;

    @Value("${platform.max-buyer-sessions:2}")
    private int maxBuyerSessions;

    @Transactional
    public OtpSentResponse requestOtp(BuyerOtpRequest request, String ipAddress) {
        String phoneHash = encryptionUtil.hash(request.getPhone());
        boolean isNewUser = !buyerRepository.existsByPhoneHash(phoneHash);

        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("deviceId", request.getDeviceId());
        deviceInfo.put("deviceFingerprint", request.getDeviceFingerprint());

        OtpService.OtpResult result = otpService.generateOtp(
                request.getPhone(),
                OtpRequest.OtpPurpose.LOGIN,
                ipAddress,
                deviceInfo
        );

        // In production, send OTP via SMS
        // smsService.sendOtp(request.getPhone(), result.otp());

        log.info("OTP requested for phone: {}, isNewUser: {}", encryptionUtil.maskPhone(request.getPhone()), isNewUser);
        auditService.logOtpRequest(request.getPhone(), "LOGIN");

        return OtpSentResponse.builder()
                .maskedPhone(encryptionUtil.maskPhone(request.getPhone()))
                .expiresInSeconds(result.expiresInSeconds())
                .attemptsRemaining(result.requestsRemaining())
                .newUser(isNewUser)
                .build();
    }

    @Transactional
    public TokenResponse verifyOtpAndLogin(BuyerOtpVerifyRequest request, String ipAddress) {
        // Verify OTP
        otpService.verifyOtp(request.getPhone(), request.getOtp(), OtpRequest.OtpPurpose.LOGIN);

        String phoneHash = encryptionUtil.hash(request.getPhone());
        boolean isNewUser = false;

        // Find or create buyer
        Buyer buyer = buyerRepository.findByPhoneHash(phoneHash)
                .orElseGet(() -> {
                    log.info("Creating new buyer for phone: {}", encryptionUtil.maskPhone(request.getPhone()));
                    return createNewBuyer(request);
                });

        if (buyer.getCreatedAt().isAfter(Instant.now().minusSeconds(60))) {
            isNewUser = true;
        }

        // Check buyer status
        if (buyer.getStatus() != Buyer.BuyerStatus.ACTIVE) {
            auditService.logOtpVerify(buyer.getId(), request.getPhone(), false, "Account blocked");
            throw BusinessException.forbidden("Your account has been blocked. Please contact support.");
        }

        // Handle device binding
        if (buyer.getDeviceId() != null && !buyer.getDeviceId().equals(request.getDeviceId())) {
            // Different device - check fingerprint
            log.info("New device login for buyer: {}", buyer.getId());
        }

        // Update buyer device info
        buyer.setDeviceId(request.getDeviceId());
        if (request.getFcmToken() != null) {
            buyer.setFcmToken(request.getFcmToken());
        }
        buyerRepository.save(buyer);

        // Manage sessions - enforce max 2 active sessions
        enforceMaxSessions(buyer.getId(), request.getDeviceId());

        // Generate tokens
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("phone", encryptionUtil.maskPhone(request.getPhone()));

        String accessToken = jwtService.generateAccessToken(
                buyer.getId(),
                "BUYER",
                "BUYER",
                extraClaims
        );

        String refreshToken = jwtService.generateRefreshToken(buyer.getId(), "BUYER");

        // Create new session
        createBuyerSession(buyer.getId(), request, refreshToken, ipAddress);

        auditService.logOtpVerify(buyer.getId(), request.getPhone(), true, null);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirySeconds())
                .expiresAt(jwtService.extractExpiration(accessToken))
                .userId(buyer.getId())
                .userType("BUYER")
                .name(buyer.getName())
                .newUser(isNewUser)
                .build();
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtService.validateToken(request.getRefreshToken())
                || !jwtService.isRefreshToken(request.getRefreshToken())) {
            throw BusinessException.unauthorized("Invalid refresh token");
        }

        String tokenHash = encryptionUtil.hash(request.getRefreshToken());
        BuyerSession session = buyerSessionRepository.findByRefreshTokenHash(tokenHash)
                .orElseThrow(() -> BusinessException.unauthorized("Session not found"));

        if (!session.isValid()) {
            throw BusinessException.unauthorized("Session expired or revoked");
        }

        Buyer buyer = buyerRepository.findById(session.getBuyerId())
                .orElseThrow(() -> BusinessException.unauthorized("Buyer not found"));

        if (buyer.getStatus() != Buyer.BuyerStatus.ACTIVE) {
            throw BusinessException.forbidden("Account is not active");
        }

        // Revoke old session
        session.revoke("Token refresh");
        buyerSessionRepository.save(session);

        // Generate new tokens
        Map<String, Object> extraClaims = new HashMap<>();

        String accessToken = jwtService.generateAccessToken(
                buyer.getId(),
                "BUYER",
                "BUYER",
                extraClaims
        );

        String newRefreshToken = jwtService.generateRefreshToken(buyer.getId(), "BUYER");

        // Create new session
        BuyerSession newSession = BuyerSession.builder()
                .buyerId(buyer.getId())
                .deviceId(session.getDeviceId())
                .deviceFingerprint(session.getDeviceFingerprint())
                .deviceInfo(session.getDeviceInfo())
                .ipAddress(session.getIpAddress())
                .refreshTokenHash(encryptionUtil.hash(newRefreshToken))
                .expiresAt(jwtService.getRefreshTokenExpiry())
                .build();
        buyerSessionRepository.save(newSession);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirySeconds())
                .expiresAt(jwtService.extractExpiration(accessToken))
                .userId(buyer.getId())
                .userType("BUYER")
                .name(buyer.getName())
                .build();
    }

    @Transactional
    public void logout(UUID buyerId, String refreshToken) {
        if (refreshToken != null) {
            String tokenHash = encryptionUtil.hash(refreshToken);
            buyerSessionRepository.findByRefreshTokenHash(tokenHash)
                    .ifPresent(session -> {
                        session.revoke("User logout");
                        buyerSessionRepository.save(session);
                    });
        }
        auditService.logLogout("BUYER", buyerId);
    }

    @Transactional
    public void logoutAllDevices(UUID buyerId) {
        buyerSessionRepository.revokeAllBuyerSessions(buyerId, Instant.now(), "Logout all devices");
        auditService.logLogout("BUYER", buyerId);
    }

    private Buyer createNewBuyer(BuyerOtpVerifyRequest request) {
        Buyer buyer = Buyer.builder()
                .phoneEncrypted(encryptionUtil.encrypt(request.getPhone()))
                .phoneHash(encryptionUtil.hash(request.getPhone()))
                .deviceId(request.getDeviceId())
                .fcmToken(request.getFcmToken())
                .build();
        return buyerRepository.save(buyer);
    }

    private void enforceMaxSessions(UUID buyerId, String currentDeviceId) {
        long activeSessions = buyerSessionRepository.countActiveSessions(buyerId, Instant.now());

        if (activeSessions >= maxBuyerSessions) {
            // Revoke oldest session(s) to make room for new one
            List<BuyerSession> oldestSessions = buyerSessionRepository
                    .findOldestActiveSessions(buyerId, Instant.now());

            for (int i = 0; i <= activeSessions - maxBuyerSessions && i < oldestSessions.size(); i++) {
                BuyerSession session = oldestSessions.get(i);
                if (!session.getDeviceId().equals(currentDeviceId)) {
                    session.revoke("Session limit exceeded");
                    buyerSessionRepository.save(session);
                    log.info("Revoked old session for buyer: {}, device: {}", buyerId, session.getDeviceId());
                }
            }
        }
    }

    private void createBuyerSession(UUID buyerId, BuyerOtpVerifyRequest request,
                                    String refreshToken, String ipAddress) {
        Map<String, Object> deviceInfo = new HashMap<>();
        if (request.getDeviceInfo() != null) {
            deviceInfo.put("raw", request.getDeviceInfo());
        }

        BuyerSession session = BuyerSession.builder()
                .buyerId(buyerId)
                .deviceId(request.getDeviceId())
                .deviceFingerprint(request.getDeviceFingerprint())
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .refreshTokenHash(encryptionUtil.hash(refreshToken))
                .expiresAt(jwtService.getRefreshTokenExpiry())
                .build();

        buyerSessionRepository.save(session);
    }
}
