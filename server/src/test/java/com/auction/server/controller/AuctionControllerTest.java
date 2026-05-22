package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.dto.BidHistoryDTO;
import com.auction.server.model.AuctionSession;
import com.auction.server.repository.BidRepository;
import com.auction.server.service.AuctionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionControllerTest {

    @Mock
    private AuctionService auctionService;

    @Mock
    private BidRepository bidRepository;

    private AuctionController controller;

    @BeforeEach
    void setUp() {
        controller = new AuctionController(auctionService, bidRepository);
    }

    @Test
    void check_returnsReadyMessage() {
        ResponseEntity<ApiResponse<String>> response = controller.check();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getStatus());
        assertEquals("Auction hall is ready!", response.getBody().getMessage());
        assertEquals("SUCCESS", response.getBody().getData());
    }

    @Test
    void getActiveSessions_returnsServiceResult() {
        AuctionSession session = new AuctionSession();
        session.setId(12);
        when(auctionService.getActiveSessions()).thenReturn(List.of(session));

        ResponseEntity<List<AuctionSession>> response = controller.getActiveSessions();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals(12, response.getBody().get(0).getId());
        verify(auctionService).getActiveSessions();
    }

    @Test
    void getSessionDetail_missingSession_returnsNotFound() {
        when(auctionService.getSessionById(99)).thenReturn(null);

        ResponseEntity<?> response = controller.getSessionDetail(99);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getBidHistory_missingSession_returnsNotFound() {
        when(auctionService.getSessionById(99)).thenReturn(null);

        ResponseEntity<List<BidHistoryDTO>> response = controller.getBidHistory(99);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getBidHistory_existingSession_returnsHistory() {
        AuctionSession session = new AuctionSession();
        session.setId(5);
        BidHistoryDTO dto = new BidHistoryDTO(1, 5, 20, "bidder", new BigDecimal("1000.00"), "2026-05-22T10:00:00");
        when(auctionService.getSessionById(5)).thenReturn(session);
        when(auctionService.getBidHistory(5)).thenReturn(List.of(dto));

        ResponseEntity<List<BidHistoryDTO>> response = controller.getBidHistory(5);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals(new BigDecimal("1000.00"), response.getBody().get(0).getAmount());
        verify(auctionService).getBidHistory(5);
    }
}