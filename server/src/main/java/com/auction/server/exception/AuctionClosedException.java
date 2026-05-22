package com.auction.server.exception;

/**
 * Exception thrown when a bid is placed on an auction session that has ENDED or been CANCELED.
 * Extends RuntimeException so callers are not forced to declare throws.
 */
public class AuctionClosedException extends RuntimeException {
    private final Integer sessionId;

    public AuctionClosedException(String message, Integer sessionId) {
        super(message);
        this.sessionId = sessionId;
    }

    public Integer getSessionId() {
        return sessionId;
    }
}
