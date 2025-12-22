package com.workflow.sociallabs.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * CredentialEncryption - шифрування та дешифрування credentials
 * Використовує AES-256-GCM для безпечного зберігання
 */
@Slf4j
@Component
public class CredentialEncryption {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${workflow.security.encryption.key}")
    private String encryptionKey;

    /**
     * Зашифрувати дані
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return "";
        }

        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Prepare cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    getKeyBytes(),
                    "AES"
            );
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            // Encrypt
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV + encrypted data
            byte[] combined = new byte[GCM_IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, GCM_IV_LENGTH, encrypted.length);

            // Encode to Base64
            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    /**
     * Розшифрувати дані
     */
    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            return "";
        }

        try {
            // Decode from Base64
            byte[] combined = Base64.getDecoder().decode(encrypted);

            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

            // Extract encrypted data
            byte[] encryptedData = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);

            // Prepare cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    getKeyBytes(),
                    "AES"
            );
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            // Decrypt
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            byte[] decrypted = cipher.doFinal(encryptedData);

            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    /**
     * Отримати байти ключа з правильною довжиною (32 bytes для AES-256)
     */
    private byte[] getKeyBytes() {
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);

        // Якщо ключ коротший за 32 байти - доповнити
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            return paddedKey;
        }

        // Якщо довший - обрізати
        if (keyBytes.length > 32) {
            byte[] truncatedKey = new byte[32];
            System.arraycopy(keyBytes, 0, truncatedKey, 0, 32);
            return truncatedKey;
        }

        return keyBytes;
    }

    /**
     * Згенерувати безпечний випадковий ключ
     */
    public static String generateSecureKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256, new SecureRandom());
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate key", e);
        }
    }

    /**
     * Перевірити чи валідний ключ шифрування
     */
    public boolean isValidKey() {
        try {
            String test = "test";
            String encrypted = encrypt(test);
            String decrypted = decrypt(encrypted);
            return test.equals(decrypted);
        } catch (Exception e) {
            return false;
        }
    }
}