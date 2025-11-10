package com.mybank.user.controller;

import com.mybank.user.dto.UpdateUserRequest;
import com.mybank.user.dto.UserDto;
import com.mybank.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * User Controller for user management endpoints
 */
@Slf4j
@RestController
@RequestMapping({"/api/user", "/api/v1/user"})
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping({"/me", "/profile"})
    public ResponseEntity<UserDto> getMyProfile(@RequestHeader("X-User-Id") String userId) {
        log.info("Get current user request for userId: {}", userId);
        UserDto user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateMyProfile(@RequestBody UpdateUserRequest request) {
        String userId = getCurrentUserId();
        UserDto user = userService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> getUserById(@PathVariable String userId) {
        UserDto user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> searchUsers(@RequestParam String query) {
        List<UserDto> users = userService.searchUsers(query);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignRoles(
            @PathVariable String userId,
            @RequestBody Set<String> roles) {
        userService.assignRoles(userId, roles);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(@PathVariable String userId) {
        userService.deactivateUser(userId);
        return ResponseEntity.ok().build();
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.mybank.common.security.UserPrincipal) {
            return ((com.mybank.common.security.UserPrincipal) authentication.getPrincipal()).getUserId();
        }
        throw new RuntimeException("User not authenticated");
    }

    // Added X-User-Id parameter method for header-based authentication
    private String getUserIdFromHeader(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new RuntimeException("User ID not provided in header");
        }
        return userId;
    }
}
