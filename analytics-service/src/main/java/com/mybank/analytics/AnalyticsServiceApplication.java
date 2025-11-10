package com.mybank.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Analytics Service Application
 * Bounded Context: Spending Analytics
 * Aggregate Root: SpendingAnalysis
 * Pattern: CQRS Read Model
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableMongoRepositories
@ComponentScan(basePackages = {"com.mybank.analytics", "com.mybank.common"})
public class AnalyticsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
