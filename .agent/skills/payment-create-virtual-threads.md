
---
name: payment-platform-virtual-threads
description: Enable Java 21 virtual threads for blocking IO (e.g., external PG calls)
version: 2.0
---

# Skill: Enable Virtual Threads

## Goal
Improve throughput when calling external, slow services (e.g., payment gateways) by using virtual threads.

## Step 1: Configure Spring Boot
In `application.yml`:
```yaml
spring:
  threads:
    virtual:
      enabled: true
```
Step 2: Use Virtual Thread Executor for Blocking Calls
```java
@Service
public class PgGatewayClient {
    private final RestClient restClient = RestClient.create();

    public PaymentResponse callExternalPg(PaymentConfirmCommand cmd) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<PaymentResponse> future = executor.submit(() -> 
                restClient.post()
                    .uri("https://mock-pg.com/pay")
                    .body(cmd)
                    .retrieve()
                    .body(PaymentResponse.class)
            );
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("PG call failed", e);
        }
    }
}
```
Step 3: Alternative – Use Async HTTP Client with Virtual Threads
```java
// Using Spring's WebClient with blocking wrapper
public PaymentResponse callWithWebClient(PaymentConfirmCommand cmd) {
    return Executors.newVirtualThreadPerTaskExecutor().submit(() ->
        WebClient.create()
            .post()
            .uri("https://mock-pg.com/pay")
            .bodyValue(cmd)
            .retrieve()
            .bodyToMono(PaymentResponse.class)
            .block()
    ).get();
}
```
Testing
Measure TPS with virtual threads vs traditional threads when calling external PG (simulate delay with Thread.sleep).

Verify that virtual threads do NOT exhaust memory under load.

Pitfalls
Using virtual threads for CPU-bound tasks → no benefit.

Mixing blocking and non-blocking code in same virtual thread → avoid if possible.

Exposing virtual thread executor directly → better to use wrapper (Executors.newVirtualThreadPerTaskExecutor()) for safety.
```