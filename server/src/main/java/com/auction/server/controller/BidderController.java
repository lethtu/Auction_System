package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.model.AuctionSession;
import com.auction.server.service.BidderService;
import com.auction.server.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    private SessionManager sessionManager;

    // API to get active auction sessions (paginated)
    @GetMapping("/active-sessions")
    public ApiResponse<Page<AuctionSession>> getActiveSessions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        logger.info("Fetching auction session list");
        Page<AuctionSession> activeSessions = bidderService.getActiveSessions(page, size);
        return new ApiResponse<>(200, "Active auction sessions retrieved successfully", activeSessions);
    }

    @GetMapping("/my-bidding-sessions")
    public ApiResponse<List<AuctionSession>> getMyBiddingSessions(@RequestParam Integer bidderId) {
        if (bidderId == null || bidderId <= 0) {
            return new ApiResponse<>(400, "Invalid bidderId", List.of());
        }

        List<AuctionSession> sessions = bidderService.getMyBiddingSessions(bidderId);
        return new ApiResponse<>(200, "Bidder's auction sessions retrieved successfully", sessions);
    }

    // API to deposit money
    @PostMapping("/deposit")
    public ResponseEntity<?> depositMoney(@RequestParam Integer bidderId, @RequestParam BigDecimal amount) {
        return bidderService.depositMoney(bidderId, amount)
                .map(balance -> ResponseEntity.ok("New balance: " + balance))
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
    public ApiResponse<List<SessionResponseDTO>> getMyBids(@RequestParam Integer bidderId) {
        logger.info("Fetching participated auction sessions for bidderId: {}", bidderId);
        List<SessionResponseDTO> dtos = bidderService.getMyBids(bidderId);
        return new ApiResponse<>(200, "Participated auction sessions retrieved successfully", dtos);
    }
}
