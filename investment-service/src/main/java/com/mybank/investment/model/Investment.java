package com.mybank.investment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Investment Entity (embedded within InvestmentAccount)
 * NOT a separate Document - managed by InvestmentAccount Aggregate
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Investment {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String userId;

    private String accountId;

    private String productId;

    private String productName;

    private InvestmentType investmentType; // ROUNDUP, MANUAL, AUTO

    private BigDecimal amount;

    private String currency;

    private String relatedPaymentId; // For round-up investments

    @Builder.Default
    private LocalDateTime investedAt = LocalDateTime.now();

    public enum InvestmentType {
        ROUNDUP, MANUAL, AUTO
    }
}
