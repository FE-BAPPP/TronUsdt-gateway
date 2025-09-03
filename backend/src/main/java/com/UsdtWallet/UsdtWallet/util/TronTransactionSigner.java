package com.UsdtWallet.UsdtWallet.util;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.util.encoders.Hex;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

@Slf4j
public class TronTransactionSigner {

    private static final ObjectMapper mapper = new ObjectMapper();

    /** Sign transaction theo chuẩn Tron */
    public static String signTransaction(String rawTransactionJson, String privateKeyHex) {
        try {
            log.info("🔐 Signing transaction with Tron standard protocol");
            log.debug("Transaction JSON: {}", rawTransactionJson);

            ObjectNode tx = (ObjectNode) mapper.readTree(rawTransactionJson);
            if (!tx.has("raw_data"))
                throw new IllegalArgumentException("Transaction missing raw_data");

            byte[] rawDataBytes;
            if (tx.has("raw_data_hex")) {
                String rawDataHex = tx.get("raw_data_hex").asText();
                rawDataBytes = Hex.decode(rawDataHex);
                log.info("✅ Using existing raw_data_hex for signing: {}...",
                        rawDataHex.substring(0, Math.min(32, rawDataHex.length())));
            } else {
                // Không tự serialize protobuf ở đây để tránh sai khác
                throw new IllegalArgumentException("Transaction must contain raw_data_hex for proper signing");
            }

            // Validate private key khớp owner_address (nếu trích xuất được)
            JsonNode rawData = tx.get("raw_data");
            String ownerAddress = extractOwnerAddress(rawData);
            if (ownerAddress != null) {
                validatePrivateKeyForOwner(privateKeyHex, ownerAddress);
            } else {
                log.warn("⚠️ Could not extract owner_address from transaction for validation");
            }

            // Tạo chữ ký chuẩn Tron
            String sigHex = createTronSignature(rawDataBytes, privateKeyHex, ownerAddress);

            ArrayNode sigs = mapper.createArrayNode();
            sigs.add(sigHex);
            tx.set("signature", sigs);

            String signed = mapper.writeValueAsString(tx);
            log.info("✅ Transaction signed successfully with signature: {}...", sigHex.substring(0, 20));
            return signed;

        } catch (Exception e) {
            log.error("❌ Failed to sign transaction: {}", e.getMessage(), e);
            throw new RuntimeException("Transaction signing failed: " + e.getMessage(), e);
        }
    }

    /** Trích xuất owner_address từ raw_data */
    private static String extractOwnerAddress(JsonNode rawData) {
        try {
            if (rawData.has("contract") && rawData.get("contract").isArray()) {
                JsonNode c = rawData.get("contract").get(0);
                if (c != null && c.has("parameter") && c.get("parameter").has("value")) {
                    JsonNode v = c.get("parameter").get("value");
                    if (v.has("owner_address")) return v.get("owner_address").asText();
                }
            }
            if (rawData.has("owner_address")) return rawData.get("owner_address").asText();
        } catch (Exception e) {
            log.warn("Error extracting owner_address: {}", e.getMessage());
        }
        return null;
    }

    /** Kiểm tra private key khớp owner address */
    private static void validatePrivateKeyForOwner(String privateKeyHex, String ownerAddress) {
        try {
            if (privateKeyHex.startsWith("0x")) privateKeyHex = privateKeyHex.substring(2);
            BigInteger d = new BigInteger(privateKeyHex, 16);
            ECKey key = ECKey.fromPrivate(d, false);
            String derived = tronAddressFromPubKey(key.getPubKey());
            if (!ownerAddress.equals(derived)) {
                String msg = String.format("Private key mismatch. tx.owner=%s , key.addr=%s", ownerAddress, derived);
                log.error("❌ {}", msg);
                throw new IllegalStateException(msg);
            }
            log.debug("✅ Private key validation passed for owner: {}", ownerAddress);
        } catch (Exception e) {
            throw new RuntimeException("Private key validation failed: " + e.getMessage(), e);
        }
    }

