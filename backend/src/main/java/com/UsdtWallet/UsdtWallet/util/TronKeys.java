package com.UsdtWallet.UsdtWallet.util;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bitcoinj.core.Base58;
import org.bouncycastle.jcajce.provider.digest.Keccak;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Utility class for Tron key operations and address derivation
 */
public final class TronKeys {
    private TronKeys() {}

    /**
     * Derive Tron Base58 address from private key hex
     */
    public static String privateKeyToBase58Address(String privHex) {
        try {
            // Remove 0x prefix if present
            if (privHex.startsWith("0x")) {
                privHex = privHex.substring(2);
            }

            // 1) Generate uncompressed public key (0x04 || X || Y)
            X9ECParameters params = CustomNamedCurves.getByName("secp256k1");
            ECPoint Q = params.getG().multiply(new BigInteger(privHex, 16)).normalize();
            byte[] pubUncompressed = Q.getEncoded(false); // 65 bytes, starts with 0x04

            // 2) Tron/EVM address: keccak256(pubkey without 0x04) -> last 20 bytes
            byte[] pubNoPrefix = Arrays.copyOfRange(pubUncompressed, 1, pubUncompressed.length);

            // Use Keccak-256 (not SHA3-256)
            Keccak.Digest256 keccak = new Keccak.Digest256();
            keccak.update(pubNoPrefix, 0, pubNoPrefix.length);
            byte[] hash = keccak.digest();

            byte[] addr20 = Arrays.copyOfRange(hash, 12, 32); // last 20 bytes

            // 3) Tron adds network prefix 0x41
            byte[] addr21 = new byte[21];
            addr21[0] = 0x41;
            System.arraycopy(addr20, 0, addr21, 1, 20);

            // 4) Base58Check encoding
            byte[] checksum = sha256d(addr21); // double SHA-256
            byte[] payload = new byte[25];
            System.arraycopy(addr21, 0, payload, 0, 21);
            System.arraycopy(checksum, 0, payload, 21, 4);

            return Base58.encode(payload);

        } catch (Exception e) {
            throw new RuntimeException("Failed to derive address from private key", e);
        }
    }

    /**
     * Double SHA-256 hash
     */
    private static byte[] sha256d(byte[] data) {
        SHA256Digest digest = new SHA256Digest();

        // First SHA-256
        digest.update(data, 0, data.length);
        byte[] firstHash = new byte[32];
        digest.doFinal(firstHash, 0);

        // Second SHA-256
        digest.reset();
        digest.update(firstHash, 0, firstHash.length);
        byte[] secondHash = new byte[32];
        digest.doFinal(secondHash, 0);

        return secondHash;
    }

    /**
     * Validate that private key generates the expected address
     */
    public static void validatePrivateKeyForAddress(String privateKeyHex, String expectedAddress) {
        String derivedAddress = privateKeyToBase58Address(privateKeyHex);
        if (!derivedAddress.equals(expectedAddress)) {
            throw new IllegalStateException(
                String.format("Private key/address mismatch: key => %s, expected => %s",
                    derivedAddress, expectedAddress)
            );
        }
    }
}
