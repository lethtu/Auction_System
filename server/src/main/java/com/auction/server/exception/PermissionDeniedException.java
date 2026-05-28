package com.auction.server.exception;

/**
 * Authenticated user is not allowed to perform the requested action.
 */
public class PermissionDeniedException extends ClientErrorException {
    public PermissionDeniedException(String message) {
        super(403, message);
    }
}
