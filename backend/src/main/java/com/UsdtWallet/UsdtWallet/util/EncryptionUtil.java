package com.UsdtWallet.UsdtWallet.util;

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
 * Utility class for AES-256-GCM encryption/decryption of sensitive data
 */
@Component
@Slf4j
public class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits

    @Value("${wallet.encryption.secret:}")
    private String encryptionSecret;

    public EncryptionUtil() {}

    // Constructor với secret key (cho testing)
    public EncryptionUtil(String encryptionSecret) {
        this.encryptionSecret = encryptionSecret;
    }

    /**
     * Mã hóa text với AES-256-GCM
     * @param plainText văn bản cần mã hóa
     * @return Base64 encoded string (IV + encrypted data)
     */
    public String encrypt(String plainText) {
        try {
            SecretKey secretKey = getSecretKey();

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            // Tạo IV ngẫu nhiên
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Kết hợp IV + encrypted data
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);

            return Base64.getEncoder().encodeToString(encryptedWithIv);

        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    /**
     * Giải mã text với AES-256-GCM
     * @param encryptedText Base64 encoded encrypted text
     * @return văn bản gốc
     */
    public String decrypt(String encryptedText) {
        try {
            SecretKey secretKey = getSecretKey();

            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedText);

            // Tách IV và encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedData = new byte[encryptedWithIv.length - GCM_IV_LENGTH];

            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedData = cipher.doFinal(encryptedData);

            return new String(decryptedData, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    /**
     * Lấy secret key từ environment hoặc tạo mới
     */
    private SecretKey getSecretKey() {
        if (encryptionSecret == null || encryptionSecret.trim().isEmpty()) {
            log.error("WALLET_ENCRYPTION_SECRET environment variable not set!");
            throw new RuntimeException("Encryption secret not configured. Please set WALLET_ENCRYPTION_SECRET environment variable.");
        }

        // Sử dụng SHA-256 để tạo 256-bit key từ secret string
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(encryptionSecret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate secret key", e);
        }
    }

    /**
     * Tạo secret key ngẫu nhiên mới (chỉ dùng để generate key)
     */
    public static String generateNewSecretKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate new secret key", e);
        }
    }
}
