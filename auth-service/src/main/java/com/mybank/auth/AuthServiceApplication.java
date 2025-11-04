package com.mybank.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.mybank.common.security.MicroserviceSecurityConfig;

/**
 * Authentication Service
 * Handles user authentication, authorization, and token management
 * Supports OAuth 2.0, JWT, and FIDO2 biometric authentication
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.mybank.common.client")
@SpringBootApplication(exclude = {MongoAutoConfiguration.class})
@ComponentScan(
    basePackages = {"com.mybank.auth", "com.mybank.common"},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {MicroserviceSecurityConfig.class}
    )
)
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
