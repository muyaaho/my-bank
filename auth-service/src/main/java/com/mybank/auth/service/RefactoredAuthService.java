package com.mybank.auth.service;

import com.mybank.auth.dto.LoginRequest;
import com.mybank.auth.dto.LoginResponse;
import com.mybank.auth.dto.RegisterRequest;
import com.mybank.auth.model.AuthCredential;
import com.mybank.auth.repository.AuthCredentialRepository;
import com.mybank.common.client.UserServiceClient;
import com.mybank.common.dto.CreateUserRequestDto;
import com.mybank.common.dto.UserResponseDto;
import com.mybank.common.exception.BusinessException;
import com.mybank.common.security.JwtUtil;
import com.mybank.common.session.EnhancedSessionService;
import com.mybank.common.session.UserSession;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Refactored Authentication Service
 * Focuses only on authentication logic
 * Delegates user management to user-service via Feign client
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefactoredAuthService {

    private final AuthCredentialRepository authCredentialRepository;
    private final UserServiceClient userServiceClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EnhancedSessionService sessionService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 30;

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        // Check if email already exists in auth credentials
        if (authCredentialRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("USER_EXISTS", "User with this email already exists");
        }

        // Generate user ID
        String userId = UUID.randomUUID().toString();

        // Create auth credentials
        AuthCredential credential = AuthCredential.builder()
                .userId(userId)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .isLocked(false)
                .failedLoginAttempts(0)
                .lastPasswordChangeAt(LocalDateTime.now())
                .build();

        credential = authCredentialRepository.save(credential);

        // Create user profile in user-service via Feign
        try {
            CreateUserRequestDto createUserRequest = CreateUserRequestDto.builder()
                    .id(userId)
                    .email(request.getEmail())
                    .name(request.getName())
                    .phoneNumber(request.getPhoneNumber())
                    .roles(Set.of("USER"))
                    .build();

            UserResponseDto user = userServiceClient.createUser(createUserRequest);
            log.info("User profile created successfully: {}", user.getId());

            // Generate tokens
            String accessToken = generateAccessToken(user);
            String refreshToken = generateRefreshToken(user);

            // Create session
            createUserSession(user, accessToken);

            return buildLoginResponse(user, accessToken, refreshToken);

        } catch (Exception e) {
            // Rollback: delete auth credential if user creation fails
            authCredentialRepository.delete(credential);
            log.error("Failed to create user profile: {}", e.getMessage());
            throw new BusinessException("USER_CREATION_FAILED", "Failed to create user profile");
        }
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.getEmail());

        // Find auth credentials
        AuthCredential credential = authCredentialRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "Invalid email or password"));

        // Check if account is locked
        if (credential.isLocked()) {
            if (credential.getLockedUntil() != null && LocalDateTime.now().isAfter(credential.getLockedUntil())) {
                // Unlock account
                credential.setLocked(false);
                credential.setFailedLoginAttempts(0);
                credential.setLockedUntil(null);
                authCredentialRepository.save(credential);
            } else {
                throw new BusinessException("ACCOUNT_LOCKED",
                        "Account is locked due to multiple failed login attempts. Please try again later.");
            }
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), credential.getPassword())) {
            handleFailedLogin(credential);
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid email or password");
        }

        // Get user profile from user-service
        UserResponseDto user;
        try {
            user = userServiceClient.getUserById(credential.getUserId());
        } catch (Exception e) {
            log.error("Failed to get user profile: {}", e.getMessage());
            throw new BusinessException("USER_NOT_FOUND", "User profile not found");
        }

        // Check if user is active
        if (!user.isActive()) {
            throw new BusinessException("ACCOUNT_INACTIVE", "Account is inactive");
        }

        // Reset failed login attempts on successful login
        credential.setFailedLoginAttempts(0);
        credential.setLastLoginAt(LocalDateTime.now());
        authCredentialRepository.save(credential);

        // Update last login in user-service
        try {
            userServiceClient.updateLastLogin(user.getId());
        } catch (Exception e) {
            log.warn("Failed to update last login: {}", e.getMessage());
        }

        // Generate tokens
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);

        // Create global session in Redis
        createUserSession(user, accessToken);

        return buildLoginResponse(user, accessToken, refreshToken);
    }

    public void logout(String userId, String token) {
        log.info("User logout: {}", userId);

        // Invalidate session using EnhancedSessionService
        sessionService.invalidateSession(userId, token);

        // Session service already adds token to blacklist
        log.info("Logout successful for user: {}", userId);
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException("INVALID_TOKEN", "Invalid refresh token");
        }

        String userId = jwtUtil.getUserIdFromToken(refreshToken);

        // Get user from user-service
        UserResponseDto user;
        try {
            user = userServiceClient.getUserById(userId);
        } catch (Exception e) {
            log.error("Failed to get user profile: {}", e.getMessage());
            throw new BusinessException("USER_NOT_FOUND", "User not found");
        }

        String newAccessToken = generateAccessToken(user);

        return buildLoginResponse(user, newAccessToken, refreshToken);
    }

    private void handleFailedLogin(AuthCredential credential) {
        credential.setFailedLoginAttempts(credential.getFailedLoginAttempts() + 1);

        if (credential.getFailedLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
            credential.setLocked(true);
            credential.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            log.warn("Account locked due to multiple failed login attempts: {}", credential.getEmail());
        }

        authCredentialRepository.save(credential);
    }

    private String generateAccessToken(UserResponseDto user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("name", user.getName());
        claims.put("roles", user.getRoles());

        return jwtUtil.generateToken(user.getId(), claims);
    }

    private String generateRefreshToken(UserResponseDto user) {
        return jwtUtil.generateToken(user.getId());
    }

    private void createUserSession(UserResponseDto user, String token) {
        UserSession session = UserSession.builder()
                .userId(user.getId())
                .username(user.getEmail())
                .roles(user.getRoles())
                .token(token)
                .createdAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .build();

        sessionService.createSession(user.getId(), token, session);
        log.info("Created global session for user: {}", user.getId());
    }

    private LoginResponse buildLoginResponse(UserResponseDto user, String accessToken, String refreshToken) {
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