    /** Tạo chữ ký Tron (SHA256 once + secp256k1 low-S + v=recId+27) */
    private static String createTronSignature(byte[] rawDataBytes, String privateKeyHex, String expectedOwner) {
        try {
            // 1) Tron hash = SHA256(raw_data_bytes)
            byte[] hash = Sha256Hash.hash(rawDataBytes);
            log.debug("Raw data hash: {}", Hex.toHexString(hash));

            // 2) ECKey từ private key
            if (privateKeyHex.startsWith("0x")) privateKeyHex = privateKeyHex.substring(2);
            BigInteger d = new BigInteger(privateKeyHex, 16);
            ECKey key = ECKey.fromPrivate(d, false); // uncompressed
            String addrFromKey = tronAddressFromPubKey(key.getPubKey());
            log.info("🔑 Private key {}... => address {}", privateKeyHex.substring(0, 8), addrFromKey);

            // 3) Ký + canonicalize low-S
            ECKey.ECDSASignature sig = key.sign(Sha256Hash.wrap(hash)).toCanonicalised();
            byte[] r = bigInt32(sig.r);
            byte[] s = bigInt32(sig.s);

            // 4) Tìm recovery id (0..3). BitcoinJ dùng 0/1 với compressed toggle → thử đủ 4 TH
            int recId = -1;
            for (int i = 0; i < 4; i++) {
                boolean compressed = (i >= 2);
                int rid = i % 2;
                ECKey rec = ECKey.recoverFromSignature(rid, sig, Sha256Hash.wrap(hash), compressed);
                if (rec != null) {
                    String recAddr = tronAddressFromPubKey(rec.getPubKey());
                    if (recAddr.equals(addrFromKey)) {
                        recId = rid;
                        log.debug("✅ Found recovery id: {} (compressed={})", rid, compressed);
                        break;
                    }
                }
            }
            if (recId == -1) {
                // fallback: 0
                recId = 0;
                log.warn("⚠️ Could not find recovery id. Fallback recId=0");
            }

            // Tron/EVM style v = recId + 27
            byte v = (byte) (recId + 27);

            byte[] sig65 = ByteBuffer.allocate(65).put(r).put(s).put(v).array();
            String sigHex = Hex.toHexString(sig65);
            log.debug("Created signature: {} (len={})", sigHex, sig65.length);

            // 5) Tự verify local (không bắt buộc, nhưng giúp debug)
            if (expectedOwner != null) {
                boolean ok = verifySignature(sigHex, rawDataBytes, expectedOwner);
                if (!ok) {
                    throw new IllegalStateException("Local signature verification failed");
                }
            }

            return sigHex;

        } catch (Exception e) {
            log.error("❌ Failed to create signature: {}", e.getMessage(), e);
            throw new RuntimeException("Signature creation failed: " + e.getMessage(), e);
        }
    }

