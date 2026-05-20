package com.paymentplatform.userservice.infrastructure.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "idempotency:";

    public boolean startProcessing(String idempotencyKey) {
        // SET NX with a TTL of 24 hours
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + idempotencyKey, "PROCESSING", Duration.ofHours(24));
        return Boolean.TRUE.equals(success);
    }

    public String getStatus(String idempotencyKey) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
    }

    public void complete(String idempotencyKey, String value) {
        redisTemplate.opsForValue().set(KEY_PREFIX + idempotencyKey, value, Duration.ofHours(24));
    }

    public void remove(String idempotencyKey) {
        redisTemplate.delete(KEY_PREFIX + idempotencyKey);
    }
}
