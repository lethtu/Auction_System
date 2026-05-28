package com.auction.server.exception;

/**
 * Input/request validation failed.
 */
public class ValidationException extends ClientErrorException {
    public ValidationException(String message) {
        super(400, message);
    }
}
