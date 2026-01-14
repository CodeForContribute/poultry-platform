package com.poultry.auth.service;

import com.poultry.auth.entity.OtpRequest;
import com.poultry.auth.repository.OtpRequestRepository;
import com.poultry.common.exception.BusinessException;
import com.poultry.common.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRequestRepository otpRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionUtil encryptionUtil;

    @Value("${otp.length:6}")
    private int otpLength;

    @Value("${otp.expiry-minutes:5}")
    private int expiryMinutes;

    @Value("${otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${otp.rate-limit.max-per-hour:5}")
    private int maxOtpPerHour;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public OtpResult generateOtp(String phone, OtpRequest.OtpPurpose purpose,
                                 String ipAddress, Map<String, Object> deviceInfo) {
        String phoneHash = encryptionUtil.hash(phone);

        // Check rate limit
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentRequests = otpRequestRepository.countRecentRequests(phoneHash, oneHourAgo);

        if (recentRequests >= maxOtpPerHour) {
            throw BusinessException.rateLimitExceeded(
                    "Too many OTP requests. Please try again later."
            );
        }

        // Generate OTP
        String otp = generateRandomOtp();
        String otpHash = passwordEncoder.encode(otp);

        // Save OTP request
        OtpRequest otpRequest = OtpRequest.builder()
                .phoneHash(phoneHash)
                .phoneEncrypted(encryptionUtil.encrypt(phone))
                .otpHash(otpHash)
                .purpose(purpose)
                .expiresAt(Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES))
                .maxAttempts(maxAttempts)
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .build();

        otpRequestRepository.save(otpRequest);

        log.info("OTP generated for phone hash: {}, purpose: {}", phoneHash, purpose);

        return new OtpResult(otp, expiryMinutes * 60, maxOtpPerHour - (int) recentRequests - 1);
    }

    @Transactional
    public boolean verifyOtp(String phone, String otp, OtpRequest.OtpPurpose purpose) {
        String phoneHash = encryptionUtil.hash(phone);

        OtpRequest otpRequest = otpRequestRepository
                .findLatestValidOtp(phoneHash, purpose, Instant.now())
                .orElseThrow(() -> new BusinessException(
                        "No valid OTP found. Please request a new OTP.",
                        "OTP_NOT_FOUND",
                        HttpStatus.BAD_REQUEST
                ));

        // Check attempts
        if (otpRequest.getAttempts() >= otpRequest.getMaxAttempts()) {
            throw new BusinessException(
                    "Maximum verification attempts exceeded. Please request a new OTP.",
                    "OTP_MAX_ATTEMPTS",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        // Increment attempts
        otpRequest.incrementAttempts();

        // Verify OTP
        if (!passwordEncoder.matches(otp, otpRequest.getOtpHash())) {
            otpRequestRepository.save(otpRequest);
            int remaining = otpRequest.getMaxAttempts() - otpRequest.getAttempts();
            throw new BusinessException(
                    "Invalid OTP. " + remaining + " attempts remaining.",
                    "OTP_INVALID",
                    HttpStatus.BAD_REQUEST,
                    Map.of("attemptsRemaining", remaining)
            );
        }

        // Mark as verified
        otpRequest.markVerified();
        otpRequestRepository.save(otpRequest);

        log.info("OTP verified for phone hash: {}, purpose: {}", phoneHash, purpose);
        return true;
    }

    private String generateRandomOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }

    public record OtpResult(String otp, int expiresInSeconds, int requestsRemaining) {}
}
