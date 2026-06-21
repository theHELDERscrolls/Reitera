package com.helderruiz.reitera_backend.core.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Centralized exception handler for all REST controllers.
 * Maps application exceptions to appropriate HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Bean Validation errors (@NotBlank, @Email, etc.) and returns HTTP 400
     * with a map of field names to their validation messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**
     * Handles invalid, expired, or revoked refresh tokens and returns HTTP 401.
     * Signals the client that the session cannot be renewed and re-authentication is required.
     */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles resource not found errors (Deck, Card, Category, etc.) and returns HTTP 404.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles data conflict errors (duplicate email, duplicate username, etc.) and returns HTTP 409.
     * Allows the client to distinguish conflicts from other validation errors.
     */
    @ExceptionHandler(DataConflictException.class)
    public ResponseEntity<Map<String, String>> handleDataConflict(DataConflictException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles expired password reset tokens and returns HTTP 410 Gone.
     * Distinct from 400 so the frontend can show a targeted "link expired" message.
     */
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<Map<String, String>> handleTokenExpired(TokenExpiredException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.GONE).body(error);
    }

    /**
     * Handles business rule violations explicitly thrown by services and returns HTTP 400.
     * Messages thrown via IllegalArgumentException are considered safe to expose to clients.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles unexpected internal errors (e.g. missing required system data) and returns HTTP 500
     * without exposing the original message to the client.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.error("Internal server error [correlationId={}]", correlationId, ex);

        Map<String, String> error = new HashMap<>();
        error.put("error", "An internal server error occurred");
        error.put("correlationId", correlationId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Catch-all for any unhandled RuntimeException. Returns HTTP 500 with a generic message
     * and a correlation ID; the full stack trace is logged server-side so support can match
     * a user report to a server log without leaking internal details over the wire.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeExceptions(RuntimeException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.error("Unhandled runtime exception [correlationId={}]", correlationId, ex);

        Map<String, String> error = new HashMap<>();
        error.put("error", "An unexpected error occurred");
        error.put("correlationId", correlationId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
