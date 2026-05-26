package com.auction.server.controller;

import com.auction.server.dto.BidRequest;
import com.auction.server.dto.BidResponse;
import com.auction.server.service.AuctionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiddingControllerTest {

    @Test
    void setAndGetAuctionService_returnsInjectedService() {
        BiddingController controller = new BiddingController();
        AuctionService auctionService = Mockito.mock(AuctionService.class);

        controller.setAuctionService(auctionService);

        assertSame(auctionService, controller.getAuctionService());
    }

    @Test
    void handleBid_delegatesToAuctionServiceUpdateBid() {
        BiddingController controller = new BiddingController();
        AuctionService auctionService = Mockito.mock(AuctionService.class);
        controller.setAuctionService(auctionService);

        BidRequest request = new BidRequest(7, 15, new BigDecimal("120000"));
        BidResponse expected = new BidResponse(true, "Bid accepted", new BigDecimal("120000"));

        when(auctionService.updateBid(7, 15, new BigDecimal("120000"))).thenReturn(expected);

        BidResponse actual = controller.handleBid(request);

        assertSame(expected, actual);
        verify(auctionService).updateBid(7, 15, new BigDecimal("120000"));
    }

    @Test
    void registerAutoBid_delegatesToAuctionService() throws Exception {
        BiddingController controller = new BiddingController();
        AuctionService auctionService = Mockito.mock(AuctionService.class);
        controller.setAuctionService(auctionService);

        controller.registerAutoBid(9, 21, new BigDecimal("500000"), new BigDecimal("10000"));

        verify(auctionService).registerAutoBid(9, 21, new BigDecimal("500000"), new BigDecimal("10000"));
    }

    @Test
    void resolveAutoBids_delegatesToAuctionService() {
        BiddingController controller = new BiddingController();
        AuctionService auctionService = Mockito.mock(AuctionService.class);
        controller.setAuctionService(auctionService);

        BidResponse expected = new BidResponse(true, "Auto bid resolved", new BigDecimal("250000"));
        when(auctionService.resolveAutoBids(11)).thenReturn(expected);

        BidResponse actual = controller.resolveAutoBids(11);

        assertSame(expected, actual);
        assertEquals("Auto bid resolved", actual.getMessage());
        verify(auctionService).resolveAutoBids(11);
    }
}