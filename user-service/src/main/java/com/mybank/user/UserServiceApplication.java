package com.mybank.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * User Service Application
 * Manages user profiles, roles, and permissions
 */
@SpringBootApplication(
    scanBasePackages = {"com.mybank.user", "com.mybank.common"},
    exclude = {MongoAutoConfiguration.class}
)
@EnableFeignClients
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
