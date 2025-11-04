package com.mybank.gateway.filter;

import com.mybank.common.session.EnhancedSessionService;
import com.mybank.common.session.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Redis Session WebFilter for API Gateway
 * Validates session existence and freshness in Redis
 * Implements sliding window session expiration
 * Runs after JWT validation
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSessionWebFilter implements WebFilter {

    private final EnhancedSessionService sessionService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        // Skip session check for public endpoints
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        // Get user ID from request header (set by JWT filter)
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String token = exchange.getRequest().getHeaders().getFirst("X-Token");

        if (userId == null || token == null) {
            log.warn("Missing user context headers");
            return onError(exchange, "Missing user context", HttpStatus.UNAUTHORIZED);
        }

        // Validate token in Redis (check if blacklisted)
        return sessionService.validateTokenReactive(token)
                .flatMap(validatedUserId -> {
                    // Token is valid and not blacklisted
                    if (!validatedUserId.equals(userId)) {
                        log.warn("User ID mismatch: JWT={}, Redis={}", userId, validatedUserId);
                        return onError(exchange, "Invalid session", HttpStatus.UNAUTHORIZED);
                    }

                    // Get session from Redis
                    return sessionService.getSessionReactive(userId)
                            .flatMap(session -> {
                                log.debug("Session found for user: {}", userId);

                                // Refresh session TTL (sliding window)
                                return sessionService.refreshSessionReactive(userId)
                                        .then(Mono.defer(() -> {
                                            // Add session info to exchange attributes
                                            exchange.getAttributes().put("userSession", session);
                                            return chain.filter(exchange);
                                        }));
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                // Session not found or expired
                                log.warn("Session not found or expired for user: {}", userId);
                                return onError(exchange, "Session expired", HttpStatus.UNAUTHORIZED);
                            }))
                            .onErrorResume(error -> {
                                // Handle Redis or deserialization errors
                                log.error("Error retrieving session for user {}: {}", userId, error.getMessage());
                                return onError(exchange, "Session error", HttpStatus.INTERNAL_SERVER_ERROR);
                            });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Token is blacklisted or not found
                    log.warn("Token is blacklisted or not found for user: {}", userId);
                    return onError(exchange, "Invalid or revoked token", HttpStatus.UNAUTHORIZED);
                }))
                .onErrorResume(error -> {
                    // Handle token validation errors
                    log.error("Error validating token: {}", error.getMessage());
                    return onError(exchange, "Token validation error", HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }

    private boolean isPublicEndpoint(String path) {
        return path.contains("/auth/") ||
               path.contains("/actuator/") ||
               path.contains("/health");
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        log.warn("Session validation error: {} - Status: {}", message, status);
        return response.setComplete();
    }
}
