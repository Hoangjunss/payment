package com.paymentplatform.userservice.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final DeadLetterOutboxRepository deadLetterOutboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.poller.delay:2000}")
    public void pollOutbox() {
        List<OutboxEntity> pendingMessages = outboxRepository.findPendingMessages();

        for (OutboxEntity message : pendingMessages) {
            try {
                // Try to claim the message using optimistic locking by updating status to PROCESSING
                message.setStatus("PROCESSING");
                message.setProcessedAt(LocalDateTime.now());

                // Save and flush immediately to trigger locking check
                OutboxEntity claimedMessage = outboxRepository.saveAndFlush(message);

                try {
                    // Send to Kafka synchronously and wait for acknowledgment
                    kafkaTemplate.send(claimedMessage.getTopic(), claimedMessage.getAggregateId(), claimedMessage.getPayload())
                            .get(5, TimeUnit.SECONDS);

                    // Successfully sent
                    claimedMessage.setStatus("SENT");
                    outboxRepository.save(claimedMessage);
                    log.info("Successfully published outbox event id={} to topic={}", claimedMessage.getId(), claimedMessage.getTopic());

                } catch (Exception ex) {
                    log.error("Failed to send message to Kafka synchronously: topic={}, id={}", claimedMessage.getTopic(), claimedMessage.getId(), ex);

                    int nextRetryCount = claimedMessage.getRetryCount() + 1;
                    claimedMessage.setRetryCount(nextRetryCount);

                    if (nextRetryCount >= 5) {
                        log.error("Outbox message id={} reached max retries. Moving to dead-letter table.", claimedMessage.getId());

                        DeadLetterOutboxEntity deadLetter = DeadLetterOutboxEntity.builder()
                                .aggregateType(claimedMessage.getAggregateType())
                                .aggregateId(claimedMessage.getAggregateId())
                                .type(claimedMessage.getType())
                                .payload(claimedMessage.getPayload())
                                .topic(claimedMessage.getTopic())
                                .createdAt(claimedMessage.getCreatedAt())
                                .failedAt(LocalDateTime.now())
                                .errorMessage(ex.getMessage() != null ? ex.getMessage() : ex.toString())
                                .build();

                        deadLetterOutboxRepository.save(deadLetter);
                        outboxRepository.delete(claimedMessage);
                    } else {
                        // Reset status to PENDING for retry on the next poller cycle
                        claimedMessage.setStatus("PENDING");
                        outboxRepository.save(claimedMessage);
                    }
                }

            } catch (ObjectOptimisticLockingFailureException e) {
                // Ignore: another pod/thread has already updated and claimed this record
                log.debug("Optimistic locking collision for outbox id={}", message.getId());
            } catch (Exception e) {
                log.error("Unexpected error processing outbox id={}", message.getId(), e);
            }
        }
    }

    // Recover messages that got stuck in PROCESSING (e.g. if the pod crashed mid-execution)
    @Scheduled(fixedDelay = 300000) // Runs every 5 minutes
    public void recoverStuckMessages() {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(5);
        List<OutboxEntity> stuckMessages = outboxRepository.findByStatusAndProcessedAtBefore("PROCESSING", timeout);

        if (!stuckMessages.isEmpty()) {
            log.warn("Found {} outbox messages stuck in PROCESSING state. Resetting them to PENDING.", stuckMessages.size());
            for (OutboxEntity message : stuckMessages) {
                try {
                    message.setStatus("PENDING");
                    outboxRepository.save(message);
                } catch (Exception e) {
                    log.error("Failed to recover stuck outbox message id={}", message.getId(), e);
                }
            }
        }
    }
}
