package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.mapper.SessionResponseMapper;
import com.auction.server.model.AuctionStatus;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/auctions")
public class PublicAuctionController {

    private final AuctionSessionRepository auctionSessionRepository;
    private final UserRepository userRepository;

    public PublicAuctionController(
            AuctionSessionRepository auctionSessionRepository,
            UserRepository userRepository
    ) {
        this.auctionSessionRepository = Objects.requireNonNull(
                auctionSessionRepository,
                "auctionSessionRepository must not be null"
        );
        this.userRepository = Objects.requireNonNull(
                userRepository,
                "userRepository must not be null"
        );
    }

    // Public endpoint allowing everyone to view auction sessions on the main screen.
    // Returns DTO instead of entity to avoid JPA relationship serialization errors and for stable client reading.
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<SessionResponseDTO>>> getAllSessions() {
        List<SessionResponseDTO> sessions = auctionSessionRepository
                .findVisiblePublicSessionsExcludingStatuses(List.of(AuctionStatus.DRAFT, AuctionStatus.CANCELED))
                .stream()
                .map(SessionResponseMapper::toDTO)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("All auction sessions retrieved successfully", sessions));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPublicStats() {
        long liveAuctions = auctionSessionRepository.countByStatus(AuctionStatus.ACTIVE);
        long activeBidders = userRepository.countAllByRole("USER");

        Map<String, Object> data = new HashMap<>();
        data.put("activeBidders", activeBidders > 0 ? activeBidders : 12000);
        data.put("liveAuctions", liveAuctions);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}
