package com.mybank.auth.repository;

import com.mybank.auth.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for UserRepository
 * Tests database operations without mocking
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@mybank.com")
                .password("encodedPassword")
                .name("Test User")
                .phoneNumber("010-1234-5678")
                .roles(Set.of("USER"))
                .isActive(true)
                .isLocked(false)
                .failedLoginAttempts(0)
                .build();
    }

    @Test
    @DisplayName("Should save user successfully")
    void shouldSaveUser() {
        // When
        User savedUser = userRepository.save(testUser);

        // Then
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("test@mybank.com");
        assertThat(savedUser.getName()).isEqualTo("Test User");
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Given
        entityManager.persist(testUser);
        entityManager.flush();

        // When
        Optional<User> foundUser = userRepository.findByEmail("test@mybank.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@mybank.com");
        assertThat(foundUser.get().getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Should return empty when user not found by email")
    void shouldReturnEmptyWhenUserNotFoundByEmail() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@mybank.com");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Should check if user exists by email")
    void shouldCheckIfUserExistsByEmail() {
        // Given
        entityManager.persist(testUser);
        entityManager.flush();

        // When
        boolean exists = userRepository.existsByEmail("test@mybank.com");
        boolean notExists = userRepository.existsByEmail("nonexistent@mybank.com");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should update user successfully")
    void shouldUpdateUser() {
        // Given
        User savedUser = entityManager.persist(testUser);
        entityManager.flush();

        // When
        savedUser.setName("Updated Name");
        savedUser.setPhoneNumber("010-9999-9999");
        User updatedUser = userRepository.save(savedUser);
        entityManager.flush();

        // Then
        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
        assertThat(updatedUser.getPhoneNumber()).isEqualTo("010-9999-9999");
        assertThat(updatedUser.getUpdatedAt()).isAfter(updatedUser.getCreatedAt());
    }

    @Test
    @DisplayName("Should delete user successfully")
    void shouldDeleteUser() {
        // Given
        User savedUser = entityManager.persist(testUser);
        entityManager.flush();
        String userId = savedUser.getId();

        // When
        userRepository.deleteById(userId);
        entityManager.flush();

        // Then
        Optional<User> foundUser = userRepository.findById(userId);
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Should handle unique email constraint")
    void shouldHandleUniqueEmailConstraint() {
        // Given
        entityManager.persist(testUser);
        entityManager.flush();

        User duplicateUser = User.builder()
                .email("test@mybank.com") // Same email
                .password("anotherPassword")
                .name("Another User")
                .roles(Set.of("USER"))
                .build();

        // When/Then
        // This should throw DataIntegrityViolationException
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> {
                    userRepository.save(duplicateUser);
                    entityManager.flush();
                }
        );
    }

    @Test
    @DisplayName("Should increment failed login attempts")
    void shouldIncrementFailedLoginAttempts() {
        // Given
        User savedUser = entityManager.persist(testUser);
        entityManager.flush();

        // When
        savedUser.setFailedLoginAttempts(savedUser.getFailedLoginAttempts() + 1);
        userRepository.save(savedUser);
        entityManager.flush();

        // Then
        User updatedUser = userRepository.findById(savedUser.getId()).get();
        assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should lock user account")
    void shouldLockUserAccount() {
        // Given
        User savedUser = entityManager.persist(testUser);
        entityManager.flush();

        // When
        savedUser.setLocked(true);
        savedUser.setFailedLoginAttempts(5);
        userRepository.save(savedUser);
        entityManager.flush();

        // Then
        User lockedUser = userRepository.findById(savedUser.getId()).get();
        assertThat(lockedUser.isLocked()).isTrue();
        assertThat(lockedUser.getFailedLoginAttempts()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should deactivate user account")
    void shouldDeactivateUserAccount() {
        // Given
        User savedUser = entityManager.persist(testUser);
        entityManager.flush();

        // When
        savedUser.setActive(false);
        userRepository.save(savedUser);
        entityManager.flush();

        // Then
        User inactiveUser = userRepository.findById(savedUser.getId()).get();
        assertThat(inactiveUser.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should store FIDO2 credential ID")
    void shouldStoreFido2CredentialId() {
        // Given
        User savedUser = entityManager.persist(testUser);
        entityManager.flush();

        // When
        savedUser.setFido2CredentialId("fido2-credential-123");
        userRepository.save(savedUser);
        entityManager.flush();

        // Then
        User userWithFido = userRepository.findById(savedUser.getId()).get();
        assertThat(userWithFido.getFido2CredentialId()).isEqualTo("fido2-credential-123");
    }

    @Test
    @DisplayName("Should update last login timestamp")
    void shouldUpdateLastLoginTimestamp() {
        // Given
        User savedUser = entityManager.persist(testUser);
        entityManager.flush();

        // When
        savedUser.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(savedUser);
        entityManager.flush();

        // Then
        User userWithLogin = userRepository.findById(savedUser.getId()).get();
        assertThat(userWithLogin.getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("Should save user with multiple roles")
    void shouldSaveUserWithMultipleRoles() {
        // Given
        User adminUser = User.builder()
                .email("admin@mybank.com")
                .password("encodedPassword")
                .name("Admin User")
                .roles(Set.of("USER", "ADMIN", "MANAGER"))
                .isActive(true)
                .build();

        // When
        User savedUser = userRepository.save(adminUser);
        entityManager.flush();

        // Then
        User foundUser = userRepository.findById(savedUser.getId()).get();
        assertThat(foundUser.getRoles()).hasSize(3);
        assertThat(foundUser.getRoles()).containsExactlyInAnyOrder("USER", "ADMIN", "MANAGER");
    }
}
