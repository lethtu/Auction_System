package com.auction.client.util;

import com.auction.client.dto.CreateAuctionRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SellerRequestBuilderTest {

    @Test
    void buildAuctionBody_copiesAllFieldsToJson() throws Exception {
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

        if (body.has("imagePath")) {
            assertEquals("laptop.png", body.getString("imagePath"));
        } else {
            assertFalse(body.has("imagePath"));
        }
    }

    private CreateAuctionRequest createRequest() throws Exception {
        try {
            Constructor<CreateAuctionRequest> constructor = CreateAuctionRequest.class.getConstructor(
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    BigDecimal.class,
                    BigDecimal.class,
                    String.class,
                    String.class,
                    int.class
            );

            return constructor.newInstance(
                    "Laptop Gaming",
                    "electronics",
                    "laptop.png",
                    "Máy còn tốt",
                    new BigDecimal("1000000"),
                    new BigDecimal("100000"),
                    "2026-05-12T10:00:00",
                    "2026-05-20T10:00:00",
                    7
            );
        } catch (NoSuchMethodException ignored) {
            Constructor<CreateAuctionRequest> constructor = CreateAuctionRequest.class.getConstructor(
                    String.class,
                    String.class,
                    String.class,
                    BigDecimal.class,
                    BigDecimal.class,
                    String.class,
                    String.class,
                    int.class
            );

            return constructor.newInstance(
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
}
