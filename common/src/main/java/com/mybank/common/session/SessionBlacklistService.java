package com.mybank.common.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Production-Grade Token Blacklist Service
 *
 * Stateless JWT Authentication with Token Revocation:
 * - JWT carries all user information (userId, roles, etc.)
 * - Redis ONLY stores revoked/logged-out token hashes
 * - No session objects, no serialization issues
 * - Industry standard pattern (Netflix, Uber, Spotify)
 *
 * Why Blacklist instead of Session?
 * 1. Performance: 1 Redis READ vs 2 Redis ops (READ + WRITE)
 * 2. Simplicity: No serialization/deserialization
 * 3. Scalability: Minimal Redis memory (only logged-out tokens)
 * 4. Stateless: JWT is the source of truth
 *
 * Operations:
 * - addToBlacklist(): Called on logout
 * - isBlacklisted(): Called on every request (fast)
 * - Token TTL matches JWT expiration (auto cleanup)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate;

    private static final String BLACKLIST_PREFIX = "mybank:blacklist:";
    private static final Duration TOKEN_TTL = Duration.ofHours(24); // Match JWT expiration

    /**
     * Add token to blacklist (Logout)
     * @param token JWT token to revoke
     */
    public void addToBlacklist(String token) {
        String tokenHash = hashToken(token);
        String blacklistKey = BLACKLIST_PREFIX + tokenHash;

        redisTemplate.opsForValue().set(blacklistKey, "1", TOKEN_TTL);
        log.info("Token added to blacklist: {}", tokenHash);
    }

    /**
     * Add token to blacklist (Reactive)
     */
    public Mono<Void> addToBlacklistReactive(String token) {
        String tokenHash = hashToken(token);
        String blacklistKey = BLACKLIST_PREFIX + tokenHash;

        return reactiveStringRedisTemplate.opsForValue()
                .set(blacklistKey, "1", TOKEN_TTL)
                .doOnSuccess(success -> log.info("Token added to blacklist (reactive): {}", tokenHash))
                .then();
    }

    /**
     * Check if token is blacklisted (Blocking)
     */
    public boolean isBlacklisted(String token) {
        String tokenHash = hashToken(token);
        String blacklistKey = BLACKLIST_PREFIX + tokenHash;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }

    /**
     * Check if token is blacklisted (Reactive)
     */
    public Mono<Boolean> isBlacklistedReactive(String token) {
        String tokenHash = hashToken(token);
        String blacklistKey = BLACKLIST_PREFIX + tokenHash;
        return reactiveStringRedisTemplate.hasKey(blacklistKey)
                .defaultIfEmpty(false);
    }

    /**
     * Hash token using SHA-256
     * Prevents token exposure in Redis, logs, monitoring
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Remove token from blacklist (for testing)
     */
    public void removeFromBlacklist(String token) {
        String tokenHash = hashToken(token);
        String blacklistKey = BLACKLIST_PREFIX + tokenHash;
        redisTemplate.delete(blacklistKey);
        log.info("Token removed from blacklist: {}", tokenHash);
    }

    /**
     * Clear all blacklisted tokens (for maintenance)
     */
    public void clearBlacklist() {
        String pattern = BLACKLIST_PREFIX + "*";
        redisTemplate.delete(redisTemplate.keys(pattern));
        log.warn("All blacklisted tokens cleared");
    }
}
