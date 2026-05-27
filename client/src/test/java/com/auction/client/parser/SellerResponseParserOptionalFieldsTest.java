package com.auction.client.parser;

import com.auction.client.model.SessionItem;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SellerResponseParserOptionalFieldsTest {

    @Test
    void parseSessions_readsOptionalSellerFieldsUsedByDashboardForms() {
        JSONArray data = new JSONArray("""
                [
                  {
                    "id": 21,
                    "productName": "Vintage Camera",
                    "imagePath": "uploads/images/camera.png",
                    "startTime": "2026-05-27T09:30:00",
                    "endTime": "2026-05-28T09:30:00",
                    "applyMinRate": true,
                    "minRate": " 12.50 ",
                    "highestBidderId": 77
                  }
                ]
                """);

        List<SessionItem> result = SellerResponseParser.parseSessions(data);

        assertEquals(1, result.size());
        SessionItem item = result.get(0);
        assertEquals(21, item.id);
        assertEquals("Vintage Camera", item.productName);
        assertEquals("uploads/images/camera.png", item.imagePath);
        assertEquals("2026-05-27T09:30:00", item.startTime);
        assertEquals("2026-05-28T09:30:00", item.endTime);
        assertTrue(item.applyMinRate);
        assertEquals(new BigDecimal("12.50"), item.minRate);
        assertEquals(77, item.highestBidderId);
    }

    @Test
    void parseSessions_invalidOptionalNumericFieldsFallbackSafely() {
        JSONArray data = new JSONArray("""
                [
                  {
                    "id": 22,
                    "applyMinRate": true,
                    "minRate": "not-a-number",
                    "reservePrice": "bad-price",
                    "highestBidderId": null
                  }
                ]
                """);

        List<SessionItem> result = SellerResponseParser.parseSessions(data);

        assertEquals(1, result.size());
        SessionItem item = result.get(0);
        assertTrue(item.applyMinRate);
        assertEquals(BigDecimal.ZERO, item.minRate);
        assertEquals(BigDecimal.ZERO, item.reservePrice);
        assertNull(item.highestBidderId);
    }

    @Test
    void parseSessions_missingMinRateFieldsKeepSafeDefaults() {
        JSONArray data = new JSONArray("""
                [
                  {
                    "id": 23,
                    "productName": "No Min Rate Item"
                  }
                ]
                """);

        List<SessionItem> result = SellerResponseParser.parseSessions(data);

        assertEquals(1, result.size());
        SessionItem item = result.get(0);
        assertFalse(item.applyMinRate);
        assertEquals(BigDecimal.ZERO, item.minRate);
        assertEquals("", item.imagePath);
        assertEquals("", item.startTime);
    }
}
