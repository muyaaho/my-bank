package com.mybank.auth;

import com.mybank.auth.dto.LoginRequest;
import com.mybank.auth.dto.LoginResponse;
import com.mybank.auth.dto.RegisterRequest;
import com.mybank.auth.model.AuthCredential;
import com.mybank.auth.repository.AuthCredentialRepository;
import com.mybank.auth.service.RefactoredAuthService;
import com.mybank.common.client.UserServiceClient;
import com.mybank.common.dto.CreateUserRequestDto;
import com.mybank.common.dto.UserResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test for the new authentication flow
 * Tests auth/user separation with Feign client communication
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthenticationFlowIntegrationTest {

    @Autowired
    private RefactoredAuthService authService;

    @Autowired
    private AuthCredentialRepository authCredentialRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() {
        authCredentialRepository.deleteAll();
    }

    @Test
    void testUserRegistrationFlow() {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setName("Test User");
        registerRequest.setPhoneNumber("010-1234-5678");

        UserResponseDto mockUserResponse = UserResponseDto.builder()
                .id("user-123")
                .email("test@example.com")
                .name("Test User")
                .phoneNumber("010-1234-5678")
                .roles(Set.of("USER"))
                .isActive(true)
                .build();

        when(userServiceClient.createUser(any(CreateUserRequestDto.class)))
                .thenReturn(mockUserResponse);

        // When
        LoginResponse response = authService.register(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotEmpty();
        assertThat(response.getRefreshToken()).isNotEmpty();
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");

        // Verify auth credential was created
        AuthCredential credential = authCredentialRepository.findByEmail("test@example.com")
                .orElseThrow();
        assertThat(credential.getUserId()).isNotNull();
        assertThat(credential.getPassword()).isNotEmpty();
        assertThat(credential.isLocked()).isFalse();

        // Verify user-service was called
        ArgumentCaptor<CreateUserRequestDto> captor = ArgumentCaptor.forClass(CreateUserRequestDto.class);
        verify(userServiceClient, times(1)).createUser(captor.capture());

        CreateUserRequestDto capturedRequest = captor.getValue();
        assertThat(capturedRequest.getEmail()).isEqualTo("test@example.com");
        assertThat(capturedRequest.getName()).isEqualTo("Test User");
    }

    @Test
    void testUserLoginFlow() {
        // Given - Setup existing user
        String userId = "user-456";
        AuthCredential credential = AuthCredential.builder()
                .userId(userId)
                .email("login@example.com")
                .password("$2a$10$X5wFutsrwyjoQ98XvjXIqON9hdF7H/tZEpKs7omKDIl6Rz.LRk5Sq") // "password123"
                .isLocked(false)
                .failedLoginAttempts(0)
                .build();
        authCredentialRepository.save(credential);

        UserResponseDto mockUserResponse = UserResponseDto.builder()
                .id(userId)
                .email("login@example.com")
                .name("Login User")
                .roles(Set.of("USER"))
                .isActive(true)
                .build();

        when(userServiceClient.getUserById(userId)).thenReturn(mockUserResponse);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("login@example.com");
        loginRequest.setPassword("password123");

        // When
        LoginResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotEmpty();
        assertThat(response.getUser().getEmail()).isEqualTo("login@example.com");

        // Verify user-service was called to get user profile
        verify(userServiceClient, times(1)).getUserById(userId);
        verify(userServiceClient, times(1)).updateLastLogin(userId);

        // Verify failed login attempts were reset
        AuthCredential updatedCredential = authCredentialRepository.findByEmail("login@example.com")
                .orElseThrow();
        assertThat(updatedCredential.getFailedLoginAttempts()).isEqualTo(0);
    }
}