    /** Verify: recover pubkey từ (r,s,v) rồi suy ra Tron address, so với expectedAddress */
    public static boolean verifySignature(String signatureHex, byte[] rawDataBytes, String expectedAddress) {
        try {
            byte[] sig = Hex.decode(signatureHex);
            if (sig.length != 65) return false;

            byte[] r = Arrays.copyOfRange(sig, 0, 32);
            byte[] s = Arrays.copyOfRange(sig, 32, 64);
            int v = sig[64] & 0xFF;

            // Tron hash = SHA256(raw_data_bytes)
            byte[] hash = Sha256Hash.hash(rawDataBytes);

            ECKey.ECDSASignature ecdsa = new ECKey.ECDSASignature(new BigInteger(1, r), new BigInteger(1, s));
            int recId = v >= 27 ? (v - 27) : v; // chấp nhận cả 0/1 lẫn 27/28

            // Thử cả compressed/uncompressed để chắc chắn
            ECKey recovered = null;
            String recoveredAddr = null;
            for (int i = 0; i < 2; i++) {
                boolean compressed = (i == 1);
                ECKey test = ECKey.recoverFromSignature(recId, ecdsa, Sha256Hash.wrap(hash), compressed);
                if (test != null) {
                    String addr = tronAddressFromPubKey(test.getPubKey());
                    if (expectedAddress.equals(addr)) {
                        recovered = test;
                        recoveredAddr = addr;
                        break;
                    }
                }
            }
            boolean match = (recovered != null);
            log.info("🔍 Verify: expected={}, recovered={}, ok={}", expectedAddress, recoveredAddr, match);
            return match;
        } catch (Exception e) {
            log.debug("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /** Create and sign USDT transfer transaction */
    public static String createTransaction(String fromAddress, String contractAddress,
                                         Map<String, Object> txParams, String privateKey) {
        try {
            log.info("Creating USDT transfer transaction from {} to contract {}", fromAddress, contractAddress);

            // Extract parameters
            String parameter = (String) txParams.get("parameter");
            Long feeLimit = (Long) txParams.get("fee_limit");

            // Create raw transaction JSON structure
            ObjectNode transaction = mapper.createObjectNode();
            ObjectNode rawData = mapper.createObjectNode();

            // Set transaction details
            rawData.put("ref_block_bytes", "0000");
            rawData.put("ref_block_hash", "0000000000000000");
            rawData.put("expiration", System.currentTimeMillis() + 60000); // 1 minute expiration
            rawData.put("fee_limit", feeLimit != null ? feeLimit : 50000000L);
            rawData.put("timestamp", System.currentTimeMillis());

            // Create contract array
            ArrayNode contracts = mapper.createArrayNode();
            ObjectNode contract = mapper.createObjectNode();
            contract.put("type", "TriggerSmartContract");

            ObjectNode contractParam = mapper.createObjectNode();
            ObjectNode value = mapper.createObjectNode();
            value.put("owner_address", fromAddress);
            value.put("contract_address", contractAddress);
            value.put("data", parameter);
            value.put("call_value", 0);

            contractParam.set("value", value);
            contractParam.put("type_url", "type.googleapis.com/protocol.TriggerSmartContract");
            contract.set("parameter", contractParam);

            contracts.add(contract);
            rawData.set("contract", contracts);

            transaction.set("raw_data", rawData);

            // Convert to hex for signing
            String rawDataHex = convertRawDataToHex(rawData);
            transaction.put("raw_data_hex", rawDataHex);

            // Sign the transaction
            String signedTx = signTransaction(transaction.toString(), privateKey);

            log.info("Transaction created and signed successfully");
            return signedTx;

        } catch (Exception e) {
            log.error("Failed to create transaction", e);
            throw new RuntimeException("Transaction creation failed: " + e.getMessage());
        }
    }

    /** Convert raw data to hex format */
    private static String convertRawDataToHex(ObjectNode rawData) {
        // This is a simplified implementation
        // In production, you would use proper protobuf serialization
        try {
            String rawDataJson = mapper.writeValueAsString(rawData);
            return Hex.toHexString(rawDataJson.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert raw data to hex", e);
        }
    }

    /* ------------ Helpers ------------ */

    private static byte[] bigInt32(BigInteger x) {
        byte[] b = Utils.bigIntegerToBytes(x, 32);
        if (b.length == 32) return b;
        byte[] out = new byte[32];
        System.arraycopy(b, 0, out, 32 - b.length, b.length);
        return out;
    }

    /** Tính địa chỉ Tron (base58) từ public key (uncompressed hoặc compressed đều được) */
    private static String tronAddressFromPubKey(byte[] pubKey) {
        // Đảm bảo uncompressed (bắt đầu bằng 0x04, dài 65)
        byte[] uncompressed = ECKey.fromPublicOnly(pubKey).decompress().getPubKeyPoint().getEncoded(false);
        byte[] pubNoPrefix = Arrays.copyOfRange(uncompressed, 1, uncompressed.length); // bỏ 0x04

        // Keccak-256
        Keccak.Digest256 keccak = new Keccak.Digest256();
        byte[] hash = keccak.digest(pubNoPrefix);

        // Lấy 20 byte cuối
        byte[] addr20 = Arrays.copyOfRange(hash, hash.length - 20, hash.length);

        // Thêm prefix 0x41
        byte[] addr21 = new byte[21];
        addr21[0] = 0x41;
        System.arraycopy(addr20, 0, addr21, 1, 20);

        // Base58Check
        return base58Check(addr21);
    }

    private static String base58Check(byte[] payload) {
        byte[] check = Sha256Hash.hashTwice(payload);
        byte[] data = new byte[payload.length + 4];
        System.arraycopy(payload, 0, data, 0, payload.length);
        System.arraycopy(check, 0, data, payload.length, 4);
        return org.bitcoinj.core.Base58.encode(data);
    }
}
