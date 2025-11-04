package com.mybank.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Header-based Authentication Filter
 * Extracts user information from request headers set by API Gateway
 * Sets Spring Security context for authorization
 */
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_EMAIL = "X-User-Email";
    private static final String HEADER_USER_NAME = "X-User-Name";
    private static final String HEADER_USER_ROLES = "X-User-Roles";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String userId = request.getHeader(HEADER_USER_ID);
        String email = request.getHeader(HEADER_USER_EMAIL);
        String name = request.getHeader(HEADER_USER_NAME);
        String rolesStr = request.getHeader(HEADER_USER_ROLES);

        if (userId != null && email != null) {
            log.debug("Authenticating user from headers: userId={}, email={}", userId, email);

            // Parse roles
            Set<String> roles = parseRoles(rolesStr);

            // Create authorities with ROLE_ prefix
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());

            // Create UserPrincipal
            UserPrincipal principal = UserPrincipal.builder()
                    .userId(userId)
                    .email(email)
                    .name(name)
                    .roles(roles)
                    .build();

            // Create authentication token
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            // Set SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Security context set for user: {} with roles: {}", userId, roles);
        } else {
            log.debug("No authentication headers found, skipping authentication");
        }

        filterChain.doFilter(request, response);
    }

    private Set<String> parseRoles(String rolesStr) {
        if (rolesStr == null || rolesStr.trim().isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(rolesStr.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }
}
