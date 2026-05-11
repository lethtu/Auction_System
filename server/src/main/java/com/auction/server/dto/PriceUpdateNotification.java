package com.auction.server.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class PriceUpdateNotification implements Serializable {
    private Integer auctionSessionId;
    private BigDecimal newPrice;
    private String message;

    public PriceUpdateNotification(Integer auctionSessionId, BigDecimal newPrice, String message) {
        this.auctionSessionId = auctionSessionId;
        this.newPrice = newPrice;
        this.message = message;
    }

    // Getters và Setters
    public Integer getAuctionSessionId() { return auctionSessionId; }
    public BigDecimal getNewPrice() { return newPrice; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return "THÔNG BÁO CHUNG: Phiên " + auctionSessionId + " vừa có giá mới: " + newPrice;
    }
}