package com.UsdtWallet.UsdtWallet.util;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.*;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

@Component
public class TronAddressUtil {

    private static final int TRON_COIN_TYPE = 195;
    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    /**
     * Generate new mnemonic (12 words)
     */
    public String generateMnemonic() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] entropy = new byte[16]; // 128 bits = 12 words
        secureRandom.nextBytes(entropy);

        try {
            List<String> words = MnemonicCode.INSTANCE.toMnemonic(entropy);
            return String.join(" ", words);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate mnemonic", e);
        }
    }

    /**
     * Create seed from mnemonic
     */
    public byte[] seedFromMnemonic(String mnemonic, String passphrase) {
        try {
            List<String> words = Arrays.asList(mnemonic.trim().split("\\s+"));
            return MnemonicCode.toSeed(words, passphrase == null ? "" : passphrase);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create seed from mnemonic", e);
        }
    }

    /**
     * Derive child key from master seed
     * Path: m/44'/195'/0'/0/{index}
     */
    public DeterministicKey deriveChildKey(byte[] seed, int index) {
        try {
            DeterministicKey masterKey = HDKeyDerivation.createMasterPrivateKey(seed);

            // Build derivation path: m/44'/195'/0'/0/{index}
            DeterministicHierarchy hierarchy = new DeterministicHierarchy(masterKey);

            // m/44'
            DeterministicKey purposeKey = hierarchy.deriveChild(
                    Arrays.asList(new ChildNumber(44, true)), false, true, new ChildNumber(44, true)
            );

            // /195' (TRON)
            DeterministicKey coinKey = HDKeyDerivation.deriveChildKey(purposeKey, new ChildNumber(TRON_COIN_TYPE, true));

            // /0' (account)
            DeterministicKey accountKey = HDKeyDerivation.deriveChildKey(coinKey, new ChildNumber(0, true));

            // /0 (change)
            DeterministicKey changeKey = HDKeyDerivation.deriveChildKey(accountKey, new ChildNumber(0, false));

            // /{index} (address index)
            return HDKeyDerivation.deriveChildKey(changeKey, new ChildNumber(index, false));

        } catch (Exception e) {
            throw new RuntimeException("Failed to derive child key", e);
        }
    }

    /**
     * Convert public key to Tron address
     */
    public String publicKeyToTronAddress(ECKey ecKey) {
        try {
            // Get uncompressed public key (65 bytes: 0x04 + 32 bytes X + 32 bytes Y)
            byte[] pubKey = ecKey.decompress().getPubKey();

            // Remove the first byte (0x04)
            byte[] pubKeyNoPrefix = Arrays.copyOfRange(pubKey, 1, pubKey.length);

            // Keccak256 hash
            Keccak.DigestKeccak keccak = new Keccak.Digest256();
            byte[] hash = keccak.digest(pubKeyNoPrefix);

            // Take last 20 bytes
            byte[] addressBytes = Arrays.copyOfRange(hash, hash.length - 20, hash.length);

            // Add Tron prefix (0x41)
            byte[] tronAddress = new byte[21];
            tronAddress[0] = 0x41;
            System.arraycopy(addressBytes, 0, tronAddress, 1, 20);

            // Base58Check encode
            return base58CheckEncode(tronAddress);

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert public key to Tron address", e);
        }
    }

    /**
     * Base58Check encoding for Tron addresses
     */
    private String base58CheckEncode(byte[] input) {
        try {
            // Double SHA256 for checksum
            byte[] hash1 = org.bitcoinj.core.Sha256Hash.hash(input);
            byte[] hash2 = org.bitcoinj.core.Sha256Hash.hash(hash1);

            // Take first 4 bytes as checksum
            byte[] checksum = Arrays.copyOfRange(hash2, 0, 4);

            // Append checksum to input
            byte[] addressWithChecksum = new byte[input.length + 4];
            System.arraycopy(input, 0, addressWithChecksum, 0, input.length);
            System.arraycopy(checksum, 0, addressWithChecksum, input.length, 4);

            // Base58 encode (use simple version without double checksum)
            return base58EncodeSimple(addressWithChecksum);

        } catch (Exception e) {
            throw new RuntimeException("Failed to base58check encode", e);
        }
    }

    /**
     * Simple Base58 encoding (without additional checksum)
     */
    private static String base58EncodeSimple(byte[] input) {
        if (input.length == 0) {
            return "";
        }

        // Count leading zeros
        int zeroCount = 0;
        for (byte b : input) {
            if (b == 0) {
                zeroCount++;
            } else {
                break;
            }
        }

        // Convert to base58
        BigInteger value = new BigInteger(1, input);
        StringBuilder sb = new StringBuilder();

        while (value.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divmod = value.divideAndRemainder(BigInteger.valueOf(58));
            value = divmod[0];
            sb.append(ALPHABET.charAt(divmod[1].intValue()));
        }

        // Add leading '1's for leading zeros
        for (int i = 0; i < zeroCount; i++) {
            sb.append('1');
        }

        return sb.reverse().toString();
    }

    /**
     * Base58 encode with checksum (for static utility methods)
     */
    private static String base58Encode(byte[] input) {
        // Calculate checksum
        byte[] hash1 = sha256(input);
        byte[] hash2 = sha256(hash1);
        byte[] checksum = Arrays.copyOfRange(hash2, 0, 4);

        // Append checksum
        byte[] inputWithChecksum = new byte[input.length + checksum.length];
        System.arraycopy(input, 0, inputWithChecksum, 0, input.length);
        System.arraycopy(checksum, 0, inputWithChecksum, input.length, checksum.length);

        // Use simple encoding
        return base58EncodeSimple(inputWithChecksum);
    }

    /**
     * Get private key as hex string
     */
    public String getPrivateKeyHex(DeterministicKey key) {
        return Hex.toHexString(key.getPrivKeyBytes());
    }

    /**
     * Derive wallet info (address + private key)
     */
    public WalletInfo deriveWallet(String mnemonic, int index) {
        byte[] seed = seedFromMnemonic(mnemonic, null);
        DeterministicKey childKey = deriveChildKey(seed, index);
        String address = publicKeyToTronAddress(childKey);
        String privateKey = getPrivateKeyHex(childKey);

        return new WalletInfo(index, address, privateKey);
    }

    /**
     * Convert Tron Base58 address to Hex format (for API calls)
     * T... -> 0x...
     */
    public static String base58ToHex(String base58Address) {
        try {
            if (base58Address == null || !base58Address.startsWith("T")) {
                return base58Address;
            }

            byte[] decoded = base58Decode(base58Address);
            // Remove the first byte (0x41 for Tron) and last 4 bytes (checksum)
            byte[] addressBytes = Arrays.copyOfRange(decoded, 1, decoded.length - 4);
            return "0x" + Hex.toHexString(addressBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Base58 to Hex: " + base58Address, e);
        }
    }

    /**
     * Convert Hex address to Tron Base58 format (for storage)
     * 0x... -> T...
     */
    public static String hexToBase58(String hexAddress) {
        try {
            if (hexAddress == null || !hexAddress.startsWith("0x")) {
                return hexAddress;
            }

            // Remove 0x prefix
            String hex = hexAddress.substring(2);
            byte[] addressBytes = Hex.decode(hex);

            // Add Tron prefix (0x41)
            byte[] fullAddress = new byte[addressBytes.length + 1];
            fullAddress[0] = 0x41;
            System.arraycopy(addressBytes, 0, fullAddress, 1, addressBytes.length);

            return base58Encode(fullAddress);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Hex to Base58: " + hexAddress, e);
        }
    }

    /**
     * Base58 decode implementation
     */
    private static byte[] base58Decode(String input) {
        if (input.length() == 0) {
            return new byte[0];
        }

        // Convert to big integer
        BigInteger decoded = BigInteger.ZERO;
        BigInteger multi = BigInteger.ONE;
        char[] chars = input.toCharArray();

        for (int i = chars.length - 1; i >= 0; i--) {
            int digit = ALPHABET.indexOf(chars[i]);
            if (digit == -1) {
                throw new IllegalArgumentException("Invalid Base58 character: " + chars[i]);
            }
            decoded = decoded.add(multi.multiply(BigInteger.valueOf(digit)));
            multi = multi.multiply(BigInteger.valueOf(58));
        }

        byte[] result = decoded.toByteArray();

        // Handle leading zeros
        int leadingZeros = 0;
        for (char c : chars) {
            if (c == '1') leadingZeros++;
            else break;
        }

        if (leadingZeros > 0) {
            byte[] withZeros = new byte[result.length + leadingZeros];
            System.arraycopy(result, 0, withZeros, leadingZeros, result.length);
            return withZeros;
        }

        return result;
    }

    /**
     * SHA256 hash
     */
    private static byte[] sha256(byte[] input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (Exception e) {
            throw new RuntimeException("SHA256 failed", e);
        }
    }

    /**
     * Data class for wallet information
     */
    public record WalletInfo(int index, String address, String privateKey) {}
}
