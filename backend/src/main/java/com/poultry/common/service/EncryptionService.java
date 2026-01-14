package com.poultry.common.service;

import com.poultry.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive data using AES-256-GCM.
 * This service provides secure encryption for sensitive fields like bank account numbers.
 */
@Slf4j
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int AES_KEY_LENGTH = 32; // 256 bits

    private final SecretKey secretKey;

    public EncryptionService(@Value("${encryption.key}") String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Encryption key must be configured via 'encryption.key' property");
        }

        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < AES_KEY_LENGTH) {
            // Pad the key to 32 bytes if it's shorter
            byte[] paddedKey = new byte[AES_KEY_LENGTH];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, AES_KEY_LENGTH));
            keyBytes = paddedKey;
            log.warn("Encryption key was padded to 32 bytes. Consider using a full 32-byte key for better security.");
        } else if (keyBytes.length > AES_KEY_LENGTH) {
            // Truncate if longer
            byte[] truncatedKey = new byte[AES_KEY_LENGTH];
            System.arraycopy(keyBytes, 0, truncatedKey, 0, AES_KEY_LENGTH);
            keyBytes = truncatedKey;
        }

        this.secretKey = new SecretKeySpec(keyBytes, 0, AES_KEY_LENGTH, "AES");
        log.info("EncryptionService initialized with AES-256-GCM");
    }

    /**
     * Encrypts a plaintext string using AES-256-GCM.
     * The IV is prepended to the ciphertext for use during decryption.
     *
     * @param plaintext The string to encrypt
     * @return The encrypted bytes (IV + ciphertext)
     * @throws BusinessException if encryption fails
     */
    public byte[] encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new BusinessException("Cannot encrypt null or empty value", "ENCRYPTION_ERROR", HttpStatus.BAD_REQUEST);
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            return byteBuffer.array();
        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage());
            throw new BusinessException("Failed to encrypt sensitive data", "ENCRYPTION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Decrypts ciphertext using AES-256-GCM.
     * Expects the IV to be prepended to the ciphertext.
     *
     * @param ciphertext The encrypted bytes (IV + ciphertext)
     * @return The decrypted plaintext string
     * @throws BusinessException if decryption fails
     */
    public String decrypt(byte[] ciphertext) {
        if (ciphertext == null || ciphertext.length <= GCM_IV_LENGTH) {
            throw new BusinessException("Invalid encrypted data", "DECRYPTION_ERROR", HttpStatus.BAD_REQUEST);
        }

        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(ciphertext);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage());
            throw new BusinessException("Failed to decrypt sensitive data", "DECRYPTION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Encrypts a plaintext string and returns it as a Base64-encoded string.
     *
     * @param plaintext The string to encrypt
     * @return Base64-encoded encrypted string
     * @throws BusinessException if encryption fails
     */
    public String encryptToBase64(String plaintext) {
        byte[] encrypted = encrypt(plaintext);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Decrypts a Base64-encoded ciphertext string.
     *
     * @param base64Ciphertext The Base64-encoded encrypted string
     * @return The decrypted plaintext string
     * @throws BusinessException if decryption fails
     */
    public String decryptFromBase64(String base64Ciphertext) {
        if (base64Ciphertext == null || base64Ciphertext.isBlank()) {
            throw new BusinessException("Invalid encrypted data", "DECRYPTION_ERROR", HttpStatus.BAD_REQUEST);
        }

        try {
            byte[] ciphertext = Base64.getDecoder().decode(base64Ciphertext);
            return decrypt(ciphertext);
        } catch (IllegalArgumentException e) {
            log.error("Invalid Base64 encoding: {}", e.getMessage());
            throw new BusinessException("Invalid encrypted data format", "DECRYPTION_ERROR", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Masks a bank account number for display purposes.
     * Shows only the last 4 digits.
     *
     * @param accountNumber The bank account number to mask
     * @return Masked account number (e.g., "XXXXXX1234")
     */
    public String maskBankAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "XXXX";
        }
        int visibleDigits = 4;
        int maskedLength = accountNumber.length() - visibleDigits;
        return "X".repeat(maskedLength) + accountNumber.substring(maskedLength);
    }
}
