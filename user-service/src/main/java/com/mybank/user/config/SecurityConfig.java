package com.mybank.user.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Security configuration for User Service
 * Uses common microservice security configuration via component scan
 */
@Configuration
@ComponentScan(basePackages = "com.mybank.common.security")
public class SecurityConfig {
    // MicroserviceSecurityConfig will be picked up via component scan
}
