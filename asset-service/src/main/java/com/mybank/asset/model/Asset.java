package com.mybank.asset.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Asset Aggregate Root
 * Bounded Context: Asset Management
 * Domain: Personal Financial Management
 */
@Document(collection = "assets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {

    @Id
    private String id;

    private String userId;

    private AssetType assetType; // BANK, CARD, SECURITIES, INSURANCE, FINTECH

    private String institutionName;

    private String accountNumber;

    private String accountName;

    private BigDecimal balance;

    private String currency;

    private BigDecimal currentValue; // For securities and investments

    private BigDecimal profitLoss; // For investments

    private String productName; // For insurance, investment products

    private LocalDateTime lastSyncedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private boolean isActive;

    public enum AssetType {
        BANK, CARD, SECURITIES, INSURANCE, FINTECH
    }
}
