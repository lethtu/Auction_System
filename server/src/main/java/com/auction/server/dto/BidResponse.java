package com.auction.server.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class BidResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String message;
    private final BigDecimal currentPrice;
    private final String newEndTime;
    private final Integer highestBidderId;
    private final Integer bidCount;

    public BidResponse(boolean success, String message, BigDecimal currentPrice) {
        this(success, message, currentPrice, null, null, null);
    }

    public BidResponse(boolean success, String message, BigDecimal currentPrice, String newEndTime) {
        this(success, message, currentPrice, newEndTime, null, null);
    }

    public BidResponse(
            boolean success,
            String message,
            BigDecimal currentPrice,
            String newEndTime,
            Integer highestBidderId
    ) {
        this(success, message, currentPrice, newEndTime, highestBidderId, null);
    }

    public BidResponse(
            boolean success,
            String message,
            BigDecimal currentPrice,
            String newEndTime,
            Integer highestBidderId,
            Integer bidCount
    ) {
        this.success = success;
        this.message = message;
        this.currentPrice = currentPrice;
        this.newEndTime = newEndTime;
        this.highestBidderId = highestBidderId;
        this.bidCount = bidCount;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public String getNewEndTime() {
        return newEndTime;
    }

    public Integer getHighestBidderId() {
        return highestBidderId;
    }

    public Integer getBidCount() {
        return bidCount;
    }
}
