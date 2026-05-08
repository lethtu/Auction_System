package com.auction.server.dto;

import com.auction.server.model.AuctionSession;
import com.auction.server.model.Bidder;
import com.auction.server.model.User;

import java.io.Serializable;
import java.math.BigDecimal;

public class BidRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer auctionId;
    private Integer bidderId;
    private BigDecimal bidAmount;

    public BidRequest(Integer auctionId, Integer BidderId, BigDecimal bidAmount) {
        this.auctionId = auctionId;
        this.bidderId = BidderId;
        this.bidAmount = bidAmount;
    }

    public Integer getAuctionId() { return auctionId; }
    public Integer getBidderId() { return bidderId; }
    public BigDecimal getBidAmount() { return bidAmount; }


    @Override
    public String toString() {
        return "BidRequest{auctionId=" + auctionId + ", bidder=" + bidderId + ", amount=" + bidAmount + "}";
    }
}