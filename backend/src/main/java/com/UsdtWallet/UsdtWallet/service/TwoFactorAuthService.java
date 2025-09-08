package com.UsdtWallet.UsdtWallet.service;

import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;

@Service
public class TwoFactorAuthService {

    private static final Duration STEP = Duration.ofSeconds(30);
    private static final int DIGITS = 6; // for otpauth URL only

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base32 base32 = new Base32();

    @Value("${app.name:UsdtWallet}")
    private String appName;

    public String generateSecret() {
        byte[] randomBytes = new byte[20]; // 160-bit secret
        secureRandom.nextBytes(randomBytes);
        String secret = base32.encodeAsString(randomBytes).replace("=", "");
        return secret.toUpperCase();
    }

    public boolean verifyCode(String base32Secret, int code) {
        if (base32Secret == null || base32Secret.isBlank()) return false;
        try {
            byte[] keyBytes = base32.decode(base32Secret);
            SecretKey secretKey = new SecretKeySpec(keyBytes, "RAW");
            TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator(STEP);
            Instant now = Instant.now();
            if (totp.generateOneTimePassword(secretKey, now) == code) return true;
            if (totp.generateOneTimePassword(secretKey, now.minus(STEP)) == code) return true;
            return totp.generateOneTimePassword(secretKey, now.plus(STEP)) == code;
        } catch (Exception e) {
            return false;
        }
    }

    public String buildOtpAuthUrl(String accountName, String secret) {
        String issuer = urlEncode(appName);
        String account = urlEncode(accountName);
        return "otpauth://totp/" + issuer + ":" + account +
               "?secret=" + secret +
               "&issuer=" + issuer +
               "&digits=" + DIGITS +
               "&period=" + STEP.toSeconds();
    }

    private String urlEncode(String s) {
        try { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
        catch (Exception e) { return s; }
    }
}
