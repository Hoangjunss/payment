
---
name: payment-platform-idempotency
description: Idempotency using Redis `SET NX` with TTL and concurrency safety
version: 2.0
---

# Skill: Implement Idempotency Key Handling

## Goal
Guarantee that a request with the same `Idempotency-Key` is processed only once, even under concurrent calls.

## Step 1: Redis Configuration
```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```
Step 2: Idempotency Service
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Process a request idempotently.
     * @param key the Idempotency-Key
     * @param supplier the business logic (e.g., create payment)
     * @return the cached or fresh result
     */
    public String process(String key, Supplier<String> supplier) {
        // Atomically set if absent
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(key, "PROCESSING", Duration.ofMinutes(5));
        
        if (Boolean.FALSE.equals(acquired)) {
            // Key already exists – wait for result
            return waitForResult(key);
        }

        try {
            String result = supplier.get();
            // Store final result for 24h
            redisTemplate.opsForValue().set(key, result, Duration.ofHours(24));
            return result;
        } catch (Exception e) {
            redisTemplate.delete(key);
            throw e;
        }
    }

    private String waitForResult(String key) {
        // Simple polling – in production use Redis pub/sub or exponential backoff
        for (int i = 0; i < 30; i++) {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null && !"PROCESSING".equals(value)) {
                return value;
            }
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
        throw new RuntimeException("Idempotency timeout for key: " + key);
    }
}
```
Step 3: Use in Controller
```java
@PostMapping("/payments")
public ResponseEntity<PaymentResponse> create(
        @RequestBody PaymentRequest request,
        @RequestHeader("Idempotency-Key") String idempotencyKey) {
    String result = idempotencyService.process(idempotencyKey,
        () -> paymentService.create(request));
    return ResponseEntity.ok(objectMapper.readValue(result, PaymentResponse.class));
}
```
Testing
Test with multiple requests having the same key – only the first should execute.

Verify that after failure, the key is deleted and retry works.

Ensure TTL works correctly (clears after 24h).
Pitfalls
Missing setIfAbsent atomicity – use Redis command, not check-then-set.

Too short TTL for processing – adjust based on expected business time.

Not handling deserialization of cached result correctly.