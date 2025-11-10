package com.mybank.asset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Asset Service Application
 * Bounded Context: Asset Management
 * Aggregate Root: Asset
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableMongoRepositories
@ComponentScan(basePackages = {"com.mybank.asset", "com.mybank.common"})
public class AssetServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssetServiceApplication.class, args);
    }
}
