package com.mybank.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * API Gateway for MyBank 360
 * Handles routing, authentication, rate limiting, and logging
 */
@EnableDiscoveryClient
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration.class
})
@ComponentScan(
    basePackages = {"com.mybank.gateway", "com.mybank.common"},
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.mybank\\.common\\.security\\.MicroserviceSecurityConfig"
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.mybank\\.common\\.config\\.RedisSessionConfig"
        )
    }
)
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
