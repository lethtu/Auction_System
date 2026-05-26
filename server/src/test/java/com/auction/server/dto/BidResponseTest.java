package com.auction.server.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BidResponseTest {

    @Test
    void minimalConstructorStoresSuccessMessageAndPrice() {
        BidResponse response = new BidResponse(true, "ok", BigDecimal.valueOf(100));

        assertTrue(response.isSuccess());
        assertEquals("ok", response.getMessage());
        assertEquals(BigDecimal.valueOf(100), response.getCurrentPrice());
        assertNull(response.getNewEndTime());
        assertNull(response.getHighestBidderId());
        assertNull(response.getBidCount());
        assertNull(response.getBidTime());
        assertNull(response.getBidId());
        assertNull(response.getPreviousHighestBidderId());
    }

    @Test
    void overloadedConstructorsPopulateProgressivelyMoreFields() {
        BidResponse withEndTime = new BidResponse(
                true,
                "extended",
                BigDecimal.valueOf(120),
                "2026-05-26T10:00:00"
        );
        BidResponse withBidder = new BidResponse(
                true,
                "bidder",
                BigDecimal.valueOf(130),
                "2026-05-26T11:00:00",
                5
        );
        BidResponse withCount = new BidResponse(
                true,
                "count",
                BigDecimal.valueOf(140),
                "2026-05-26T12:00:00",
                6,
                3
        );
        BidResponse withTime = new BidResponse(
                true,
                "time",
                BigDecimal.valueOf(150),
                "2026-05-26T13:00:00",
                7,
                4,
                "2026-05-26T12:55:00"
        );
        BidResponse withId = new BidResponse(
                true,
                "id",
                BigDecimal.valueOf(160),
                "2026-05-26T14:00:00",
                8,
                5,
                "2026-05-26T13:55:00",
                99
        );

        assertEquals("2026-05-26T10:00:00", withEndTime.getNewEndTime());
        assertEquals(5, withBidder.getHighestBidderId());
        assertEquals(3, withCount.getBidCount());
        assertEquals("2026-05-26T12:55:00", withTime.getBidTime());
        assertEquals(99, withId.getBidId());
        assertNull(withId.getPreviousHighestBidderId());
    }

    @Test
    void fullConstructorStoresAllFields() {
        BidResponse response = new BidResponse(
                true,
                "full",
                BigDecimal.valueOf(200),
                "2026-05-26T15:00:00",
                12,
                8,
                "2026-05-26T14:59:00",
                77,
                11
        );

        assertTrue(response.isSuccess());
        assertEquals("full", response.getMessage());
        assertEquals(BigDecimal.valueOf(200), response.getCurrentPrice());
        assertEquals("2026-05-26T15:00:00", response.getNewEndTime());
        assertEquals(12, response.getHighestBidderId());
        assertEquals(8, response.getBidCount());
        assertEquals("2026-05-26T14:59:00", response.getBidTime());
        assertEquals(77, response.getBidId());
        assertEquals(11, response.getPreviousHighestBidderId());
    }

    @Test
    void staticFactoriesCreateSuccessAndFailureResponses() {
        BidResponse success = BidResponse.success("accepted", BigDecimal.valueOf(300));
        BidResponse detailedSuccess = BidResponse.success(
                "accepted with count",
                BigDecimal.valueOf(350),
                "2026-05-26T16:00:00",
                20,
                9
        );
        BidResponse failure = BidResponse.failure("too low", BigDecimal.valueOf(250));

        assertTrue(success.isSuccess());
        assertEquals("accepted", success.getMessage());
        assertEquals(BigDecimal.valueOf(300), success.getCurrentPrice());

        assertTrue(detailedSuccess.isSuccess());
        assertEquals("2026-05-26T16:00:00", detailedSuccess.getNewEndTime());
        assertEquals(20, detailedSuccess.getHighestBidderId());
        assertEquals(9, detailedSuccess.getBidCount());

        assertFalse(failure.isSuccess());
        assertEquals("too low", failure.getMessage());
        assertEquals(BigDecimal.valueOf(250), failure.getCurrentPrice());
    }
}
