package com.paymentplatform.userservice.adapter.out.persistence;

import com.paymentplatform.userservice.application.port.out.PublishEventPort;
import com.paymentplatform.userservice.infrastructure.outbox.OutboxEntity;
import com.paymentplatform.userservice.infrastructure.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OutboxPublishEventAdapter implements PublishEventPort {

    private final OutboxRepository outboxRepository;

    @Override
    public void publish(String topic, String aggregateId, String eventType, String payload) {
        OutboxEntity entity = OutboxEntity.builder()
                .topic(topic)
                .aggregateType("USER")
                .aggregateId(aggregateId)
                .type(eventType)
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .status("PENDING")
                .build();
        outboxRepository.save(entity);
    }
}
