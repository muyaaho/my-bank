package com.mybank.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Event for completed payment
 * Published by Payment Service, consumed by Investment Service for round-up logic
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentCompletedEvent extends BaseEvent {

    private String paymentId;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private String merchantName;
}
