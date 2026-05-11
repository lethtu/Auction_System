package com.auction.server.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class BidResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private BigDecimal currentPrice;

    public BidResponse(boolean success, String message, BigDecimal currentPrice) {
        this.success = success;
        this.message = message;
        this.currentPrice = currentPrice;
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
}