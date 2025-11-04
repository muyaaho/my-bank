package com.mybank.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * User DTO for API responses and inter-service communication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private String id;
    private String email;
    private String name;
    private String phoneNumber;
    private String profileImageUrl;
    private String bio;
    private Set<String> roles;
    private Set<String> permissions;
    private boolean isActive;
    private boolean isEmailVerified;
    private boolean isPhoneVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
}
