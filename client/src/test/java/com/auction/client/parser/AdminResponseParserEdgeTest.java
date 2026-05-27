package com.auction.client.parser;

import com.auction.client.model.AdminSessionRow;
import com.auction.client.model.AdminUserRow;
import com.auction.client.model.PendingSessionRow;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminResponseParserEdgeTest {

    @Test
    void parseAllSessions_currentPriceFallsBackToStartingPriceWhenMissingZeroOrInvalid() {
        JSONArray data = new JSONArray("""
                [
                  {
                    "id": 10,
                    "productId": 99,
                    "productName": "Laptop",
                    "sellerUsername": "seller01",
                    "startingPrice": 1000000,
                    "currentPrice": 1250000,
                    "status": "ACTIVE",
                    "productVisible": false
                  },
                  {
                    "id": 11,
                    "productName": "Phone",
                    "sellerUsername": "seller02",
                    "startingPrice": 500000,
                    "currentPrice": 0,
                    "status": "ENDED"
                  },
                  {
                    "id": 12,
                    "productName": "Tablet",
                    "sellerUsername": "seller03",
                    "startingPrice": 700000,
                    "currentPrice": "not-a-number",
                    "status": "PENDING"
                  },
                  {
                    "id": 13,
                    "productName": "Camera",
                    "sellerUsername": "seller04",
                    "startingPrice": 900000,
                    "status": "ACTIVE"
                  }
                ]
                """);

        List<AdminSessionRow> rows = AdminResponseParser.parseAllSessions(data);

        assertEquals(4, rows.size());

        AdminSessionRow withPositiveCurrentPrice = rows.get(0);
        assertEquals(10, withPositiveCurrentPrice.getId());
        assertEquals(99, withPositiveCurrentPrice.getProductId());
        assertEquals(new BigDecimal("1000000"), withPositiveCurrentPrice.getStartingPrice());
        assertEquals(new BigDecimal("1250000"), withPositiveCurrentPrice.getCurrentPrice());
        assertFalse(withPositiveCurrentPrice.isProductVisible());

        AdminSessionRow zeroCurrentPrice = rows.get(1);
        assertEquals(new BigDecimal("500000"), zeroCurrentPrice.getStartingPrice());
        assertEquals(new BigDecimal("500000"), zeroCurrentPrice.getCurrentPrice());
        assertTrue(zeroCurrentPrice.isProductVisible());

        AdminSessionRow invalidCurrentPrice = rows.get(2);
        assertEquals(new BigDecimal("700000"), invalidCurrentPrice.getStartingPrice());
        assertEquals(new BigDecimal("700000"), invalidCurrentPrice.getCurrentPrice());

        AdminSessionRow missingCurrentPrice = rows.get(3);
        assertEquals(new BigDecimal("900000"), missingCurrentPrice.getStartingPrice());
        assertEquals(new BigDecimal("900000"), missingCurrentPrice.getCurrentPrice());
    }

    @Test
    void parseRows_skipNonObjectEntriesInsteadOfThrowing() {
        JSONArray mixedData = new JSONArray("""
                [
                  null,
                  123,
                  "bad-row",
                  {"id": 21, "productName": "Valid Item", "startingPrice": 300000}
                ]
                """);

        List<PendingSessionRow> rows = AdminResponseParser.parsePendingSessions(mixedData);

        assertEquals(1, rows.size());
        assertEquals(21, rows.get(0).getId());
        assertEquals("Valid Item", rows.get(0).getProductName());
        assertEquals(new BigDecimal("300000"), rows.get(0).getStartingPrice());
    }

    @Test
    void parseUsers_missingOptionalFieldsDefaultSafely() {
        JSONArray data = new JSONArray("""
                [
                  {"id": 5},
                  {"id": 6, "username": "blocked", "banned": true}
                ]
                """);

        List<AdminUserRow> rows = AdminResponseParser.parseUsers(data);

        assertEquals(2, rows.size());

        AdminUserRow blankUser = rows.get(0);
        assertEquals(5, blankUser.getId());
        assertEquals("", blankUser.getUsername());
        assertEquals("", blankUser.getFullname());
        assertEquals("", blankUser.getEmail());
        assertEquals("", blankUser.getRole());
        assertFalse(blankUser.isBanned());

        AdminUserRow bannedUser = rows.get(1);
        assertEquals(6, bannedUser.getId());
        assertEquals("blocked", bannedUser.getUsername());
        assertTrue(bannedUser.isBanned());
    }

    @Test
    void parseAllSessions_nullArrayReturnsEmptyList() {
        assertTrue(AdminResponseParser.parseAllSessions(null).isEmpty());
    }
}
