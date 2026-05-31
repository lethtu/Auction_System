package com.auction.server.controller;

import com.auction.server.dto.BidRequest;
import com.auction.server.dto.BidResponse;
import com.auction.server.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BiddingController {
    @Autowired
    private AuctionService auctionService;

    public AuctionService getAuctionService() {
        return this.auctionService;
    }

    public void setAuctionService(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public BidResponse handleBid(BidRequest req) {
        BidResponse success = auctionService.updateBid(req.getAuctionId(), req.getBidderId(), req.getBidAmount(), req.getExpectedPrice());
        return success;
    }

    public void registerAutoBid(int auctionId, int bidderId, BigDecimal maxBid, BigDecimal increment) throws Exception {
        auctionService.registerAutoBid(auctionId, bidderId, maxBid, increment);
    }

    public BidResponse resolveAutoBids(Integer sessionId) {
        return auctionService.resolveAutoBids(sessionId);
    }
}
