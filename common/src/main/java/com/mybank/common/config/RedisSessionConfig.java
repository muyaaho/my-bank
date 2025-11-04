package com.mybank.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;

/**
 * Redis Session Configuration for Global Session Management
 *
 * Features:
 * - Distributed session storage across all microservices
 * - Session data stored in Redis with TTL
 * - Session ID transmitted via HTTP header (X-Auth-Token)
 * - Automatic session expiration (30 minutes)
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800) // 30 minutes
public class RedisSessionConfig {

    /**
     * Use header-based session ID resolution instead of cookies
     * Header name: X-Auth-Token
     *
     * This is more suitable for microservices architecture
     * and API-first applications
     */
    @Bean
    public HttpSessionIdResolver httpSessionIdResolver() {
        return HeaderHttpSessionIdResolver.xAuthToken();
    }
}
