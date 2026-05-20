package com.paymentplatform.userservice.infrastructure.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    public boolean isIdempotent(String idempotencyKey) {
        // SET NX with a TTL of 24 hours
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent("idempotency:" + idempotencyKey, "PROCESSED", Duration.ofHours(24));
        return Boolean.TRUE.equals(success);
    }
}
