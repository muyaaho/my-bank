package com.mybank.auth.service;

import com.mybank.auth.dto.LoginRequest;
import com.mybank.auth.dto.LoginResponse;
import com.mybank.auth.dto.RegisterRequest;
import com.mybank.auth.model.User;
import com.mybank.auth.repository.UserRepository;
import com.mybank.common.exception.BusinessException;
import com.mybank.common.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Service tests for AuthService
 * Tests business logic with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-123")
                .email("test@mybank.com")
                .password("encodedPassword")
                .name("Test User")
                .phoneNumber("010-1234-5678")
                .roles(Set.of("USER"))
                .isActive(true)
                .isLocked(false)
                .failedLoginAttempts(0)
                .build();

        registerRequest = RegisterRequest.builder()
                .email("test@mybank.com")
                .password("Test1234!")
                .name("Test User")
                .phoneNumber("010-1234-5678")
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@mybank.com")
                .password("Test1234!")
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(eq(testUser.getId()), anyMap())).thenReturn("access-token");
        when(jwtUtil.generateToken(testUser.getId())).thenReturn("refresh-token");

        // When
        LoginResponse response = authService.register(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getId()).isEqualTo("user-123");
        assertThat(response.getUser().getEmail()).isEqualTo("test@mybank.com");

        // Verify interactions
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when registering existing user")
    void shouldThrowExceptionWhenRegisteringExistingUser() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "USER_EXISTS")
                .hasMessageContaining("already exists");

        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfullyWithValidCredentials() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(eq(testUser.getId()), anyMap())).thenReturn("access-token");
        when(jwtUtil.generateToken(testUser.getId())).thenReturn("refresh-token");

        // When
        LoginResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        // Verify user state was updated
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(savedUser.getLastLoginAt()).isNotNull();

        // Verify session was cached
        verify(valueOperations).set(eq("session:user-123"), eq("access-token"), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "INVALID_CREDENTIALS");

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when account is locked")
    void shouldThrowExceptionWhenAccountIsLocked() {
        // Given
        testUser.setLocked(true);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));

        // When/Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ACCOUNT_LOCKED")
                .hasMessageContaining("locked");

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when account is inactive")
    void shouldThrowExceptionWhenAccountIsInactive() {
        // Given
        testUser.setActive(false);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));

        // When/Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ACCOUNT_INACTIVE");

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should increment failed login attempts on wrong password")
    void shouldIncrementFailedLoginAttemptsOnWrongPassword() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "INVALID_CREDENTIALS");

        // Verify failed login was handled
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should lock account after max failed login attempts")
    void shouldLockAccountAfterMaxFailedAttempts() {
        // Given
        testUser.setFailedLoginAttempts(4); // One attempt before lock
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class);

        // Verify account was locked
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(savedUser.isLocked()).isTrue();
    }

    @Test
    @DisplayName("Should logout successfully")
    void shouldLogoutSuccessfully() {
        // Given
        String userId = "user-123";
        String token = "access-token";

        // When
        authService.logout(userId, token);

        // Then
        verify(redisTemplate).delete("session:user-123");
        verify(valueOperations).set(eq("blacklist:access-token"), eq("1"), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() {
        // Given
        String refreshToken = "valid-refresh-token";
        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(refreshToken)).thenReturn("user-123");
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(eq(testUser.getId()), anyMap())).thenReturn("new-access-token");

        // When
        LoginResponse response = authService.refreshToken(refreshToken);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken);

        verify(jwtUtil).validateToken(refreshToken);
        verify(jwtUtil).getUserIdFromToken(refreshToken);
        verify(userRepository).findById("user-123");
    }

    @Test
    @DisplayName("Should throw exception when refresh token is invalid")
    void shouldThrowExceptionWhenRefreshTokenIsInvalid() {
        // Given
        String invalidToken = "invalid-refresh-token";
        when(jwtUtil.validateToken(invalidToken)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "INVALID_TOKEN");

        verify(jwtUtil).validateToken(invalidToken);
        verify(jwtUtil, never()).getUserIdFromToken(anyString());
        verify(userRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("Should throw exception when user not found during token refresh")
    void shouldThrowExceptionWhenUserNotFoundDuringRefresh() {
        // Given
        String refreshToken = "valid-refresh-token";
        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(refreshToken)).thenReturn("user-123");
        when(userRepository.findById("user-123")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "USER_NOT_FOUND");

        verify(userRepository).findById("user-123");
    }

    @Test
    @DisplayName("Should reset failed login attempts on successful login")
    void shouldResetFailedLoginAttemptsOnSuccessfulLogin() {
        // Given
        testUser.setFailedLoginAttempts(3);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(eq(testUser.getId()), anyMap())).thenReturn("access-token");
        when(jwtUtil.generateToken(testUser.getId())).thenReturn("refresh-token");

        // When
        LoginResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(0);
    }
}
