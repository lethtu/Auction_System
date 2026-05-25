package com.auction.server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.dto.ApiResponse;
import com.auction.server.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.auction.server.service.BidderService;
import com.auction.server.util.SessionManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bidder")
public class BidderController {
    private static final Logger logger = LoggerFactory.getLogger(BidderController.class);
    @Autowired
    private BidderService bidderService;

    @Autowired
    private AuctionSessionRepository auctionSessionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionManager sessionManager;

    // API to get active auction sessions (paginated)
    @GetMapping("/active-sessions")
    public ApiResponse<Page<AuctionSession>> getActiveSessions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        logger.info("Fetching auction session list");
        // Sort by newest first (startTime descending)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));

        // Call paginated query from Repository with ACTIVE status
        Page<AuctionSession> activeSessions = auctionSessionRepository.findByStatus(AuctionStatus.ACTIVE, pageable);

        // Wrap data in standard ApiResponse format
        return new ApiResponse<>(200, "Active auction sessions retrieved successfully", activeSessions);
    }


    @GetMapping("/my-bidding-sessions")
    public ApiResponse<List<AuctionSession>> getMyBiddingSessions(@RequestParam Integer bidderId) {
        if (bidderId == null || bidderId <= 0) {
            return new ApiResponse<>(400, "Invalid bidderId", List.of());
        }

        List<AuctionSession> sessions = bidRepository.findDistinctSessionsByBidderId(bidderId);
        return new ApiResponse<>(200, "Bidder's auction sessions retrieved successfully", sessions);
    }

    // API to deposit money
    @PostMapping("/deposit")
    public ResponseEntity<?> depositMoney(@RequestParam Integer bidderId, @RequestParam BigDecimal amount) {
        return userRepository.findById(bidderId)
                .map(user -> {
                    user.setBalance(user.getBalance().add(amount));
                    userRepository.save(user);
                    return ResponseEntity.ok("New balance: " + user.getBalance());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/up-to-seller")
    public ApiResponse<String> upToSeller(@RequestParam Integer userId) {
        Map<String, Object> result = bidderService.upToSeller(userId);
        boolean isSuccess = (boolean) result.get("success");
        String message = (String) result.get("message");
        if (isSuccess) {
            sessionManager.updateRoleByUserId(userId, "seller");
            return new ApiResponse<>(200, message, "SUCCESS");
        } else {
            return new ApiResponse<>(400, message, "FAILED");
        }
    }

    @GetMapping("/my-bids")
    public ApiResponse<java.util.List<com.auction.server.dto.SessionResponseDTO>> getMyBids(@RequestParam Integer bidderId) {
        logger.info("Fetching participated auction sessions for bidderId: {}", bidderId);
        java.util.List<AuctionSession> sessions = bidRepository.findSessionsByBidderId(bidderId);
        java.util.List<com.auction.server.dto.SessionResponseDTO> dtos = sessions.stream()
                .map(session -> {
                    int bidCount = Math.toIntExact(bidRepository.countBySessionId(session.getId()));
                    com.auction.server.dto.SessionResponseDTO dto = com.auction.server.mapper.SessionResponseMapper.toDTO(session, bidCount);
                    java.math.BigDecimal maxBid = bidRepository.findMaxBidAmountBySessionIdAndBidderId(session.getId(), bidderId);
                    dto.setUserMaxBid(maxBid);
                    if (bidderId.equals(session.getHighestBidderId())) {
                        dto.setDeliveryRecipient(session.getDeliveryRecipient());
                        dto.setDeliveryPhone(session.getDeliveryPhone());
                        dto.setDeliveryAddress(session.getDeliveryAddress());
                        dto.setDeliveryNote(session.getDeliveryNote());
                        dto.setDeliverySubmittedAt(session.getDeliverySubmittedAt());
                    }
                    return dto;
                })
                .toList();
        return new ApiResponse<>(200, "Participated auction sessions retrieved successfully", dtos);
    }
}
