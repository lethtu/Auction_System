package com.auction.server.exception;

public class InvalidItemException extends ValidationException {
    public InvalidItemException(String message) {
        super(message);
    }
}
