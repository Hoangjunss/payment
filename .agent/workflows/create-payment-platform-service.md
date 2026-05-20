---
description: 
---



---
name: create-payment-platform-service
description: Scaffold a full payment‑platform style service (payment or pg) with all mandatory patterns
version: 1.0
---

# Workflow: /create-payment-platform-service <service-name>

## Steps
1. Generate Maven module with `pom.xml` (dependencies: Spring Boot, Kafka, JPA, Redis, Avro, Eureka client, Gateway).
2. Create package structure `com.paymentplatform.<service-name>` with layers: domain, application, adapter, infrastructure.
3. Create domain aggregate with **state machine** (skill `payment-create-state-machine`).
4. Create **outbox infrastructure** (skill `payment-create-outbox`).
5. Create **idempotency service** (skill `payment-create-idempotency`).
6. Create **Kafka two‑way** consumers/producers (skill `payment-create-kafka-two-way`).
7. Enable **virtual threads** (skill `payment-create-virtual-threads`).
8. Add **Eureka client** config for service discovery.
9. Generate unit and integration tests.

## Example
`/create-payment-platform-service pg-service`