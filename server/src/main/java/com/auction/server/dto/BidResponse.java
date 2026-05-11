package com.auction.server.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class BidResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private BigDecimal currentPrice;
    private String newEndTime; // Thời gian gia hạn (nếu có)

    // CONSTRUCTOR 1: Dùng cho các bài Test cũ của Tùng (3 tham số)
    public BidResponse(boolean success, String message, BigDecimal currentPrice) {
        this.success = success;
        this.message = message;
        this.currentPrice = currentPrice;
        this.newEndTime = null;
    }

    // CONSTRUCTOR 2: Dùng cho thuật toán Anti-Sniping (4 tham số)
    public BidResponse(boolean success, String message, BigDecimal currentPrice, String newEndTime) {
        this.success = success;
        this.message = message;
        this.currentPrice = currentPrice;
        this.newEndTime = newEndTime;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public String getNewEndTime() { return newEndTime; }
}