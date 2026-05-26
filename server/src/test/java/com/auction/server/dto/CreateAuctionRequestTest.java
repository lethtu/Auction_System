package com.auction.server.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CreateAuctionRequestTest {

    @Test
    void noArgConstructorDefaultsToNullFields() {
        CreateAuctionRequest request = new CreateAuctionRequest();

        assertNull(request.getName());
        assertNull(request.getType());
        assertNull(request.getDescription());
        assertNull(request.getImagePath());
        assertNull(request.getSellerId());
        assertNull(request.getStartingPrice());
        assertNull(request.getStepPrice());
        assertNull(request.getReservePrice());
        assertNull(request.getStartTime());
        assertNull(request.getEndTime());
        assertNull(request.getStatus());
        assertNull(request.getApplyMinRate());
        assertNull(request.getMinRate());
    }

    @Test
    void fullConstructorStoresCoreAuctionFields() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 26, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 5, 27, 10, 0);

        CreateAuctionRequest request = new CreateAuctionRequest(
                "Vintage Watch",
                "WATCH",
                "Old watch",
                "watch.png",
                7,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(1500),
                start,
                end
        );

        assertEquals("Vintage Watch", request.getName());
        assertEquals("WATCH", request.getType());
        assertEquals("Old watch", request.getDescription());
        assertEquals("watch.png", request.getImagePath());
        assertEquals(7, request.getSellerId());
        assertEquals(BigDecimal.valueOf(1000), request.getStartingPrice());
        assertEquals(BigDecimal.valueOf(50), request.getStepPrice());
        assertEquals(BigDecimal.valueOf(1500), request.getReservePrice());
        assertEquals(start, request.getStartTime());
        assertEquals(end, request.getEndTime());
    }

    @Test
    void settersUpdateAllOptionalAndCoreFields() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 8, 30);
        LocalDateTime end = LocalDateTime.of(2026, 6, 2, 8, 30);

        request.setName("Painting");
        request.setType("ART");
        request.setDescription("Oil painting");
        request.setImagePath("painting.png");
        request.setSellerId(11);
        request.setStartingPrice(BigDecimal.valueOf(200));
        request.setStepPrice(BigDecimal.valueOf(10));
        request.setReservePrice(BigDecimal.valueOf(500));
        request.setStartTime(start);
        request.setEndTime(end);
        request.setStatus("COMING");
        request.setApplyMinRate(Boolean.TRUE);
        request.setMinRate(BigDecimal.valueOf(300));

        assertEquals("Painting", request.getName());
        assertEquals("ART", request.getType());
        assertEquals("Oil painting", request.getDescription());
        assertEquals("painting.png", request.getImagePath());
        assertEquals(11, request.getSellerId());
        assertEquals(BigDecimal.valueOf(200), request.getStartingPrice());
        assertEquals(BigDecimal.valueOf(10), request.getStepPrice());
        assertEquals(BigDecimal.valueOf(500), request.getReservePrice());
        assertEquals(start, request.getStartTime());
        assertEquals(end, request.getEndTime());
        assertEquals("COMING", request.getStatus());
        assertTrue(request.getApplyMinRate());
        assertEquals(BigDecimal.valueOf(300), request.getMinRate());
    }
}
