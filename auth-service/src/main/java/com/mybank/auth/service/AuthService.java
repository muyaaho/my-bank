package com.mybank.auth.service;

import com.mybank.auth.dto.LoginRequest;
import com.mybank.auth.dto.LoginResponse;
import com.mybank.auth.dto.RegisterRequest;
import com.mybank.auth.model.User;
import com.mybank.auth.repository.UserRepository;
import com.mybank.common.exception.BusinessException;
import com.mybank.common.security.JwtUtil;
import com.mybank.common.session.SessionBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Authentication service implementation
 * Handles user registration, login, and token management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SessionBlacklistService blacklistService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 30;

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("USER_EXISTS", "User with this email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .roles(Set.of("USER"))
                .isActive(true)
                .isLocked(false)
                .build();

        user = userRepository.save(user);

        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);

        return buildLoginResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "Invalid email or password"));

        if (user.isLocked()) {
            throw new BusinessException("ACCOUNT_LOCKED",
                    "Account is locked due to multiple failed login attempts. Please try again later.");
        }

        if (!user.isActive()) {
            throw new BusinessException("ACCOUNT_INACTIVE", "Account is inactive");
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user);
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid email or password");
        }

        // Reset failed login attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);

        // No session creation needed - JWT is stateless!
        // Token contains all user info (userId, roles, etc.)
        log.info("User logged in successfully: {}", user.getId());

        return buildLoginResponse(user, accessToken, refreshToken);
    }

    /**
     * Logout user by adding token to blacklist
     * Production pattern: No session to invalidate, just blacklist the token
     */
    public void logout(String userId, String token) {
        log.info("User logout: {}", userId);

        // Add token to blacklist (production pattern)
        blacklistService.addToBlacklist(token);

        log.info("Token blacklisted successfully for user: {}", userId);
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException("INVALID_TOKEN", "Invalid refresh token");
        }

        String userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));

        String newAccessToken = generateAccessToken(user);

        return buildLoginResponse(user, newAccessToken, refreshToken);
    }

    private void handleFailedLogin(User user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

        if (user.getFailedLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
            user.setLocked(true);
            log.warn("Account locked due to multiple failed login attempts: {}", user.getEmail());
        }

        userRepository.save(user);
    }

    private String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("name", user.getName());
        claims.put("roles", user.getRoles());

        return jwtUtil.generateToken(user.getId(), claims);
    }

    private String generateRefreshToken(User user) {
        return jwtUtil.generateToken(user.getId());
    }

    // REMOVED: createUserSession method
    // Production pattern: JWT is stateless, no session creation needed
    // All user info is in the JWT payload (userId, roles, email)

    private LoginResponse buildLoginResponse(User user, String accessToken, String refreshToken) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400) // 24 hours in seconds
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .build())
                .build();
    }
}
