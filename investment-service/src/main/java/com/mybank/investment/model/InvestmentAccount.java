package com.mybank.investment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * InvestmentAccount Aggregate Root
 * Bounded Context: Investment Management
 *
 * Manages:
 * - Investment transactions within this account
 * - Round-up configuration for automatic investing
 * - Account balance and lifecycle
 */
@Document(collection = "investment_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentAccount {

    @Id
    private String id;

    private String userId;

    private String accountNumber;

    private String accountName;

    private BigDecimal balance;

    private String currency;

    private AccountType accountType; // STOCKS, ETF, ROUNDUP

    private boolean isActive;

    // Embedded entities and value objects
    @Builder.Default
    private List<Investment> investments = new ArrayList<>();

    private RoundUpConfig roundUpConfig;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum AccountType {
        STOCKS, ETF, ROUNDUP, FUND
    }

    // Aggregate behavior: Add investment to this account
    public void addInvestment(Investment investment) {
        if (this.investments == null) {
            this.investments = new ArrayList<>();
        }

        // Set the account reference
        investment.setAccountId(this.id);
        investment.setUserId(this.userId);

        // Add to collection
        this.investments.add(investment);

        // Update balance
        this.balance = this.balance.add(investment.getAmount());
        this.updatedAt = LocalDateTime.now();
    }

    // Aggregate behavior: Process round-up investment
    public Investment processRoundUp(String paymentId, BigDecimal paymentAmount, String currency) {
        if (roundUpConfig == null || !roundUpConfig.isEnabled()) {
            throw new IllegalStateException("Round-up not enabled for this account");
        }

        BigDecimal roundUpAmount = roundUpConfig.calculateRoundUp(paymentAmount);

        if (roundUpAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null; // No round-up needed
        }

        // Create round-up investment
        Investment investment = Investment.builder()
                .userId(this.userId)
                .accountId(this.id)
                .productId("ROUNDUP-PRODUCT-001")
                .productName("Round-up Investment")
                .investmentType(Investment.InvestmentType.ROUNDUP)
                .amount(roundUpAmount)
                .currency(currency)
                .relatedPaymentId(paymentId)
                .investedAt(LocalDateTime.now())
                .build();

        // Add investment to account
        addInvestment(investment);

        // Update round-up statistics
        roundUpConfig.recordTransaction(roundUpAmount);

        return investment;
    }

    // Aggregate behavior: Enable round-up
    public void enableRoundUp(String sourceAccountId, BigDecimal roundUpUnit) {
        if (this.roundUpConfig == null) {
            this.roundUpConfig = new RoundUpConfig();
        }
        this.roundUpConfig.enable(sourceAccountId, this.id, roundUpUnit);
        this.updatedAt = LocalDateTime.now();
    }

    // Aggregate behavior: Disable round-up
    public void disableRoundUp() {
        if (this.roundUpConfig != null) {
            this.roundUpConfig.disable();
        }
        this.updatedAt = LocalDateTime.now();
    }
}
