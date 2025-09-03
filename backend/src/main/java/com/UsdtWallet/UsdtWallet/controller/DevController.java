package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.entity.HdMasterWallet;
import com.UsdtWallet.UsdtWallet.repository.HdMasterWalletRepository;
import com.UsdtWallet.UsdtWallet.util.EncryptionUtil;
import com.UsdtWallet.UsdtWallet.util.TronAddressUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Cung cấp các endpoint hỗ trợ dev và backup wallet
 */
@RestController
@RequestMapping("/api/dev")
@Profile("dev") // Chỉ active trong profile dev
@Slf4j
public class DevController {

    private final EncryptionUtil encryptionUtil;
    private final HdMasterWalletRepository masterWalletRepository;

    public DevController(EncryptionUtil encryptionUtil,
                         HdMasterWalletRepository masterWalletRepository) {
        this.encryptionUtil = encryptionUtil;
        this.masterWalletRepository = masterWalletRepository;
    }

    /**
     * Endpoint để generate encryption key cho wallet mnemonic
     */
    @GetMapping("/generate-encryption-key")
    public ResponseEntity<Map<String, Object>> generateEncryptionKey() {
        log.info("Generating new encryption key for development...");

        try {
            String newKey = EncryptionUtil.generateNewSecretKey();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Encryption key generated successfully");
            response.put("key", newKey);

            // Hướng dẫn sử dụng
            Map<String, String> instructions = new HashMap<>();
            instructions.put("environment_variable", "export WALLET_ENCRYPTION_SECRET=" + newKey);
            instructions.put("application_yml", "wallet.encryption.secret: " + newKey);
            instructions.put("docker_compose", "WALLET_ENCRYPTION_SECRET=" + newKey);

            response.put("instructions", instructions);

            Map<String, String> warnings = new HashMap<>();
            warnings.put("security", "KHÔNG bao giờ commit key này vào git!");
            warnings.put("backup", "Backup key này ở nơi an toàn");
            warnings.put("production", "Endpoint này chỉ dành cho DEV environment");

            response.put("warnings", warnings);

            log.info("Encryption key generated successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to generate encryption key", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to generate encryption key");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get master wallet backup information
     * Endpoint này hiển thị sensitive data
     */
    @GetMapping("/wallet-backup")
    public ResponseEntity<Map<String, Object>> getWalletBackup() {
        log.warn("🚨 ACCESSING WALLET BACKUP - CHỈ DÀNH CHO DEV!");

        try {
            // Lấy master wallet từ database
            Optional<HdMasterWallet> masterWallet = masterWalletRepository.findTopByOrderByIdDesc();

            if (masterWallet.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "No master wallet found");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            HdMasterWallet master = masterWallet.get();
            String encryptedMnemonic = master.getEncryptedMnemonic();
            String masterAddress = master.getMasterAddress();

            // Decrypt mnemonic
            String decryptedMnemonic = decryptMnemonic(encryptedMnemonic);

            // Get private key
            TronAddressUtil tronUtil = new TronAddressUtil();
            TronAddressUtil.WalletInfo walletInfo = tronUtil.deriveWallet(decryptedMnemonic, 0);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Wallet backup retrieved successfully");

            // Wallet information
            Map<String, Object> walletData = new HashMap<>();
            walletData.put("masterAddress", masterAddress);
            walletData.put("mnemonic", decryptedMnemonic);
            walletData.put("privateKey", walletInfo.privateKey());
            walletData.put("derivationPath", "m/44'/195'/0'/0/0");
            walletData.put("mnemonicWordCount", decryptedMnemonic.split(" ").length);

            response.put("wallet", walletData);

            // TronLink import guide
            Map<String, Object> importGuide = new HashMap<>();
            importGuide.put("method1_privateKey", Map.of(
                "step1", "Mở TronLink -> Create Wallet -> Import Wallet",
                "step2", "Chọn 'Private Key'",
                "step3", "Nhập private key: " + walletInfo.privateKey(),
                "step4", "Address sẽ là: " + masterAddress,
                "recommended", "KHUYẾN NGHỊ - Dùng method này"
            ));

            importGuide.put("method2_mnemonic", Map.of(
                "step1", "Mở TronLink -> Create Wallet -> Import Wallet",
                "step2", "Chọn 'Mnemonic Phrase'",
                "step3", "Nhập mnemonic: " + decryptedMnemonic,
                "step4", "Address có thể khác (TronLink có implementation khác)",
                "note", "⚠️ Address có thể không khớp với hệ thống"
            ));

            response.put("importGuide", importGuide);

            // Explanation về vấn đề TronLink
            Map<String, String> explanation = new HashMap<>();
            explanation.put("issue", "TronLink và hệ thống dùng cùng mnemonic + derivation path nhưng tạo ra address khác nhau");
            explanation.put("cause", "Implementation khác nhau trong cách generate address từ private key");
            explanation.put("solution", "Sử dụng private key để import thay vì mnemonic");
            explanation.put("result", "Import private key sẽ tạo ra đúng address của hệ thống");

            response.put("explanation", explanation);

            // Security warnings
            Map<String, String> warnings = new HashMap<>();
            warnings.put("security", "MNEMONIC & PRIVATE KEY - BẢO MẬT TUYỆT ĐỐI!");
            warnings.put("backup", "BACKUP NGAY VÀO NƠI AN TOÀN!");
            warnings.put("production", "KHÔNG BAO GIỜ sử dụng endpoint này trong production");
            warnings.put("access", "Chỉ admin được phép access");

            response.put("warnings", warnings);

            log.warn("WALLET BACKUP ACCESSED - Address: {}", masterAddress);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get wallet backup", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve wallet backup: " + e.getMessage());
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Test encryption configuration
     */
    @GetMapping("/test-encryption")
    public ResponseEntity<Map<String, Object>> testEncryption() {
        log.info("Testing encryption/decryption...");

        try {
            String testMessage = "test encryption for wallet security";

            // Encrypt
            String encrypted = encryptionUtil.encrypt(testMessage);

            // Decrypt
            String decrypted = encryptionUtil.decrypt(encrypted);

            boolean isWorking = testMessage.equals(decrypted);

            Map<String, Object> response = new HashMap<>();
            response.put("success", isWorking);
            response.put("message", isWorking ? "Encryption/Decryption working correctly" : "Encryption/Decryption failed");
            response.put("algorithm", "AES-256-GCM");
            response.put("keyConfigured", isWorking);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Encryption test failed", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Encryption test failed");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // Helper methods
    private boolean isValidEncryptedData(String data) {
        try {
            Base64.getDecoder().decode(data);
            byte[] decoded = Base64.getDecoder().decode(data);
            return decoded.length >= (12 + 16 + 1); // IV(12) + TAG(16) + DATA(>=1)
        } catch (Exception e) {
            return false;
        }
    }

    private String decryptMnemonic(String encryptedMnemonic) {
        try {
            if (isValidEncryptedData(encryptedMnemonic)) {
                return encryptionUtil.decrypt(encryptedMnemonic);
            } else {
                // Legacy unencrypted mnemonic
                return encryptedMnemonic;
            }
        } catch (Exception e) {
            throw new RuntimeException("Mnemonic decryption failed", e);
        }
    }
}
