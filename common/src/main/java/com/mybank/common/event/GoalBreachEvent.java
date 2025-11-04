package com.mybank.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Event for goal breach notifications
 * Published by AI Coach Service, consumed by Notification Service
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GoalBreachEvent extends BaseEvent {

    private String goalId;
    private String goalType; // SPENDING, SAVING
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private String breachType; // EXCEEDED, FALLING_BEHIND
    private String message;
}
