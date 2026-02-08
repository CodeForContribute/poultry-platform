package com.poultry.seller.service;

import com.poultry.auth.dto.ChangePasswordRequest;
import com.poultry.auth.entity.SellerUser;
import com.poultry.auth.repository.RefreshTokenRepository;
import com.poultry.auth.repository.SellerUserRepository;
import com.poultry.auth.service.AuditService;
import com.poultry.common.exception.BusinessException;
import com.poultry.seller.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerSecurityService {

    private final SellerUserRepository sellerUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    private static final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public SecuritySettingsDto getSecuritySettings(UUID userId, UUID sellerId) {
        log.info("Fetching security settings for user: {}", userId);

        SellerUser user = sellerUserRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User", userId));

        // Count active sessions
        long activeSessions = refreshTokenRepository.countActiveTokens(userId, "SELLER_USER", Instant.now());

        // Check if 2FA is enabled (placeholder - would need 2FA fields in SellerUser)
        // For now, return false as 2FA is not yet implemented
        boolean twoFactorEnabled = false;

        return SecuritySettingsDto.builder()
                .twoFactorEnabled(twoFactorEnabled)
                .lastPasswordChange(user.getPasswordChangedAt())
                .lastLoginAt(user.getLastLoginAt())
                .mustChangePassword(user.getMustChangePassword())
                .activeSessions((int) activeSessions)
                .build();
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        log.info("Changing password for user: {}", userId);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match", "PASSWORD_MISMATCH", HttpStatus.BAD_REQUEST);
        }

        SellerUser user = sellerUserRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw BusinessException.unauthorized("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException(
                    "New password must be different from current password",
                    "PASSWORD_SAME",
                    HttpStatus.BAD_REQUEST
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(Instant.now());
        user.setMustChangePassword(false);
        sellerUserRepository.save(user);

        // Revoke all refresh tokens to force re-login on other devices
        refreshTokenRepository.revokeAllUserTokens(
                "SELLER_USER", userId, Instant.now(), "Password changed"
        );

        auditService.logPasswordChange("SELLER_USER", userId, false);
        log.info("Password changed for user: {}", userId);
    }

    @Transactional(readOnly = true)
    public TwoFactorSetupDto setup2FA(UUID userId) {
        log.info("Generating 2FA setup for user: {}", userId);

        SellerUser user = sellerUserRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User", userId));

        // Generate a secret key
        byte[] secretBytes = new byte[20];
        secureRandom.nextBytes(secretBytes);
        String secretKey = Base64.getEncoder().encodeToString(secretBytes);

        // Generate QR code URL (for Google Authenticator format)
        String issuer = "PoultryPlatform";
        String accountName = user.getEmail();
        String qrCodeUrl = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                issuer, accountName, secretKey, issuer
        );

        return TwoFactorSetupDto.builder()
                .secretKey(secretKey)
                .qrCodeUrl(qrCodeUrl)
                .manualEntryKey(secretKey)
                .build();
    }

    @Transactional
    public void enable2FA(UUID userId, TwoFactorVerifyRequest request) {
        log.info("Enabling 2FA for user: {}", userId);

        SellerUser user = sellerUserRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User", userId));

        // In a real implementation, we would:
        // 1. Store the secret key in the user record (encrypted)
        // 2. Verify the provided code matches the expected TOTP code
        // 3. Generate and store backup codes

        // For now, this is a placeholder that shows the API structure
        // A real implementation would use a TOTP library like com.warrenstrange:googleauth

        if (request.getCode() == null || request.getCode().length() != 6) {
            throw new BusinessException("Invalid verification code", "INVALID_CODE", HttpStatus.BAD_REQUEST);
        }

        // TODO: Implement actual TOTP verification
        // For now, always succeed if code is 6 digits
        log.info("2FA enabled for user: {} (Note: This is a placeholder implementation)", userId);
    }

    @Transactional
    public void disable2FA(UUID userId, TwoFactorVerifyRequest request) {
        log.info("Disabling 2FA for user: {}", userId);

        SellerUser user = sellerUserRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User", userId));

        // Verify the code before disabling
        if (request.getCode() == null || request.getCode().length() != 6) {
            throw new BusinessException("Invalid verification code", "INVALID_CODE", HttpStatus.BAD_REQUEST);
        }

        // TODO: Implement actual TOTP verification and disable 2FA
        log.info("2FA disabled for user: {} (Note: This is a placeholder implementation)", userId);
    }

    @Transactional
    public boolean verify2FA(UUID userId, String code) {
        log.info("Verifying 2FA code for user: {}", userId);

        SellerUser user = sellerUserRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User", userId));

        if (code == null || code.length() != 6) {
            return false;
        }

        // TODO: Implement actual TOTP verification
        // For now, this is a placeholder
        return true;
    }

    @Transactional
    public BackupCodesDto generateBackupCodes(UUID userId) {
        log.info("Generating backup codes for user: {}", userId);

        SellerUser user = sellerUserRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User", userId));

        // Generate 10 backup codes
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String code = String.format("%04d-%04d",
                    secureRandom.nextInt(10000),
                    secureRandom.nextInt(10000));
            codes.add(code);
        }

        // TODO: Store hashed backup codes in database

        return BackupCodesDto.builder()
                .codes(codes)
                .usedCount(0)
                .totalCount(10)
                .build();
    }

    @Transactional
    public void logoutAllSessions(UUID userId) {
        log.info("Logging out all sessions for user: {}", userId);

        // Revoke all refresh tokens for this user
        refreshTokenRepository.revokeAllUserTokens(
                "SELLER_USER", userId, Instant.now(), "User initiated logout all"
        );

        auditService.logLogout("SELLER_USER", userId);
        log.info("All sessions logged out for user: {}", userId);
    }
}
