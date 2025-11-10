package com.mybank.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Spending analysis response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpendingAnalysisResponse {

    private BigDecimal totalSpending;
    private String period;
    private List<CategorySpending> categoryBreakdown;
    private List<AnomalousTransaction> anomalousTransactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySpending {
        private String category;
        private BigDecimal amount;
        private int transactionCount;
        private BigDecimal averageAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnomalousTransaction {
        private String transactionId;
        private String category;
        private BigDecimal amount;
        private String merchantName;
        private String reason;
    }
}
