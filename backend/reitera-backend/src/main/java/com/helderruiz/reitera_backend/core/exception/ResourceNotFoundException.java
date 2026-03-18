package com.helderruiz.reitera_backend.core.exception;

/**
 * Exception thrown when a requested resource (Deck, Card, etc.) is not found in the database.
 * Results in an HTTP 404 response via GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
