package com.auction.server.controller;

import com.auction.server.dto.BidRequest;
import com.auction.server.dto.BidResponse;
import com.auction.server.service.AuctionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BiddingControllerTest {

    @Mock
    private AuctionService auctionService;

    private BiddingController controller;

    @BeforeEach
    void setUp() {
        controller = new BiddingController();
        controller.setAuctionService(auctionService);
    }

    @Test
    void handleBid_delegatesToAuctionService() {
        BigDecimal amount = new BigDecimal("1500.00");
        BidRequest request = new BidRequest(1, 7, amount);
        BidResponse expected = BidResponse.success("OK", amount);
        when(auctionService.updateBid(1, 7, amount)).thenReturn(expected);

        BidResponse result = controller.handleBid(request);

        assertSame(expected, result);
        verify(auctionService).updateBid(1, 7, amount);
    }

    @Test
    void registerAutoBid_delegatesToAuctionService() throws Exception {
        BigDecimal maxBid = new BigDecimal("5000.00");
        BigDecimal increment = new BigDecimal("100.00");

        controller.registerAutoBid(2, 8, maxBid, increment);

        verify(auctionService).registerAutoBid(2, 8, maxBid, increment);
    }

    @Test
    void resolveAutoBids_delegatesToAuctionService() {
        BidResponse expected = BidResponse.success("Resolved", new BigDecimal("2000.00"));
        when(auctionService.resolveAutoBids(3)).thenReturn(expected);

        BidResponse result = controller.resolveAutoBids(3);

        assertSame(expected, result);
        verify(auctionService).resolveAutoBids(3);
    }
}