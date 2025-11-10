package com.mybank.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * SpendingAnalysis Aggregate Root
 * Bounded Context: Spending Analytics
 * Pattern: CQRS Read Model
 * Domain: Personal Financial Management
 */
@Document(collection = "spending_analysis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpendingAnalysis {

    @Id
    private String id;

    private String userId;

    private String transactionId;

    private String category; // FOOD, TRANSPORT, SHOPPING, ENTERTAINMENT, etc.

    private BigDecimal amount;

    private String merchantName;

    private LocalDateTime transactionDate;

    private Map<String, Object> metadata; // Additional analysis data

    private boolean isAnomalous; // Detected as unusual spending

    private String anomalyReason;

    private LocalDateTime analyzedAt;
}
