package com.auction.server.exception;

/**
 * Requested domain resource does not exist.
 */
public class ResourceNotFoundException extends ClientErrorException {
    public ResourceNotFoundException(String message) {
        super(404, message);
    }
}
