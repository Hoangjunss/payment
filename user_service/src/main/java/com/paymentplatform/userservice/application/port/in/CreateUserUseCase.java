package com.paymentplatform.userservice.application.port.in;

import com.paymentplatform.userservice.domain.User;

public interface CreateUserUseCase {
    User createUser(CreateUserCommand command, String idempotencyKey);

    record CreateUserCommand(String username, String email) {}
}
