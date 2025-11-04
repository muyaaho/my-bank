# Schema Module

This module manages Kafka Avro schemas for event-driven communication between microservices.

## Overview

The schema module provides:
- **Avro Schema Definitions** (.avsc files) for all domain events
- **Auto-generated Java classes** from Avro schemas
- **Schema versioning** and compatibility management
- **Type-safe event serialization/deserialization**

## Avro Schemas

### Payment Events
- **payment-completed-event.avsc**: Published by Payment Service, consumed by Investment Service for round-up logic

### Transaction Events
- **transaction-event.avsc**: Published by Payment Service, consumed by PFM Core Service for spending analysis

### Goal Events
- **goal-breach-event.avsc**: Published by AI Coach Service, consumed by Notification Service

## Building

```bash
# Generate Java classes from Avro schemas
./gradlew :schema:build

# Clean and regenerate
./gradlew :schema:clean :schema:build
```

Generated classes will be in: `schema/build/generated-main-avro-java/`

## Usage in Services

### Add Dependency

In service `build.gradle`:
```gradle
dependencies {
    implementation project(':schema')
}
```

### Producer Example

```java
import com.mybank.schema.event.PaymentCompletedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;

PaymentCompletedEvent event = PaymentCompletedEvent.newBuilder()
    .setEventId(UUID.randomUUID().toString())
    .setEventType("PAYMENT_COMPLETED")
    .setTimestamp(LocalDateTime.now().toString())
    .setPaymentId(paymentId)
    .setAccountId(accountId)
    .setAmount(amountBytes)
    .setCurrency("KRW")
    .build();

kafkaTemplate.send("payment-completed", event);
```

### Consumer Example

```java
@KafkaListener(topics = "payment-completed", groupId = "investment-service")
public void handlePaymentCompleted(PaymentCompletedEvent event) {
    log.info("Received payment completed event: {}", event);
    // Process event
}
```

## Avro Data Types

### Decimal (BigDecimal)
- Type: `bytes` with `logicalType: decimal`
- Precision: 19, Scale: 4
- Java mapping: `ByteBuffer` (manual conversion needed)

### DateTime (LocalDateTime)
- Type: `string`
- Format: ISO-8601 (e.g., "2025-11-03T12:34:56")
- Java mapping: `String` (manual conversion needed)

### Enums
- TransactionType: DEBIT, CREDIT
- TransactionStatus: COMPLETED, FAILED, PENDING
- GoalType: SPENDING, SAVING
- BreachType: EXCEEDED, FALLING_BEHIND

## Schema Evolution

Avro supports schema evolution with backward/forward compatibility:

1. **Adding fields**: Use default values (backward compatible)
2. **Removing fields**: Old consumers ignore new fields (forward compatible)
3. **Changing types**: Use type promotion (int → long, float → double)

## Schema Registry

For production, consider using Confluent Schema Registry:
- Centralized schema management
- Schema versioning
- Compatibility checks
- Schema ID-based serialization

## Best Practices

1. **Always set default values** for optional fields
2. **Use enums** for fixed value sets
3. **Document fields** with `doc` attribute
4. **Version schemas** in filenames if breaking changes needed
5. **Test schema compatibility** before deploying
