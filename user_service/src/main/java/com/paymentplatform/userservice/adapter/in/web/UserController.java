package com.paymentplatform.userservice.adapter.in.web;

import com.paymentplatform.userservice.application.port.in.CreateUserUseCase;
import com.paymentplatform.userservice.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final CreateUserUseCase createUserUseCase;

    @PostMapping
    public ResponseEntity<User> createUser(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreateUserUseCase.CreateUserCommand command) {
        
        User user = createUserUseCase.createUser(command, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
}
