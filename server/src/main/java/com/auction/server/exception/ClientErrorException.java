package com.auction.server.exception;

/**
 * Base class for errors caused by a client request or a business rule violation.
 * It keeps the response status close to the exception while still behaving like
 * IllegalArgumentException for older service tests and callers.
 */
public class ClientErrorException extends IllegalArgumentException {
    private final int status;

    protected ClientErrorException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
