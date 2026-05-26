package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.dto.BidHistoryDTO;
import com.auction.server.dto.DeliveryInfoRequest;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Bid;
import com.auction.server.model.Electronics;
import com.auction.server.model.User;
import com.auction.server.repository.BidRepository;
import com.auction.server.service.AuctionService;
import com.auction.server.util.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    void constructor_rejectsMissingDependencies() {
        NullPointerException missingService = assertThrows(
                NullPointerException.class,
                () -> new AuctionController(null, bidRepository)
        );
        assertEquals("auctionService must not be null", missingService.getMessage());

        NullPointerException missingRepository = assertThrows(
                NullPointerException.class,
                () -> new AuctionController(auctionService, null)
        );
        assertEquals("bidRepository must not be null", missingRepository.getMessage());
    }

    @Test
    void check_returnsReadyResponse() {
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
        session.setId(7);
        when(auctionService.getActiveSessions()).thenReturn(List.of(session));

        ResponseEntity<List<AuctionSession>> response = controller.getActiveSessions();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
        assertEquals(7, response.getBody().get(0).getId());
        verify(auctionService).getActiveSessions();
    }

    @Test
    void getSessionDetail_returnsNotFoundWhenSessionMissing() {
        when(auctionService.getSessionById(99)).thenReturn(null);

        ResponseEntity<SessionResponseDTO> response = controller.getSessionDetail(99);

        assertEquals(404, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(bidRepository, never()).countBySessionId(anyInt());
    }

    @Test
    void getSessionDetail_mapsBidCountWinningBidAndBidList() {
        AuctionSession session = sampleSession(15);

        User winner = new User();
        winner.setId(44);

        Bid winningBid = new Bid();
        winningBid.setAmount(new BigDecimal("650000"));
        winningBid.setBidder(winner);

        Bid lowerBid = new Bid();
        lowerBid.setAmount(new BigDecimal("600000"));

        when(auctionService.getSessionById(15)).thenReturn(session);
        when(bidRepository.countBySessionId(15)).thenReturn(3);
        when(bidRepository.findWinningBidsForSessions(List.of(15))).thenReturn(List.of(winningBid));
        when(bidRepository.findBySessionIdOrderByAmountAsc(15)).thenReturn(List.of(lowerBid, winningBid));

        ResponseEntity<SessionResponseDTO> response = controller.getSessionDetail(15);

        assertEquals(200, response.getStatusCode().value());
        SessionResponseDTO dto = Objects.requireNonNull(response.getBody());
        assertEquals(15, dto.getId());
        assertEquals("Camera test", dto.getProductName());
        assertEquals("ACTIVE", dto.getStatus());
        assertEquals(3, dto.getBidCount());
        assertEquals(new BigDecimal("650000"), dto.getCurrentPrice());
        assertEquals(44, dto.getHighestBidderId());
        assertEquals(2, dto.getBids().size());
    }

    @Test
    void getBidHistory_returnsNotFoundWhenSessionMissing() {
        when(auctionService.getSessionById(77)).thenReturn(null);

        ResponseEntity<List<BidHistoryDTO>> response = controller.getBidHistory(77);

        assertEquals(404, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(auctionService, never()).getBidHistory(anyInt());
    }

    @Test
    void getBidHistory_returnsServiceHistoryWhenSessionExists() {
        AuctionSession session = sampleSession(20);
        BidHistoryDTO dto = new BidHistoryDTO(1, 20, 9, "bidder", new BigDecimal("120000"), "2026-05-26T12:00:00");

        when(auctionService.getSessionById(20)).thenReturn(session);
        when(auctionService.getBidHistory(20)).thenReturn(List.of(dto));

        ResponseEntity<List<BidHistoryDTO>> response = controller.getBidHistory(20);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
        assertEquals(new BigDecimal("120000"), response.getBody().get(0).getAmount());
    }

    @Test
    void saveDeliveryInfo_returnsSuccessWhenServiceAcceptsRequest() {
        DeliveryInfoRequest request = new DeliveryInfoRequest();
        SessionManager.SessionUser sessionUser = new SessionManager.SessionUser(9, "winner", "bidder");

        ResponseEntity<ApiResponse<String>> response = controller.saveDeliveryInfo(4, request, sessionUser);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getStatus());
        assertEquals("Delivery information saved successfully.", response.getBody().getMessage());
        assertEquals("SAVED", response.getBody().getData());
        verify(auctionService).saveDeliveryInfo(4, 9, request);
    }

    @Test
    void saveDeliveryInfo_returnsForbiddenForSecurityException() {
        DeliveryInfoRequest request = new DeliveryInfoRequest();
        SessionManager.SessionUser sessionUser = new SessionManager.SessionUser(9, "winner", "bidder");
        doThrow(new SecurityException("Only winner can update delivery info"))
                .when(auctionService).saveDeliveryInfo(4, 9, request);

        ResponseEntity<ApiResponse<String>> response = controller.saveDeliveryInfo(4, request, sessionUser);

        assertEquals(403, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Only winner can update delivery info", response.getBody().getMessage());
    }

    @Test
    void saveDeliveryInfo_returnsBadRequestForInvalidState() {
        DeliveryInfoRequest request = new DeliveryInfoRequest();
        SessionManager.SessionUser sessionUser = new SessionManager.SessionUser(9, "winner", "bidder");
        doThrow(new IllegalStateException("Auction is not ended"))
                .when(auctionService).saveDeliveryInfo(4, 9, request);

        ResponseEntity<ApiResponse<String>> response = controller.saveDeliveryInfo(4, request, sessionUser);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Auction is not ended", response.getBody().getMessage());
    }

    private AuctionSession sampleSession(Integer id) {
        AuctionSession session = new AuctionSession();
        session.setId(id);
        session.setStatus(AuctionStatus.ACTIVE);
        session.setStartingPrice(new BigDecimal("500000"));
        session.setCurrentPrice(new BigDecimal("600000"));
        session.setStepPrice(new BigDecimal("50000"));

        Electronics item = new Electronics();
        item.setId(100);
        item.setName("Camera test");
        item.setType("Electronics");
        item.setDescription("Camera for auction");
        session.setItem(item);

        return session;
    }
}
