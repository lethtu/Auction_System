package com.auction.server.controller;

import com.auction.server.dto.BidRequest;
import com.auction.server.dto.BidResponse;
import com.auction.server.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BiddingController {
    @Autowired
    private AuctionService auctionService;

    public void setAuctionService(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public synchronized BidResponse handleBid(BidRequest req) {
        BidResponse success = auctionService.updateBid(req.getAuctionId(), req.getBidderId(), req.getBidAmount());
        return success;
    }
}
