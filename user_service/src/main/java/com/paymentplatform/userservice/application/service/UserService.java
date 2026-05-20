package com.paymentplatform.userservice.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.userservice.application.exception.ConcurrentRequestException;
import com.paymentplatform.userservice.application.exception.UserNotFoundException;
import com.paymentplatform.userservice.application.port.in.CreateUserUseCase;
import com.paymentplatform.userservice.application.port.out.PublishEventPort;
import com.paymentplatform.userservice.application.port.out.UserRepositoryPort;
import com.paymentplatform.userservice.domain.User;
import com.paymentplatform.userservice.domain.event.UserCreatedEvent;
import com.paymentplatform.userservice.infrastructure.idempotency.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements CreateUserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PublishEventPort publishEventPort;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public User createUser(CreateUserCommand command, String idempotencyKey) {
        String status = idempotencyService.getStatus(idempotencyKey);

        if (status != null) {
            if ("PROCESSING".equals(status)) {
                throw new ConcurrentRequestException("Request with this idempotency key is already being processed");
            }
            if (status.startsWith("userId:")) {
                Long userId = Long.parseLong(status.substring(7));
                log.info("Duplicate request for idempotencyKey: {}, returning existing user id: {}", idempotencyKey, userId);
                return userRepositoryPort.findById(userId)
                        .orElseThrow(() -> new UserNotFoundException("User not found for cached ID: " + userId));
            }
            // If the key is in some other state (like FAILED or invalid), we clean it up and proceed
            idempotencyService.remove(idempotencyKey);
        }

        // Attempt to acquire idempotency lock
        if (!idempotencyService.startProcessing(idempotencyKey)) {
            throw new ConcurrentRequestException("Duplicate request or concurrent processing detected");
        }

        // Register transaction synchronization to handle key cleanup on rollback
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int completionStatus) {
                    if (completionStatus != STATUS_COMMITTED) {
                        log.warn("Transaction rolled back or failed, deleting idempotency key: {}", idempotencyKey);
                        idempotencyService.remove(idempotencyKey);
                    }
                }
            });
        }

        try {
            User user = User.builder()
                    .username(command.username())
                    .email(command.email())
                    .status("ACTIVE")
                    .build();

            User savedUser = userRepositoryPort.save(user);

            // Safely serialize user details as valid JSON
            String payload;
            try {
                UserCreatedEvent event = new UserCreatedEvent(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());
                payload = objectMapper.writeValueAsString(event);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize event payload for user creation", e);
            }

            // Publish event via Outbox
            publishEventPort.publish("user.events.created", String.valueOf(savedUser.getId()), "USER_CREATED", payload);

            // Register transaction synchronization to complete the key on commit
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        log.info("Transaction committed successfully. Completing idempotency key: {} with user ID: {}", 
                                idempotencyKey, savedUser.getId());
                        idempotencyService.complete(idempotencyKey, "userId:" + savedUser.getId());
                    }
                });
            } else {
                idempotencyService.complete(idempotencyKey, "userId:" + savedUser.getId());
            }

            return savedUser;
        } catch (Exception e) {
            // Clean up key if exception is thrown before transaction commits/synchronizations run
            idempotencyService.remove(idempotencyKey);
            throw e;
        }
    }
}
