package com.auction.client.util;

import com.auction.client.common.AuctionStatus;
import com.auction.client.model.SessionItem;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

public final class SellerStatsCalculator {
    private static final String TOTAL_SESSIONS_LABEL = "Total sessions: ";
    private static final String PENDING_SESSIONS_LABEL = "Pending sessions: ";
    private static final String ACTIVE_SESSIONS_LABEL = "Active sessions: ";
    private static final String REJECTED_SESSIONS_LABEL = "Rejected sessions: ";
    private static final String ENDED_SESSIONS_LABEL = "Ended sessions: ";
    private static final String CANCELED_SESSIONS_LABEL = "Canceled sessions: ";
    private static final String TOTAL_REVENUE_LABEL = "Total revenue from ended sessions: ";

    private static final String MONEY_PATTERN = "#,##0.##";

    private SellerStatsCalculator() {
    }

    public static String buildStatsText(List<SessionItem> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return TOTAL_SESSIONS_LABEL + 0;
        }

        SellerStats stats = calculateStats(sessions);
        return formatStats(stats);
    }

    private static SellerStats calculateStats(List<SessionItem> sessions) {
        SellerStats stats = new SellerStats(sessions.size());

        for (SessionItem session : sessions) {
            addSessionToStats(stats, session);
        }

        return stats;
    }

    private static void addSessionToStats(SellerStats stats, SessionItem session) {
        String status = normalizeStatus(session.status);

        switch (status) {
            case AuctionStatus.PENDING -> stats.pending++;
            case AuctionStatus.ACTIVE -> stats.active++;
            case AuctionStatus.REJECTED -> stats.rejected++;
            case AuctionStatus.ENDED -> {
                stats.ended++;
                stats.revenue = stats.revenue.add(safePrice(session.currentPrice));
            }
            case AuctionStatus.CANCELED -> stats.canceled++;
            default -> {
            }
        }
    }

    private static String formatStats(SellerStats stats) {
        return TOTAL_SESSIONS_LABEL + stats.total + "\n"
                + PENDING_SESSIONS_LABEL + stats.pending + "\n"
                + ACTIVE_SESSIONS_LABEL + stats.active + "\n"
                + REJECTED_SESSIONS_LABEL + stats.rejected + "\n"
                + ENDED_SESSIONS_LABEL + stats.ended + "\n"
                + CANCELED_SESSIONS_LABEL + stats.canceled + "\n"
                + TOTAL_REVENUE_LABEL + moneyFormatter().format(stats.revenue);
    }

    private static DecimalFormat moneyFormatter() {
        return new DecimalFormat(MONEY_PATTERN);
    }

    private static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }

    private static BigDecimal safePrice(BigDecimal price) {
        return price == null ? BigDecimal.ZERO : price;
    }

    private static final class SellerStats {
        private final int total;
        private int pending;
        private int active;
        private int rejected;
        private int ended;
        private int canceled;
        private BigDecimal revenue = BigDecimal.ZERO;

        private SellerStats(int total) {
            this.total = total;
        }
    }
}