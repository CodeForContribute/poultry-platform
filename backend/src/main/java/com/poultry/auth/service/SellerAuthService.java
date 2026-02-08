package com.poultry.auth.service;

import com.poultry.auth.dto.*;
import com.poultry.auth.entity.RefreshToken;
import com.poultry.auth.entity.SellerUser;
import com.poultry.auth.repository.RefreshTokenRepository;
import com.poultry.auth.repository.SellerUserRepository;
import com.poultry.auth.security.JwtService;
import com.poultry.common.exception.BusinessException;
import com.poultry.common.util.EncryptionUtil;
import com.poultry.product.entity.Seller;
import com.poultry.product.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerAuthService {

    private final SellerUserRepository sellerUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final EncryptionUtil encryptionUtil;

    @Value("${password.rotation-days:90}")
    private int passwordRotationDays;

    @Transactional
    public TokenResponse login(SellerLoginRequest request, String ipAddress) {
        SellerUser user = sellerUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    auditService.logLoginFailure("SELLER_USER", request.getEmail(), "User not found");
                    return BusinessException.unauthorized("Invalid credentials");
                });

        // Check if account is locked
        if (user.isLocked()) {
            auditService.logLoginFailure("SELLER_USER", request.getEmail(), "Account locked");
            throw new BusinessException(
                    "Account is locked. Please try again later.",
                    "ACCOUNT_LOCKED",
                    HttpStatus.FORBIDDEN
            );
        }

        // Check status
        if (user.getStatus() != SellerUser.SellerStatus.ACTIVE) {
            auditService.logLoginFailure("SELLER_USER", request.getEmail(), "Account inactive");
            throw new BusinessException(
                    "Account is not active. Please contact support.",
                    "ACCOUNT_INACTIVE",
                    HttpStatus.FORBIDDEN
            );
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.incrementFailedAttempts();
            sellerUserRepository.save(user);
            auditService.logLoginFailure("SELLER_USER", request.getEmail(), "Invalid password");
            throw BusinessException.unauthorized("Invalid credentials");
        }

        // Reset failed attempts on successful login
        user.resetFailedAttempts();
        user.setLastLoginAt(Instant.now());
        sellerUserRepository.save(user);

        // Check password expiry
        boolean mustChangePassword = user.getMustChangePassword()
                || user.isPasswordExpired(passwordRotationDays);

        // Generate tokens
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("sellerId", user.getSellerId().toString());
        extraClaims.put("email", user.getEmail());
        extraClaims.put("mustChangePassword", mustChangePassword);

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                "SELLER_USER",
                user.getRole().name(),
                extraClaims
        );

        String refreshToken = jwtService.generateRefreshToken(user.getId(), "SELLER_USER");

        // Store refresh token
        saveRefreshToken(user.getId(), "SELLER_USER", refreshToken, request.getDeviceId(), ipAddress);

        auditService.logLoginSuccess("SELLER_USER", user.getId(), user.getEmail());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirySeconds())
                .expiresAt(jwtService.extractExpiration(accessToken))
                .userId(user.getId())
                .userType("SELLER_USER")
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .sellerId(user.getSellerId())
                .mustChangePassword(mustChangePassword)
                .build();
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtService.validateToken(request.getRefreshToken())
                || !jwtService.isRefreshToken(request.getRefreshToken())) {
            throw BusinessException.unauthorized("Invalid refresh token");
        }

        String tokenHash = encryptionUtil.hash(request.getRefreshToken());
        RefreshToken storedToken = refreshTokenRepository
                .findValidToken(tokenHash, Instant.now())
                .orElseThrow(() -> BusinessException.unauthorized("Refresh token not found or revoked"));

        if (!"SELLER_USER".equals(storedToken.getUserType())) {
            throw BusinessException.unauthorized("Invalid token type");
        }

        SellerUser user = sellerUserRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> BusinessException.unauthorized("User not found"));

        // Revoke old token
        storedToken.revoke("Token refresh");
        refreshTokenRepository.save(storedToken);

        // Generate new tokens
        boolean mustChangePassword = user.getMustChangePassword()
                || user.isPasswordExpired(passwordRotationDays);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("sellerId", user.getSellerId().toString());
        extraClaims.put("email", user.getEmail());
        extraClaims.put("mustChangePassword", mustChangePassword);

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                "SELLER_USER",
                user.getRole().name(),
                extraClaims
        );

        String newRefreshToken = jwtService.generateRefreshToken(user.getId(), "SELLER_USER");

        // Store new refresh token
        saveRefreshToken(user.getId(), "SELLER_USER", newRefreshToken,
                request.getDeviceId(), storedToken.getIpAddress());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirySeconds())
                .expiresAt(jwtService.extractExpiration(accessToken))
                .userId(user.getId())
                .userType("SELLER_USER")
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .sellerId(user.getSellerId())
                .mustChangePassword(mustChangePassword)
                .build();
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match", "PASSWORD_MISMATCH");
        }

        SellerUser user = sellerUserRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw BusinessException.unauthorized("Current password is incorrect");
        }

        // Check if new password is same as old
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException(
                    "New password must be different from current password",
                    "PASSWORD_SAME"
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(Instant.now());
        user.setMustChangePassword(false);
        sellerUserRepository.save(user);

        // Revoke all refresh tokens to force re-login
        refreshTokenRepository.revokeAllUserTokens(
                "SELLER_USER", userId, Instant.now(), "Password changed"
        );

        auditService.logPasswordChange("SELLER_USER", userId, false);
    }

    @Transactional
    public void logout(UUID userId, String refreshToken) {
        if (refreshToken != null) {
            String tokenHash = encryptionUtil.hash(refreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash)
                    .ifPresent(token -> {
                        token.revoke("User logout");
                        refreshTokenRepository.save(token);
                    });
        }
        auditService.logLogout("SELLER_USER", userId);
    }

    private void saveRefreshToken(UUID userId, String userType, String token,
                                  String deviceId, String ipAddress) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .userType(userType)
                .tokenHash(encryptionUtil.hash(token))
                .deviceId(deviceId)
                .ipAddress(ipAddress)
                .expiresAt(jwtService.getRefreshTokenExpiry())
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUser(UUID userId) {
        SellerUser user = sellerUserRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User", userId));

        String businessName = sellerRepository.findById(user.getSellerId())
                .map(Seller::getBusinessName)
                .orElse(null);

        boolean mustChangePassword = user.getMustChangePassword()
                || user.isPasswordExpired(passwordRotationDays);

        return UserInfoResponse.builder()
                .userId(user.getId())
                .userType("SELLER_USER")
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .sellerId(user.getSellerId())
                .businessName(businessName)
                .mustChangePassword(mustChangePassword)
                .build();
    }
}
