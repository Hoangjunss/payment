package com.paymentplatform.userservice.domain.event;

public record UserCreatedEvent(Long id, String username, String email) {
}
