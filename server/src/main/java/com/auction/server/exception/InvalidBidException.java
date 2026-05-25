package com.auction.server.exception;

/**
 * Exception thrown when a bid is placed that violates bidding rules
 * (e.g. bidding below the minimum increment, bidding on one's own auction, etc.).
 */
public class InvalidBidException extends IllegalArgumentException {
    public InvalidBidException(String message) {
        super(message);
    }
}
