package com.auction.client.model;

import java.math.BigDecimal;

/**
 * Represents a single bid data point for chart/history visualization.
 * Used as the canonical data model for both Mini Chart and Full History Popup.
 */
public class BidChartPoint {

    private final int bidId;
    private final BigDecimal amount;
    private final String bidTime;       // ISO-8601
    private final long epochMillis;
    private final int bidderId;
    private final String maskedBidderCode; // "#A7B2"
    private final boolean mine;           // true if bidderId == current user
    private String relativeTime;          // "2s ago", "1h ago" — mutable, updated on render

    public BidChartPoint(int bidId, BigDecimal amount, String bidTime, long epochMillis,
                         int bidderId, String maskedBidderCode, boolean mine) {
        this.bidId = bidId;
        this.amount = amount;
        this.bidTime = bidTime;
        this.epochMillis = epochMillis;
        this.bidderId = bidderId;
        this.maskedBidderCode = maskedBidderCode;
        this.mine = mine;
        this.relativeTime = "";
    }

    public int getBidId() { return bidId; }
    public BigDecimal getAmount() { return amount; }
    public String getBidTime() { return bidTime; }
    public long getEpochMillis() { return epochMillis; }
    public int getBidderId() { return bidderId; }
    public String getMaskedBidderCode() { return maskedBidderCode; }
    public boolean isMine() { return mine; }

    public String getRelativeTime() { return relativeTime; }
    public void setRelativeTime(String relativeTime) { this.relativeTime = relativeTime; }

    /**
     * Display name cho UI: "You" nếu mine, ngược lại maskedBidderCode.
     */
    public String getDisplayName() {
        return mine ? "You" : maskedBidderCode;
    }
}
