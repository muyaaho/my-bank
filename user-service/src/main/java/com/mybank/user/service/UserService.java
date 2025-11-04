package com.mybank.user.service;

import com.mybank.common.exception.BusinessException;
import com.mybank.user.dto.CreateUserRequest;
import com.mybank.user.dto.UpdateUserRequest;
import com.mybank.user.dto.UserDto;
import com.mybank.user.model.User;
import com.mybank.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User service for managing user profiles and information
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        log.info("Creating new user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("USER_EXISTS", "User with this email already exists");
        }

        User user = User.builder()
                .id(request.getId())
                .email(request.getEmail())
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .roles(request.getRoles() != null ? request.getRoles() : Set.of("USER"))
                .isActive(true)
                .isEmailVerified(false)
                .isPhoneVerified(false)
                .build();

        user = userRepository.save(user);
        log.info("User created successfully: {}", user.getId());

        return toDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsersByIds(List<String> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto updateUser(String userId, UpdateUserRequest request) {
        log.info("Updating user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        user = userRepository.save(user);
        log.info("User updated successfully: {}", userId);

        return toDto(user);
    }

    @Transactional
    public void updateLastLogin(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    @Transactional
    public void assignRoles(String userId, Set<String> roles) {
        log.info("Assigning roles to user {}: {}", userId, roles);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));

        user.setRoles(roles);
        userRepository.save(user);
    }

    @Transactional
    public void deactivateUser(String userId) {
        log.info("Deactivating user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));

        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> searchUsers(String query) {
        List<User> users = userRepository.findByEmailContaining(query);
        users.addAll(userRepository.findByNameContaining(query));
        return users.stream()
                .distinct()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .profileImageUrl(user.getProfileImageUrl())
                .bio(user.getBio())
                .roles(user.getRoles())
                .permissions(user.getPermissions())
                .isActive(user.isActive())
                .isEmailVerified(user.isEmailVerified())
                .isPhoneVerified(user.isPhoneVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
