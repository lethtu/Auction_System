package com.auction.client.util;

import com.auction.client.model.SessionItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellerStatsCalculatorTest {

    @Test
    void buildStatsText_nullSessions_returnsZeroTotal() {
        assertEquals("Total sessions: 0", SellerStatsCalculator.buildStatsText(null));
    }

    @Test
    void buildStatsText_emptySessions_returnsZeroTotal() {
        assertEquals("Total sessions: 0", SellerStatsCalculator.buildStatsText(List.of()));
    }

    @Test
    void buildStatsText_countsStatusesAndSumsEndedRevenue() {
        String result = SellerStatsCalculator.buildStatsText(List.of(
                session("PENDING", new BigDecimal("1000")),
                session(" active ", new BigDecimal("2000")),
                session("REJECTED", new BigDecimal("3000")),
                session("ENDED", new BigDecimal("4000")),
                session("ended", null),
                session("CANCELED", new BigDecimal("5000")),
                session("UNKNOWN", new BigDecimal("6000"))
        ));

        assertTrue(result.contains("Total sessions: 7"));
        assertTrue(result.contains("Pending sessions: 1"));
        assertTrue(result.contains("Active sessions: 1"));
        assertTrue(result.contains("Rejected sessions: 1"));
        assertTrue(result.contains("Ended sessions: 2"));
        assertTrue(result.contains("Canceled sessions: 1"));
        assertTrue(result.contains("Total revenue from ended sessions: 4,000"));
    }

    private SessionItem session(String status, BigDecimal currentPrice) {
        SessionItem item = new SessionItem();
        item.status = status;
        item.currentPrice = currentPrice;
        return item;
    }
}
