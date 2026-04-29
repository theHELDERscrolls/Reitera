package com.helderruiz.reitera_backend.core.exception;

/**
 * Thrown when a refresh token is invalid, expired, or has been revoked.
 * Mapped to HTTP 401 by GlobalExceptionHandler to signal an authentication failure,
 * allowing clients to redirect to the login screen.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
