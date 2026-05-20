package com.paymentplatform.userservice.adapter.in.web;

import com.paymentplatform.userservice.application.exception.ConcurrentRequestException;
import com.paymentplatform.userservice.application.exception.UserNotFoundException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConcurrentRequestException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentRequest(ConcurrentRequestException ex) {
        log.warn("Concurrent/duplicate request processing: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.CONFLICT.value())
                        .error("Conflict")
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        log.warn("User not found exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.NOT_FOUND.value())
                        .error("Not Found")
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("Bad Request")
                        .message("Validation failed")
                        .details(errors)
                        .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON payload: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("Bad Request")
                        .message("Malformed JSON payload or invalid request body structure")
                        .build());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("Bad Request")
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("Bad Request")
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .error("Internal Server Error")
                        .message("An unexpected error occurred")
                        .build());
    }

    @Data
    @Builder
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private Object details;
    }
}
