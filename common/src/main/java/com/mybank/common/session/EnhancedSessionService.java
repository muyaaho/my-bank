package com.mybank.common.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Enhanced Global Session Management Service
 * Supports both blocking and reactive operations
 * Provides centralized session management across all microservices using Redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedSessionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    private static final String SESSION_PREFIX = "mybank:session:";
    private static final String TOKEN_PREFIX = "mybank:token:";
    private static final String BLACKLIST_PREFIX = "mybank:blacklist:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(30); // 30 minutes sliding window
    private static final Duration TOKEN_TTL = Duration.ofHours(24); // 24 hours

    /**
     * Create user session (Blocking)
     */
    public void createSession(String userId, String token, UserSession sessionData) {
        String sessionKey = SESSION_PREFIX + userId;
        String tokenKey = TOKEN_PREFIX + token;

        // Store session data by user ID
        redisTemplate.opsForValue().set(sessionKey, sessionData, SESSION_TTL);

        // Store token mapping for quick validation
        redisTemplate.opsForValue().set(tokenKey, userId, TOKEN_TTL);

        log.info("Created session for user: {} with TTL: {}", userId, SESSION_TTL);
    }

    /**
     * Create user session (Reactive)
     */
    public Mono<Void> createSessionReactive(String userId, String token, UserSession sessionData) {
        String sessionKey = SESSION_PREFIX + userId;
        String tokenKey = TOKEN_PREFIX + token;

        return reactiveRedisTemplate.opsForValue()
                .set(sessionKey, sessionData, SESSION_TTL)
                .flatMap(success -> reactiveRedisTemplate.opsForValue().set(tokenKey, userId, TOKEN_TTL))
                .doOnSuccess(success -> log.info("Created reactive session for user: {}", userId))
                .then();
    }

    /**
     * Get user session (Blocking)
     */
    public Optional<UserSession> getSession(String userId) {
        String sessionKey = SESSION_PREFIX + userId;
        UserSession session = (UserSession) redisTemplate.opsForValue().get(sessionKey);
        return Optional.ofNullable(session);
    }

    /**
     * Get user session (Reactive)
     */
    public Mono<UserSession> getSessionReactive(String userId) {
        String sessionKey = SESSION_PREFIX + userId;
        return reactiveRedisTemplate.opsForValue()
                .get(sessionKey)
                .cast(UserSession.class);
    }

    /**
     * Validate token and get user ID (Blocking)
     */
    public Optional<String> validateToken(String token) {
        // Check if token is blacklisted
        if (isTokenBlacklisted(token)) {
            log.warn("Token is blacklisted: {}", token.substring(0, 10) + "...");
            return Optional.empty();
        }

        String tokenKey = TOKEN_PREFIX + token;
        String userId = (String) redisTemplate.opsForValue().get(tokenKey);
        return Optional.ofNullable(userId);
    }

    /**
     * Validate token and get user ID (Reactive)
     */
    public Mono<String> validateTokenReactive(String token) {
        return isTokenBlacklistedReactive(token)
                .flatMap(isBlacklisted -> {
                    if (isBlacklisted) {
                        log.warn("Token is blacklisted: {}", token.substring(0, 10) + "...");
                        return Mono.empty();
                    }
                    String tokenKey = TOKEN_PREFIX + token;
                    return reactiveRedisTemplate.opsForValue()
                            .get(tokenKey)
                            .cast(String.class);
                });
    }

    /**
     * Refresh session TTL (Sliding window) (Blocking)
     */
    public void refreshSession(String userId) {
        String sessionKey = SESSION_PREFIX + userId;

        // Get current session
        UserSession session = (UserSession) redisTemplate.opsForValue().get(sessionKey);
        if (session != null) {
            // Update last accessed time
            session.setLastAccessedAt(LocalDateTime.now());

            // Reset TTL
            redisTemplate.opsForValue().set(sessionKey, session, SESSION_TTL);
            log.debug("Refreshed session for user: {}", userId);
        }
    }

    /**
     * Refresh session TTL (Reactive)
     */
    public Mono<Void> refreshSessionReactive(String userId) {
        String sessionKey = SESSION_PREFIX + userId;

        return reactiveRedisTemplate.opsForValue()
                .get(sessionKey)
                .cast(UserSession.class)
                .flatMap(session -> {
                    session.setLastAccessedAt(LocalDateTime.now());
                    return reactiveRedisTemplate.opsForValue()
                            .set(sessionKey, session, SESSION_TTL);
                })
                .doOnSuccess(success -> log.debug("Refreshed reactive session for user: {}", userId))
                .then();
    }

    /**
     * Invalidate session (Logout) (Blocking)
     */
    public void invalidateSession(String userId, String token) {
        String sessionKey = SESSION_PREFIX + userId;
        String tokenKey = TOKEN_PREFIX + token;
        String blacklistKey = BLACKLIST_PREFIX + token;

        // Delete session and token mapping
        redisTemplate.delete(sessionKey);
        redisTemplate.delete(tokenKey);

        // Add token to blacklist
        redisTemplate.opsForValue().set(blacklistKey, "1", TOKEN_TTL);

        log.info("Invalidated session for user: {}", userId);
    }

    /**
     * Invalidate session (Reactive)
     */
    public Mono<Void> invalidateSessionReactive(String userId, String token) {
        String sessionKey = SESSION_PREFIX + userId;
        String tokenKey = TOKEN_PREFIX + token;
        String blacklistKey = BLACKLIST_PREFIX + token;

        return reactiveRedisTemplate.delete(sessionKey)
                .flatMap(count -> reactiveRedisTemplate.delete(tokenKey))
                .flatMap(count -> reactiveRedisTemplate.opsForValue().set(blacklistKey, "1", TOKEN_TTL))
                .doOnSuccess(success -> log.info("Invalidated reactive session for user: {}", userId))
                .then();
    }

    /**
     * Check if token is blacklisted (Blocking)
     */
    public boolean isTokenBlacklisted(String token) {
        String blacklistKey = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }

    /**
     * Check if token is blacklisted (Reactive)
     */
    public Mono<Boolean> isTokenBlacklistedReactive(String token) {
        String blacklistKey = BLACKLIST_PREFIX + token;
        return reactiveRedisTemplate.hasKey(blacklistKey);
    }

    /**
     * Check if session exists (Blocking)
     */
    public boolean hasSession(String userId) {
        String sessionKey = SESSION_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey));
    }

    /**
     * Check if session exists (Reactive)
     */
    public Mono<Boolean> hasSessionReactive(String userId) {
        String sessionKey = SESSION_PREFIX + userId;
        return reactiveRedisTemplate.hasKey(sessionKey);
    }
}
