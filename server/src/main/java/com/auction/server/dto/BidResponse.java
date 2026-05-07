package com.auction.server.dto;

import java.io.Serializable;

public class BidResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private double currentPrice;

    public BidResponse(boolean success, String message, double currentPrice) {
        this.success = success;
        this.message = message;
        this.currentPrice = currentPrice;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public double getCurrentPrice() { return currentPrice; }
}