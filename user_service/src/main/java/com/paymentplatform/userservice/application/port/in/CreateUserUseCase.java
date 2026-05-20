package com.paymentplatform.userservice.application.port.in;

import com.paymentplatform.userservice.domain.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public interface CreateUserUseCase {
    User createUser(CreateUserCommand command, String idempotencyKey);

    record CreateUserCommand(
            @NotBlank(message = "Username cannot be empty")
            String username,

            @NotBlank(message = "Email cannot be empty")
            @Email(message = "Invalid email format")
            String email
    ) {}
}
