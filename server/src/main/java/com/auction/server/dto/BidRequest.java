package com.auction.server.dto;


import java.io.Serializable;
import java.math.BigDecimal;

public class BidRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer auctionId;
    private Integer bidderId;
    private BigDecimal bidAmount;
    private BigDecimal expectedPrice;

    public BidRequest(Integer auctionId, Integer BidderId, BigDecimal bidAmount) {
        this.auctionId = auctionId;
        this.bidderId = BidderId;
        this.bidAmount = bidAmount;
        this.expectedPrice = null;
    }

    public BidRequest(Integer auctionId, Integer BidderId, BigDecimal bidAmount, BigDecimal expectedPrice) {
        this.auctionId = auctionId;
        this.bidderId = BidderId;
        this.bidAmount = bidAmount;
        this.expectedPrice = expectedPrice;
    }

    public Integer getAuctionId() { return auctionId; }
    public Integer getBidderId() { return bidderId; }
    public BigDecimal getBidAmount() { return bidAmount; }
    public BigDecimal getExpectedPrice() { return expectedPrice; }


    @Override
    public String toString() {
        return "BidRequest{auctionId=" + auctionId + ", bidder=" + bidderId + ", amount=" + bidAmount + ", expectedPrice=" + expectedPrice + "}";
    }
}