package com.mybank.common.client;

import com.mybank.common.dto.CreateUserRequestDto;
import com.mybank.common.dto.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feign Client for User Service
 * Used by other services to communicate with user-service
 */
@FeignClient(name = "user-service", path = "/internal/users")
public interface UserServiceClient {

    @PostMapping
    UserResponseDto createUser(@RequestBody CreateUserRequestDto request);

    @GetMapping("/{userId}")
    UserResponseDto getUserById(@PathVariable("userId") String userId);

    @GetMapping("/email/{email}")
    UserResponseDto getUserByEmail(@PathVariable("email") String email);

    @PostMapping("/batch")
    List<UserResponseDto> getUsersByIds(@RequestBody List<String> userIds);

    @PutMapping("/{userId}/last-login")
    void updateLastLogin(@PathVariable("userId") String userId);
}
