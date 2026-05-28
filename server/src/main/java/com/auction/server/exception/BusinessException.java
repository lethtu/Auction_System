package com.auction.server.exception;

/**
 * A valid request cannot be completed because it violates an application rule.
 */
public class BusinessException extends ClientErrorException {
    public BusinessException(String message) {
        super(400, message);
    }
}
