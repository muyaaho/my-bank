package com.mybank.user.controller;

import com.mybank.user.dto.CreateUserRequest;
import com.mybank.user.dto.UserDto;
import com.mybank.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Internal User Controller for inter-service communication
 * Not exposed to external clients
 */
@Slf4j
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Internal API: Creating user {}", request.getEmail());
        UserDto user = userService.createUser(request);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable String userId) {
        log.debug("Internal API: Getting user {}", userId);
        UserDto user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        log.debug("Internal API: Getting user by email {}", email);
        UserDto user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<UserDto>> getUsersByIds(@RequestBody List<String> userIds) {
        log.debug("Internal API: Getting {} users", userIds.size());
        List<UserDto> users = userService.getUsersByIds(userIds);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{userId}/last-login")
    public ResponseEntity<Void> updateLastLogin(@PathVariable String userId) {
        log.debug("Internal API: Updating last login for user {}", userId);
        userService.updateLastLogin(userId);
        return ResponseEntity.ok().build();
    }
}
