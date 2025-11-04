package com.mybank.common.session;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Global Session Management Service
 *
 * Provides centralized session management across all microservices using Redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_PREFIX = "mybank:session:";
    private static final String TOKEN_PREFIX = "mybank:token:";
    private static final long SESSION_TTL = 1800; // 30 minutes in seconds

    /**
     * Store user session data
     *
     * @param userId User ID
     * @param token JWT token
     * @param sessionData Session data to store
     */
    public void createSession(String userId, String token, UserSession sessionData) {
        String sessionKey = SESSION_PREFIX + userId;
        String tokenKey = TOKEN_PREFIX + token;

        // Store session data by user ID
        redisTemplate.opsForValue().set(sessionKey, sessionData, SESSION_TTL, TimeUnit.SECONDS);

        // Store token mapping for quick validation
        redisTemplate.opsForValue().set(tokenKey, userId, SESSION_TTL, TimeUnit.SECONDS);

        log.info("Created session for user: {}", userId);
    }

    /**
     * Get user session data
     *
     * @param userId User ID
     * @return Optional containing session data if exists
     */
    public Optional<UserSession> getSession(String userId) {
        String sessionKey = SESSION_PREFIX + userId;
        UserSession session = (UserSession) redisTemplate.opsForValue().get(sessionKey);
        return Optional.ofNullable(session);
    }

    /**
     * Validate token and get user ID
     *
     * @param token JWT token
     * @return Optional containing user ID if token is valid
     */
    public Optional<String> validateToken(String token) {
        String tokenKey = TOKEN_PREFIX + token;
        String userId = (String) redisTemplate.opsForValue().get(tokenKey);
        return Optional.ofNullable(userId);
    }

    /**
     * Refresh session TTL
     *
     * @param userId User ID
     */
    public void refreshSession(String userId) {
        String sessionKey = SESSION_PREFIX + userId;
        redisTemplate.expire(sessionKey, SESSION_TTL, TimeUnit.SECONDS);
        log.debug("Refreshed session for user: {}", userId);
    }

    /**
     * Invalidate session (logout)
     *
     * @param userId User ID
     * @param token JWT token
     */
    public void invalidateSession(String userId, String token) {
        String sessionKey = SESSION_PREFIX + userId;
        String tokenKey = TOKEN_PREFIX + token;

        redisTemplate.delete(sessionKey);
        redisTemplate.delete(tokenKey);

        log.info("Invalidated session for user: {}", userId);
    }

    /**
     * Check if session exists
     *
     * @param userId User ID
     * @return true if session exists
     */
    public boolean hasSession(String userId) {
        String sessionKey = SESSION_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey));
    }
}
