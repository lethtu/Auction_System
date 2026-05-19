package com.auction.client.util;

import com.auction.client.dto.CreateAuctionRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SellerRequestBuilderTest {

    @Test
    void buildAuctionBody_copiesAllFieldsToJsonWithoutImageField() {
        CreateAuctionRequest request = createRequest();

        JSONObject body = SellerRequestBuilder.buildAuctionBody(request);

        assertEquals("Laptop Gaming", body.getString("name"));
        assertEquals("electronics", body.getString("type"));
        assertEquals("Máy còn tốt", body.getString("description"));
        assertEquals(new BigDecimal("1000000"), body.getBigDecimal("startingPrice"));
        assertEquals(new BigDecimal("100000"), body.getBigDecimal("stepPrice"));
        assertEquals("2026-05-12T10:00:00", body.getString("startTime"));
        assertEquals("2026-05-20T10:00:00", body.getString("endTime"));
        assertEquals(7, body.getInt("sellerId"));
        assertFalse(body.has("imagePath"));
        assertFalse(body.has("imageUrl"));
    }

    private CreateAuctionRequest createRequest() {
        return new CreateAuctionRequest(
                "Laptop Gaming",
                "electronics",
                "Máy còn tốt",
                new BigDecimal("1000000"),
                new BigDecimal("100000"),
                "2026-05-12T10:00:00",
                "2026-05-20T10:00:00",
                7
        );
    }
}
