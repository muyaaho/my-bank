package com.mybank.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base event class for all domain events
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent implements Serializable {

    private String eventId = UUID.randomUUID().toString();
    private String eventType;
    private LocalDateTime timestamp = LocalDateTime.now();
    private String correlationId;
    private String userId;
}
