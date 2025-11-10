package com.mybank.investment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RoundUpConfig Value Object (embedded within InvestmentAccount)
 * Represents automatic round-up investing configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundUpConfig {

    private String sourceAccountId; // Payment account

    private String targetInvestmentAccountId; // Investment account for round-up

    @Builder.Default
    private BigDecimal roundUpUnit = new BigDecimal("1000"); // Default: 1000 KRW

    @Builder.Default
    private boolean enabled = false;

    @Builder.Default
    private BigDecimal totalRoundedUp = BigDecimal.ZERO;

    @Builder.Default
    private int totalTransactions = 0;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Value Object behavior: Calculate round-up amount
    public BigDecimal calculateRoundUp(BigDecimal paymentAmount) {
        BigDecimal remainder = paymentAmount.remainder(roundUpUnit);

        if (remainder.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO; // Already rounded
        }

        return roundUpUnit.subtract(remainder);
    }

    // Value Object behavior: Record a round-up transaction
    public void recordTransaction(BigDecimal roundUpAmount) {
        this.totalRoundedUp = this.totalRoundedUp.add(roundUpAmount);
        this.totalTransactions++;
        this.updatedAt = LocalDateTime.now();
    }

    // Value Object behavior: Enable round-up
    public void enable(String sourceAccountId, String targetAccountId, BigDecimal roundUpUnit) {
        this.sourceAccountId = sourceAccountId;
        this.targetInvestmentAccountId = targetAccountId;
        this.roundUpUnit = roundUpUnit;
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Value Object behavior: Disable round-up
    public void disable() {
        this.enabled = false;
        this.updatedAt = LocalDateTime.now();
    }
}
