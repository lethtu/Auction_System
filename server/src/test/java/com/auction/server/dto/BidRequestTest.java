package com.auction.server.dto;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BidRequestTest {

    @Test
    void constructorAndGetters_storeProvidedValues() {
        BigDecimal amount = new BigDecimal("150.50");

        BidRequest request = new BidRequest(10, 20, amount);

        assertEquals(10, request.getAuctionId());
        assertEquals(20, request.getBidderId());
        assertEquals(amount, request.getBidAmount());
    }

    @Test
    void constructor_acceptsNullValues() {
        BidRequest request = new BidRequest(null, null, null);

        assertNull(request.getAuctionId());
        assertNull(request.getBidderId());
        assertNull(request.getBidAmount());
    }

    @Test
    void toString_containsMainFields() {
        BidRequest request = new BidRequest(1, 2, new BigDecimal("300.00"));

        String text = request.toString();

        assertTrue(text.contains("auctionId=1"));
        assertTrue(text.contains("bidder=2"));
        assertTrue(text.contains("amount=300.00"));
    }

    @Test
    void request_isSerializable() throws Exception {
        BidRequest request = new BidRequest(3, 4, new BigDecimal("999.99"));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(request);
        }

        Object restored;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = input.readObject();
        }

        BidRequest restoredRequest = assertInstanceOf(BidRequest.class, restored);
        assertEquals(3, restoredRequest.getAuctionId());
        assertEquals(4, restoredRequest.getBidderId());
        assertEquals(new BigDecimal("999.99"), restoredRequest.getBidAmount());
    }
}
