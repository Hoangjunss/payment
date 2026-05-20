package com.paymentplatform.userservice.infrastructure.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "idempotency:";

    @Value("${idempotency.processing-ttl:5m}")
    private Duration processingTtl;

    public Duration getProcessingTtl() {
        return processingTtl;
    }

    public boolean startProcessing(String idempotencyKey) {
        // SET NX with a short, configurable TTL (e.g. 5 minutes)
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + idempotencyKey, "PROCESSING", processingTtl);
        return Boolean.TRUE.equals(success);
    }

    public String getStatus(String idempotencyKey) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
    }

    public Long getTtl(String idempotencyKey) {
        return redisTemplate.getExpire(KEY_PREFIX + idempotencyKey, TimeUnit.SECONDS);
    }

    public void complete(String idempotencyKey, String value) {
        redisTemplate.opsForValue().set(KEY_PREFIX + idempotencyKey, value, Duration.ofHours(24));
    }

    public void remove(String idempotencyKey) {
        redisTemplate.delete(KEY_PREFIX + idempotencyKey);
    }
}
