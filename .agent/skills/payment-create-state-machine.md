
---
name: payment-platform-state-machine
description: State machine for payment transactions (as in payment-platform Phase 5)
version: 2.0
---

# Skill: Implement Payment State Machine

## Goal
Model payment lifecycle with explicit states and transition validation.

## Step 1: Define Enum
```java
public enum PaymentState {
    PENDING,
    PG_PENDING,
    PG_SUCCESS,
    COMPLETED,
    FAILED,
    TIMEOUT
}
```
Step 2: Aggregate with State Transitions
```java
@Entity
public class Payment {
    @Id private String id;
    private String userId;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentState state;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Builder pattern or factory methods for construction

    public void startPgCall() {
        if (state != PaymentState.PENDING) {
            throw new IllegalStateException("Cannot start PG call from state " + state);
        }
        state = PaymentState.PG_PENDING;
        updatedAt = LocalDateTime.now();
    }

    public void handlePgSuccess() {
        if (state != PaymentState.PG_PENDING) {
            throw new IllegalStateException("Invalid state for PG success: " + state);
        }
        state = PaymentState.PG_SUCCESS;
        updatedAt = LocalDateTime.now();
    }

    public void complete() {
        if (state != PaymentState.PG_SUCCESS) {
            throw new IllegalStateException("Invalid state for completion: " + state);
        }
        state = PaymentState.COMPLETED;
        updatedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        state = PaymentState.FAILED;
        updatedAt = LocalDateTime.now();
    }

    public void timeout() {
        state = PaymentState.TIMEOUT;
        updatedAt = LocalDateTime.now();
    }
}
```
Step 3: Use in Use Case (with Outbox)
```java
@Getter
public class Payment {
    private String id;
    private PaymentState state;
    private LocalDateTime updatedAt;

    public void startPgCall() {
        if (state != PaymentState.PENDING) {
            throw new IllegalStateException("Cannot start PG call from state " + state);
        }
        this.state = PaymentState.PG_PENDING;
        this.updatedAt = LocalDateTime.now();
    }

    public void handlePgSuccess() {
        if (state != PaymentState.PG_PENDING) {
            throw new IllegalStateException("Cannot succeed PG call from state " + state);
        }
        this.state = PaymentState.PG_SUCCESS;
        this.updatedAt = LocalDateTime.now();
        // Optionally auto-transition to COMPLETED if no further steps
    }

    public void complete() {
        if (state != PaymentState.PG_SUCCESS) {
            throw new IllegalStateException("Cannot complete from state " + state);
        }
        this.state = PaymentState.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void fail() {
        if (state == PaymentState.COMPLETED) {
            throw new IllegalStateException("Already completed");
        }
        this.state = PaymentState.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public void timeout() {
        if (state == PaymentState.PG_PENDING) {
            this.state = PaymentState.TIMEOUT;
        }
    }
}
```
Step 3: Persist State with JPA
```java
@Entity
public class PaymentEntity {
    @Id
    private String id;
    @Enumerated(EnumType.STRING)
    private PaymentState state;
    private LocalDateTime updatedAt;
    // other fields...
}
```
Step 4: Use in Saga Orchestrator
```java
@Service
@Transactional
public class PaymentOrchestrator {
    public void handlePgConfirmed(String paymentId) {
        Payment payment = paymentRepo.findById(paymentId);
        payment.handlePgSuccess();
        paymentRepo.save(payment);
        // Then move to completion or next step
    }
}
```
Testing
Unit test each transition: valid and invalid moves.

Use @SpringBootTest with in-memory DB to verify state persisted.

Pitfalls
Forgetting to persist state after transition.

Allowing illegal transitions – always validate current state.

Concurrency: use @Version on entity to prevent race conditions.

