package com.auction.server.dto;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

public final class BidResponse implements Serializable {
    @Serial
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

    public static BidResponse success(String message, BigDecimal currentPrice) {
        return new BidResponse(true, message, currentPrice);
    }

    public static BidResponse success(
            String message,
            BigDecimal currentPrice,
            String newEndTime,
            Integer highestBidderId,
            Integer bidCount
    ) {
        return new BidResponse(true, message, currentPrice, newEndTime, highestBidderId, bidCount);
    }

    public static BidResponse failure(String message, BigDecimal currentPrice) {
        return new BidResponse(false, message, currentPrice);
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