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
        assertEquals("Tổng số phiên: 0", SellerStatsCalculator.buildStatsText(null));
    }

    @Test
    void buildStatsText_emptySessions_returnsZeroTotal() {
        assertEquals("Tổng số phiên: 0", SellerStatsCalculator.buildStatsText(List.of()));
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

        assertTrue(result.contains("Tổng số phiên: 7"));
        assertTrue(result.contains("Số phiên chờ duyệt: 1"));
        assertTrue(result.contains("Số phiên đang hoạt động: 1"));
        assertTrue(result.contains("Số phiên bị từ chối: 1"));
        assertTrue(result.contains("Số phiên đã kết thúc: 2"));
        assertTrue(result.contains("Số phiên đã hủy: 1"));
        assertTrue(result.contains("Tổng doanh thu phiên đã kết thúc: 4,000"));
    }

    private SessionItem session(String status, BigDecimal currentPrice) {
        SessionItem item = new SessionItem();
        item.status = status;
        item.currentPrice = currentPrice;
        return item;
    }
}
