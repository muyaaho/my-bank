package com.mybank.common.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.mybank.common.security.UserPrincipal;

/**
 * Feign Client Configuration
 * Adds authentication headers for inter-service communication
 */
@Slf4j
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor serviceToServiceAuthInterceptor() {
        return requestTemplate -> {
            // Propagate user context if exists
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal user = (UserPrincipal) auth.getPrincipal();
                requestTemplate.header("X-User-Id", user.getUserId());
                requestTemplate.header("X-User-Email", user.getEmail());
                requestTemplate.header("X-User-Roles", String.join(",", user.getRoles()));
                log.debug("Propagating user context to Feign request: userId={}", user.getUserId());
            }
        };
    }
}
