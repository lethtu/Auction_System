package com.auction.client.parser;

import com.auction.client.model.SessionItem;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SellerResponseParserTest {

    @Test
    void parseSessions_nullData_returnsEmptyList() {
        List<SessionItem> result = SellerResponseParser.parseSessions(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseSessions_validData_returnsSessionItems() {
        JSONArray data = new JSONArray("""
                [
                  {
                    "id": 1,
                    "productName": "Laptop",
                    "productType": "Electronics",
                    "description": "Gaming laptop",
                    "startingPrice": 1000000,
                    "currentPrice": 1500000,
                    "stepPrice": 100000,
                    "reservePrice": 2000000,
                    "highestBidderId": 14,
                    "endTime": "2026-05-20T10:00:00",
                    "status": "PENDING"
                  }
                ]
                """);

        List<SessionItem> result = SellerResponseParser.parseSessions(data);

        assertEquals(1, result.size());

        SessionItem item = result.get(0);
        assertEquals(1, item.id);
        assertEquals("Laptop", item.productName);
        assertEquals("Electronics", item.productType);
        assertEquals("Gaming laptop", item.description);
        assertEquals(new BigDecimal("1000000"), item.startingPrice);
        assertEquals(new BigDecimal("1500000"), item.currentPrice);
        assertEquals(new BigDecimal("100000"), item.stepPrice);
        assertEquals(new BigDecimal("2000000"), item.reservePrice);
        assertEquals(14, item.highestBidderId);
        assertEquals("2026-05-20T10:00:00", item.endTime);
        assertEquals("PENDING", item.status);
    }

    @Test
    void parseSessions_missingFields_usesDefaultValues() {
        JSONArray data = new JSONArray("""
                [
                  {
                    "id": 5
                  }
                ]
                """);

        List<SessionItem> result = SellerResponseParser.parseSessions(data);

        assertEquals(1, result.size());

        SessionItem item = result.get(0);
        assertEquals(5, item.id);
        assertEquals("Unknown", item.productName);
        assertEquals("", item.productType);
        assertEquals("", item.description);
        assertEquals(BigDecimal.ZERO, item.startingPrice);
        assertEquals(BigDecimal.ZERO, item.currentPrice);
        assertEquals(BigDecimal.ZERO, item.stepPrice);
        assertEquals(BigDecimal.ZERO, item.reservePrice);
        assertNull(item.highestBidderId);
        assertEquals("", item.endTime);
        assertEquals("UNKNOWN", item.status);
    }

    @Test
    void parseSessions_invalidPrice_returnsZero() {
        JSONArray data = new JSONArray("""
                [
                  {
                    "id": 10,
                    "productName": "Phone",
                    "startingPrice": "abc",
                    "currentPrice": null,
                    "stepPrice": "50000"
                  }
                ]
                """);

        List<SessionItem> result = SellerResponseParser.parseSessions(data);

        assertEquals(1, result.size());

        SessionItem item = result.get(0);
        assertEquals(BigDecimal.ZERO, item.startingPrice);
        assertEquals(BigDecimal.ZERO, item.currentPrice);
        assertEquals(new BigDecimal("50000"), item.stepPrice);
    }

    @Test
    void parseSessions_arrayWithInvalidItem_skipsInvalidItem() {
        JSONArray data = new JSONArray("""
                [
                  "invalid",
                  {
                    "id": 2,
                    "productName": "Watch"
                  }
                ]
                """);

        List<SessionItem> result = SellerResponseParser.parseSessions(data);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).id);
        assertEquals("Watch", result.get(0).productName);
    }
}
