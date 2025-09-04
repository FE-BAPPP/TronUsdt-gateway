package com.UsdtWallet.UsdtWallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthTokenService {

    @Qualifier("customStringRedisTemplate")
    private final RedisTemplate<String, String> stringRedis;

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String RESET_PREFIX = "auth:reset:";
    private static final String RESET_USER_PREFIX = "auth:reset:user:";
    private static final String RESET_THROTTLE_EMAIL = "auth:reset:throttle:email:";
    private static final String RESET_THROTTLE_IP = "auth:reset:throttle:ip:";

    public void blacklistToken(String token, long ttlMillis) {
        try {
            long ttlSec = Math.max(1, ttlMillis / 1000);
            stringRedis.opsForValue().set(BLACKLIST_PREFIX + token, "1", ttlSec, TimeUnit.SECONDS);
            log.info("Blacklisted token for {} seconds", ttlSec);
        } catch (Exception e) {
            log.error("Failed to blacklist token", e);
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            Boolean exists = stringRedis.hasKey(BLACKLIST_PREFIX + token);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Blacklist check failed, defaulting to not blacklisted", e);
            return false;
        }
    }

    /**
     * Create reset token
     */
    public String createPasswordResetToken(String userId, Duration ttl) {
        String token = UUID.randomUUID().toString();
        try {
            stringRedis.opsForValue().set(RESET_PREFIX + token, userId, ttl.getSeconds(), TimeUnit.SECONDS);
            log.info("Created password reset token for user {} with TTL {}s", userId, ttl.getSeconds());
        } catch (Exception e) {
            log.error("Failed to create password reset token", e);
            throw e;
        }
        return token;
    }

    /**
     * Create a new reset token and invalidate any previous token for this user.
     */
    public String createPasswordResetTokenReplacingPrevious(String userId, Duration ttl) {
        try {
            // Invalidate previous token if any
            String userKey = RESET_USER_PREFIX + userId;
            String previousToken = stringRedis.opsForValue().get(userKey);
            if (previousToken != null) {
                stringRedis.delete(RESET_PREFIX + previousToken);
                log.debug("Invalidated previous reset token for user {}", userId);
            }

            // Create new token
            String token = UUID.randomUUID().toString();
            long ttlSeconds = ttl.getSeconds();
            stringRedis.opsForValue().set(RESET_PREFIX + token, userId, ttlSeconds, TimeUnit.SECONDS);
            stringRedis.opsForValue().set(userKey, token, ttlSeconds, TimeUnit.SECONDS);
            log.info("Created new reset token for user {} with single-active guarantee", userId);
            return token;
        } catch (Exception e) {
            log.error("Failed to create replacing reset token", e);
            throw e;
        }
    }

    public String validateResetToken(String token) {
        try {
            return stringRedis.opsForValue().get(RESET_PREFIX + token);
        } catch (Exception e) {
            log.error("Failed to validate reset token", e);
            return null;
        }
    }

    public void consumeResetToken(String token) {
        try {
            String userId = stringRedis.opsForValue().get(RESET_PREFIX + token);
            if (userId != null) {
                // remove token and user mapping
                stringRedis.delete(RESET_USER_PREFIX + userId);
            }
            stringRedis.delete(RESET_PREFIX + token);
        } catch (Exception e) {
            log.warn("Failed to consume reset token", e);
        }
    }

    public boolean acquireResetThrottle(String email, String ip) {
        boolean emailOk = true;
        boolean ipOk = true;
        try {
            // 60s per email
            emailOk = Boolean.TRUE.equals(stringRedis.opsForValue().setIfAbsent(
                RESET_THROTTLE_EMAIL + email.toLowerCase(), "1", 60, TimeUnit.SECONDS));
        } catch (Exception e) {
            log.debug("Email throttle check failed, allowing by default: {}", e.getMessage());
        }
        try {
            // 15s per IP
            ipOk = Boolean.TRUE.equals(stringRedis.opsForValue().setIfAbsent(
                RESET_THROTTLE_IP + ip, "1", 15, TimeUnit.SECONDS));
        } catch (Exception e) {
            log.debug("IP throttle check failed, allowing by default: {}", e.getMessage());
        }
        return emailOk && ipOk;
    }
}
