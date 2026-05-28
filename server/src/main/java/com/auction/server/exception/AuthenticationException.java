package com.auction.server.exception;

/**
 * Exception thrown when authentication fails.
 */
public class AuthenticationException extends ClientErrorException {
    public AuthenticationException(String message) {
        super(401, message);
    }
}
