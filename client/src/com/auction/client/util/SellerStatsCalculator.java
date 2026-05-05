package com.auction.client.util;

import com.auction.client.common.AuctionStatus;
import com.auction.client.model.SessionItem;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

public class SellerStatsCalculator {
    public static String buildStatsText(List<SessionItem> sessions) {
        int pending = 0;
        int active = 0;
        int rejected = 0;
        int completed = 0;
        int canceled = 0;
        BigDecimal revenue = BigDecimal.ZERO;

        for (SessionItem session : sessions) {
            if (session.status == null) {
                continue;
            }

            switch (session.status.toUpperCase()) {
                case AuctionStatus.PENDING -> pending++;
                case AuctionStatus.ACTIVE -> active++;
                case AuctionStatus.REJECTED -> rejected++;
                case AuctionStatus.COMPLETED -> {
                    completed++;
                    if (session.currentPrice != null) {
                        revenue = revenue.add(session.currentPrice);
                    }
                }
                case AuctionStatus.CANCELED -> canceled++;
            }
        }

        DecimalFormat df = new DecimalFormat("#,##0.##");

        return "Tổng số phiên: " + sessions.size() + "\n" +
                "Số phiên chờ duyệt: " + pending + "\n" +
                "Số phiên đang hoạt động: " + active + "\n" +
                "Số phiên bị từ chối: " + rejected + "\n" +
                "Số phiên đã hoàn thành: " + completed + "\n" +
                "Số phiên đã hủy: " + canceled + "\n" +
                "Tổng doanh thu phiên hoàn thành: " + df.format(revenue);
    }
}