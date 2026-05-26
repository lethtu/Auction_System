package com.auction.server.exception;

import com.auction.server.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleAuthenticationException_returnsUnauthorizedResponse() {
        ApiResponse<?> response = handler.handleAuthenticationException(new AuthenticationException("Invalid token"));

        assertEquals(401, response.getStatus());
        assertEquals("Invalid token", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void handleInvalidBidException_returnsBadRequestResponse() {
        ApiResponse<?> response = handler.handleInvalidBidException(new InvalidBidException("Bid too low"));

        assertEquals(400, response.getStatus());
        assertEquals("Bid too low", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void handleAuctionClosedException_returnsBadRequestResponse() {
        ApiResponse<?> response = handler.handleAuctionClosedException(new AuctionClosedException("Auction closed", 12));

        assertEquals(400, response.getStatus());
        assertEquals("Auction closed", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void handleInvalidItemException_returnsBadRequestResponse() {
        ApiResponse<?> response = handler.handleInvalidItemException(new InvalidItemException("Invalid item"));

        assertEquals(400, response.getStatus());
        assertEquals("Invalid item", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void handleBadRequestException_supportsIllegalArgumentAndIllegalState() {
        ApiResponse<?> illegalArgument = handler.handleBadRequestException(new IllegalArgumentException("Missing field"));
        ApiResponse<?> illegalState = handler.handleBadRequestException(new IllegalStateException("Wrong state"));

        assertEquals(400, illegalArgument.getStatus());
        assertEquals("Missing field", illegalArgument.getMessage());
        assertEquals(400, illegalState.getStatus());
        assertEquals("Wrong state", illegalState.getMessage());
    }

    @Test
    void handleSecurityException_returnsForbiddenResponse() {
        ApiResponse<?> response = handler.handleSecurityException(new SecurityException("Forbidden"));

        assertEquals(403, response.getStatus());
        assertEquals("Forbidden", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void handleGenericException_hidesInternalMessage() {
        ApiResponse<?> response = handler.handleGenericException(new RuntimeException("Sensitive database detail"));

        assertEquals(500, response.getStatus());
        assertEquals("Internal server error", response.getMessage());
        assertNull(response.getData());
    }
}
