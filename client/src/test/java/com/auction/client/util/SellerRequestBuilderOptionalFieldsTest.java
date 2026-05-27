package com.auction.client.util;

import com.auction.client.dto.CreateAuctionRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellerRequestBuilderOptionalFieldsTest {

    @Test
    void buildAuctionBody_withImageReserveAndMinRate_includesOptionalFields() {
        CreateAuctionRequest request = new CreateAuctionRequest(
                "Luxury Watch",
                "Accessories",
                "Limited edition",
                " uploads/watch.glb ",
                new BigDecimal("1000000"),
                new BigDecimal("100000"),
                new BigDecimal("1800000"),
                "2026-06-01T09:00:00",
                "2026-06-08T09:00:00",
                15,
                true,
                new BigDecimal("1500000")
        );

        JSONObject body = SellerRequestBuilder.buildAuctionBody(request);

        assertEquals("Luxury Watch", body.getString("name"));
        assertEquals("Accessories", body.getString("type"));
        assertEquals("Limited edition", body.getString("description"));
        assertEquals("uploads/watch.glb", body.getString("imagePath"));
        assertEquals(new BigDecimal("1000000"), body.getBigDecimal("startingPrice"));
        assertEquals(new BigDecimal("100000"), body.getBigDecimal("stepPrice"));
        assertEquals(new BigDecimal("1800000"), body.getBigDecimal("reservePrice"));
        assertEquals("2026-06-01T09:00:00", body.getString("startTime"));
        assertEquals("2026-06-08T09:00:00", body.getString("endTime"));
        assertEquals(15, body.getInt("sellerId"));
        assertTrue(body.getBoolean("applyMinRate"));
        assertEquals(new BigDecimal("1500000"), body.getBigDecimal("minRate"));
    }

    @Test
    void buildAuctionBody_withoutOptionalValues_omitsOptionalFieldsAndDefaultsApplyMinRateFalse() {
        CreateAuctionRequest request = new CreateAuctionRequest(
                "Old Camera",
                "Electronics",
                "Working condition",
                "   ",
                new BigDecimal("500000"),
                new BigDecimal("50000"),
                null,
                "2026-06-02T09:00:00",
                "2026-06-09T09:00:00",
                16,
                null,
                null
        );

        JSONObject body = SellerRequestBuilder.buildAuctionBody(request);

        assertFalse(body.has("imagePath"));
        assertFalse(body.has("reservePrice"));
        assertFalse(body.has("minRate"));
        assertFalse(body.getBoolean("applyMinRate"));
    }
}