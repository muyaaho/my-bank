package com.mybank.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Event for transaction/consumption activities
 * Published by Payment Service, consumed by PFM Core Service
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TransactionEvent extends BaseEvent {

    private String transactionId;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private String category;
    private String merchantName;
    private String transactionType; // DEBIT, CREDIT
    private String status; // COMPLETED, FAILED, PENDING
}
