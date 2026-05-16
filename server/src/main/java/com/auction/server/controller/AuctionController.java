package com.auction.server.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.mapper.SessionResponseMapper;
import com.auction.server.model.AuctionSession;
import com.auction.server.repository.BidRepository;
import com.auction.server.service.AuctionService;
import com.auction.server.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auctions")
public class AuctionController {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private BidRepository bidRepository;

    @GetMapping("/hello")
    public ResponseEntity<ApiResponse> check() {
        ApiResponse item = new ApiResponse(200, "Sảnh đấu giá đã sẵn sàng!", "SUCCESS");
        return ResponseEntity.ok(item);
    }

    @GetMapping("/active")
    public ResponseEntity<List<AuctionSession>> getActiveSessions() {
        return ResponseEntity.ok(auctionService.getActiveSessions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponseDTO> getSessionDetail(@PathVariable Integer id) {
        AuctionSession session = auctionService.getSessionById(id);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        int bidCount = Math.toIntExact(bidRepository.countBySessionId(id));
        return ResponseEntity.ok(SessionResponseMapper.toDTO(session, bidCount));
    }
}