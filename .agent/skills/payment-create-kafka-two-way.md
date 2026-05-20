
---
name: payment-platform-kafka-two-way
description: Two‑way asynchronous communication between payment-service and pg-service using Kafka
version: 2.0
---

# Skill: Implement Two‑Way Kafka Communication

## Goal
Let `payment-service` send commands to `pg-service` via Kafka, and `pg-service` reply with events, all using Outbox pattern.

## Step 1: Define Avro Schemas (or POJOs)
```java
// PaymentCommand
public class PaymentConfirmCommand {
    private String paymentId;
    private BigDecimal amount;
    private String currency;
    // getters/setters
}

// PaymentConfirmedEvent
public class PaymentConfirmedEvent {
    private String paymentId;
    private String pgTransactionId;
    private boolean success;
    private String failureReason;
}
```
Step 2: Payment-Service – Send Command via Outbox
```java
@Transactional
public void initiatePayment(String paymentId) {
    PaymentConfirmCommand command = new PaymentConfirmCommand(paymentId, ...);
    String payload = objectMapper.writeValueAsString(command);
    OutboxMessage outbox = OutboxMessage.builder()
        .messageId(UUID.randomUUID().toString())
        .aggregateId(paymentId)
        .eventType("PaymentConfirmCommand")
        .payload(payload)
        .topic("payment.commands.confirm")
        .build();
    outboxRepo.save(outbox);
}
```

Step 3: PG-Service – Consume Command and Publish Result
```java
@Component
public class PgCommandConsumer {
    @KafkaListener(topics = "payment.commands.confirm", groupId = "pg-group")
    public void onCommand(String message, Acknowledgment ack) {
        PaymentConfirmCommand cmd = objectMapper.readValue(message, PaymentConfirmCommand.class);
        // Call external PG (mock or real) – use virtual threads for blocking
        boolean success = mockPgCall(cmd);
        PaymentConfirmedEvent event = new PaymentConfirmedEvent(cmd.getPaymentId(), success);
        // Save outbox event in pg-service
        outboxService.save(event, "payment.events.confirmed");
        ack.acknowledge();
    }
}
```

Step 4: Payment-Service – Consume Result and Complete Saga
```java
// PaymentResultConsumer
@Component
public class PaymentEventConsumer {
    @KafkaListener(topics = "payment.events.confirmed", groupId = "payment-group")
    public void onConfirmed(String message, Acknowledgment ack) {
        PaymentConfirmedEvent event = objectMapper.readValue(message, PaymentConfirmedEvent.class);
        paymentOrchestrator.handlePgResult(event);
        ack.acknowledge();
    }
}
```
Testing
Use @EmbeddedKafka to test both services in one test.

Verify that command is sent, processed, and event received.

Pitfalls
Directly calling kafkaTemplate.send() inside business transaction (bypassing outbox) → risk of lost events.

Not acknowledging after processing → messages are replayed.
