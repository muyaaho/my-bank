package com.mybank.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybank.auth.dto.LoginRequest;
import com.mybank.auth.dto.LoginResponse;
import com.mybank.auth.dto.RegisterRequest;
import com.mybank.auth.service.AuthService;
import com.mybank.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for AuthController
 * Tests REST API endpoints with MockMvc
 */
@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private LoginResponse loginResponse;

    @BeforeEach
    void setUp() {
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

        loginResponse = LoginResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(86400)
                .user(LoginResponse.UserInfo.builder()
                        .id("user-123")
                        .email("test@mybank.com")
                        .name("Test User")
                        .build())
                .build();
    }

    @Test
    @DisplayName("POST /auth/register - Should register user successfully")
    void shouldRegisterUserSuccessfully() throws Exception {
        // Given
        when(authService.register(any(RegisterRequest.class))).thenReturn(loginResponse);

        // When/Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.id").value("user-123"))
                .andExpect(jsonPath("$.data.user.email").value("test@mybank.com"))
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    @DisplayName("POST /auth/register - Should return 400 for invalid email")
    void shouldReturn400ForInvalidEmail() throws Exception {
        // Given
        registerRequest.setEmail("invalid-email");

        // When/Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register - Should return 400 for weak password")
    void shouldReturn400ForWeakPassword() throws Exception {
        // Given
        registerRequest.setPassword("weak");

        // When/Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register - Should return 400 when user exists")
    void shouldReturn400WhenUserExists() throws Exception {
        // Given
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new BusinessException("USER_EXISTS", "User already exists"));

        // When/Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /auth/login - Should login successfully")
    void shouldLoginSuccessfully() throws Exception {
        // Given
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        // When/Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    @DisplayName("POST /auth/login - Should return 400 for invalid credentials")
    void shouldReturn400ForInvalidCredentials() throws Exception {
        // Given
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BusinessException("INVALID_CREDENTIALS", "Invalid credentials"));

        // When/Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /auth/login - Should return 400 when account is locked")
    void shouldReturn400WhenAccountIsLocked() throws Exception {
        // Given
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BusinessException("ACCOUNT_LOCKED", "Account is locked"));

        // When/Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Account is locked"));
    }

    @Test
    @DisplayName("POST /auth/logout - Should logout successfully")
    void shouldLogoutSuccessfully() throws Exception {
        // When/Then
        mockMvc.perform(post("/auth/logout")
                        .header("X-User-Id", "user-123")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    @Test
    @DisplayName("POST /auth/refresh - Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() throws Exception {
        // Given
        when(authService.refreshToken(anyString())).thenReturn(loginResponse);

        // When/Then
        mockMvc.perform(post("/auth/refresh")
                        .param("refreshToken", "refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"));
    }

    @Test
    @DisplayName("GET /auth/health - Should return healthy status")
    void shouldReturnHealthyStatus() throws Exception {
        // When/Then
        mockMvc.perform(get("/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Auth Service is healthy"));
    }
}
