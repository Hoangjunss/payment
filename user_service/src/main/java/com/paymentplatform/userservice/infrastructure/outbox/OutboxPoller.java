package com.paymentplatform.userservice.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.poller.delay:2000}")
    public void pollOutbox() {
        List<OutboxEntity> pendingMessages = outboxRepository.findPendingMessages();

        for (OutboxEntity message : pendingMessages) {
            try {
                // Optimistic locking handles concurrent modifications if multiple pods run
                message.setStatus("SENT");
                message.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(message);

                kafkaTemplate.send(message.getTopic(), message.getAggregateId(), message.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send message to Kafka: topic={}, id={}", message.getTopic(), message.getId(), ex);
                            // In a real scenario, you might want a callback to mark it FAILED and increment retryCount
                        }
                    });

            } catch (Exception e) {
                log.error("Optimistic locking failure or other error when processing outbox id={}", message.getId(), e);
            }
        }
    }
}
