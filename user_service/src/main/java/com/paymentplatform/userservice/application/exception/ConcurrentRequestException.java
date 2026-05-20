package com.paymentplatform.userservice.application.exception;

public class ConcurrentRequestException extends RuntimeException {
    public ConcurrentRequestException(String message) {
        super(message);
    }
}
