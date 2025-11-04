package com.mybank.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for user responses via Feign client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

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
