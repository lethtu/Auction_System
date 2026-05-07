package com.auction.server.dto;

import java.io.Serializable;

public class BidRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int auctionId;
    private int userId;
    private double bidAmount;

    public BidRequest(int auctionId, int userId, double bidAmount) {
        this.auctionId = auctionId;
        this.userId = userId;
        this.bidAmount = bidAmount;
    }

    public int getAuctionId() { return auctionId; }
    public int getUserId() { return userId; }
    public double getBidAmount() { return bidAmount; }

    @Override
    public String toString() {
        return "BidRequest{auctionId=" + auctionId + ", userId=" + userId + ", amount=" + bidAmount + "}";
    }
}