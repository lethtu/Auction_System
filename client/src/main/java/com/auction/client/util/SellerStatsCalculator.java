package com.auction.client.util;

import com.auction.client.common.AuctionStatus;
import com.auction.client.model.SessionItem;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

public final class SellerStatsCalculator {
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.##");

    private SellerStatsCalculator() {
    }

    public static String buildStatsText(List<SessionItem> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return "Tổng số phiên: 0";
        }

        int pending = 0;
        int active = 0;
        int rejected = 0;
        int ended = 0;
        int canceled = 0;
        BigDecimal revenue = BigDecimal.ZERO;

        for (SessionItem session : sessions) {
            String status = normalizeStatus(session.status);

            switch (status) {
                case AuctionStatus.PENDING -> pending++;
                case AuctionStatus.ACTIVE -> active++;
                case AuctionStatus.REJECTED -> rejected++;
                case AuctionStatus.ENDED -> {
                    ended++;
                    revenue = revenue.add(safePrice(session.currentPrice));
                }
                case AuctionStatus.CANCELED -> canceled++;
            }
        }

        return "Tổng số phiên: " + sessions.size() + "\n"
                + "Số phiên chờ duyệt: " + pending + "\n"
                + "Số phiên đang hoạt động: " + active + "\n"
                + "Số phiên bị từ chối: " + rejected + "\n"
                + "Số phiên đã kết thúc: " + ended + "\n"
                + "Số phiên đã hủy: " + canceled + "\n"
                + "Tổng doanh thu phiên đã kết thúc: " + MONEY_FORMAT.format(revenue);
    }

    private static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }

    private static BigDecimal safePrice(BigDecimal price) {
        return price == null ? BigDecimal.ZERO : price;
    }
}