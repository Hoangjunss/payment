package com.paymentplatform.userservice.application.service;

import com.paymentplatform.userservice.application.port.in.CreateUserUseCase;
import com.paymentplatform.userservice.application.port.out.PublishEventPort;
import com.paymentplatform.userservice.application.port.out.UserRepositoryPort;
import com.paymentplatform.userservice.domain.User;
import com.paymentplatform.userservice.infrastructure.idempotency.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements CreateUserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PublishEventPort publishEventPort;
    private final IdempotencyService idempotencyService;

    @Override
    @Transactional
    public User createUser(CreateUserCommand command, String idempotencyKey) {
        if (!idempotencyService.isIdempotent(idempotencyKey)) {
            log.info("Duplicate request detected for idempotencyKey: {}", idempotencyKey);
            // In a real scenario, you might return the existing resource or throw an exception
            // For simplicity, we just return a stub or throw
            throw new IllegalStateException("Duplicate request");
        }

        User user = User.builder()
                .username(command.username())
                .email(command.email())
                .status("ACTIVE")
                .build();

        User savedUser = userRepositoryPort.save(user);

        // Publish event via Outbox
        String payload = String.format("{\"id\":%d, \"username\":\"%s\", \"email\":\"%s\"}", 
                savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());
        
        publishEventPort.publish("user.events.created", String.valueOf(savedUser.getId()), "USER_CREATED", payload);

        return savedUser;
    }
}
