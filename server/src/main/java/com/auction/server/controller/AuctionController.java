package com.auction.server.controller;

import com.auction.server.model.AuctionSession;
import com.auction.server.service.AuctionService;
import com.auction.server.service.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auctions")
public class AuctionController {

    @Autowired
    private AuctionService auctionService;

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
    public ResponseEntity<AuctionSession> getSessionDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(auctionService.getSessionById(id));
    }
}