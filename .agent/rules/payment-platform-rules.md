---
trigger: always_on
---

---
name: payment-platform-rules
description: Rules for payment-platform (hyoguoo style) – Outbox, Idempotency, State Machine, Kafka two‑way
version: 1.0
---

# Payment Platform – Core Rules

## 1. Architecture
- The platform consists of **microservices**: `payment-service`, `pg-service`, `product-service`, `user-service`.
- **Eureka** for service discovery.
- **API Gateway** (Spring Cloud Gateway) as single entry point.
- **Kafka** for **two‑way asynchronous communication** between `payment-service` and `pg-service`.
- Each service has its own database (PostgreSQL).

## 2. Mandatory patterns (from Phase 5/6)
- **Outbox pattern** – every event that must be sent to Kafka is first stored in an `outbox` table, then polled and sent by a scheduler. This ensures **at‑least‑once delivery**.
- **Idempotency** – all payment endpoints must accept `Idempotency-Key` header. Use Redis with `SET NX` to guarantee exactly‑once processing.
- **State machine** – each payment transaction has a state (`PENDING`, `PG_PENDING`, `PG_SUCCESS`, `COMPLETED`, `FAILED`, `TIMEOUT`). Transitions are driven by events.
- **Virtual threads** – use Java 21+ virtual threads for blocking operations (e.g., calling external PG APIs). This improves throughput significantly.
- **Distributed tracing** – OpenTelemetry with trace propagation via Kafka headers.

## 3. Kafka topics (naming)
- Command topics: `payment.commands.confirm` (payment‑>pg)
- Event topics: `payment.events.confirmed` (pg‑>payment)
- Each service publishes events only through outbox.

## 4. Error handling & recovery
- Each service has a **scheduled recovery job** that retries failed transactions (with exponential backoff).
- Failed outbox messages are retried up to 5 times, then moved to a dead‑letter table.

## 5. Testing
- Use `@EmbeddedKafka`, `@DataJpaTest` with H2, and `embedded-redis` for integration tests.
- Unit tests for state machine, idempotency logic, and outbox poller.

## 6. Code style
- Use **Lombok** (`@Data`, `@Builder`, `@Slf4j`) as mandatory.
- Package structure: `com.paymentplatform.[service-name]` (e.g., `com.paymentplatform.payment`).
- Follow the same hexagonal‑like layering as in `payment-platform` original: `domain`, `application`, `adapter`, `infrastructure`.