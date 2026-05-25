package com.auction.server.exception;

/**
 * Exception thrown when authentication or authorization fails.
 */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
