package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.dto.BidHistoryDTO;
import com.auction.server.dto.DeliveryInfoRequest;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.mapper.SessionResponseMapper;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.Bid;
import com.auction.server.repository.BidRepository;
import com.auction.server.service.AuctionService;
import com.auction.server.util.SessionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/auctions")
public class AuctionController {
    private static final String AUCTION_READY_MESSAGE = "Auction hall is ready!";
    private static final String SUCCESS_STATUS = "SUCCESS";

    private final AuctionService auctionService;
    private final BidRepository bidRepository;

    public AuctionController(AuctionService auctionService, BidRepository bidRepository) {
        this.auctionService = Objects.requireNonNull(auctionService, "auctionService must not be null");
        this.bidRepository = Objects.requireNonNull(bidRepository, "bidRepository must not be null");
    }

    @GetMapping("/hello")
    public ResponseEntity<ApiResponse<String>> check() {
        ApiResponse<String> response = new ApiResponse<>(200, AUCTION_READY_MESSAGE, SUCCESS_STATUS);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<AuctionSession>> getActiveSessions() {
        List<AuctionSession> activeSessions = auctionService.getActiveSessions();
        return ResponseEntity.ok(activeSessions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponseDTO> getSessionDetail(@PathVariable Integer id) {
        AuctionSession session = auctionService.getSessionById(id);

        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toSessionResponseDTO(session, id));
    }

    /**
     * Get bid history of an auction session, sorted by time ascending.
     * Used for Bid History Chart on client.
     * Returns 404 if session does not exist (consistent with GET /api/auctions/{id}).
     */
    @GetMapping("/{id}/bid-history")
    public ResponseEntity<List<BidHistoryDTO>> getBidHistory(@PathVariable Integer id) {
        AuctionSession session = auctionService.getSessionById(id);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        List<BidHistoryDTO> history = auctionService.getBidHistory(id);
        return ResponseEntity.ok(history);
    }

    @PutMapping("/{id}/delivery")
    public ResponseEntity<ApiResponse<String>> saveDeliveryInfo(
            @PathVariable Integer id,
            @RequestBody DeliveryInfoRequest request,
            @RequestAttribute("sessionUser") SessionManager.SessionUser sessionUser) {
        try {
            auctionService.saveDeliveryInfo(id, sessionUser.getUserId(), request);
            return ResponseEntity.ok(ApiResponse.success("Delivery information saved successfully.", "SAVED"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    private SessionResponseDTO toSessionResponseDTO(AuctionSession session, Integer sessionId) {
        int bidCount = Math.toIntExact(bidRepository.countBySessionId(sessionId));
        SessionResponseDTO dto = SessionResponseMapper.toDTO(session, bidCount);
        List<Bid> winningBids = bidRepository.findWinningBidsForSessions(List.of(sessionId));
        if (!winningBids.isEmpty() && winningBids.get(0).getBidder() != null) {
            Bid winningBid = winningBids.get(0);
            dto.setCurrentPrice(winningBid.getAmount());
            dto.setHighestBidderId(winningBid.getBidder().getId());
        }
        dto.setBids(bidRepository.findBySessionIdOrderByAmountAsc(sessionId));
        return dto;
    }
}
