package com.auction.server.exception;

public class InvalidItemException extends IllegalArgumentException {
    public InvalidItemException(String message) {
        super(message);
    }
}
