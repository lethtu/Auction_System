package com.auction.server.controller;

import com.auction.server.dto.AuctionRequestDTO;
import com.auction.server.dto.SellerStatsDTO;
import com.auction.server.model.AuctionSession;
import com.auction.server.service.SellerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller")
public class SellerController {

    @Autowired
    private SellerService sellerService;

    @PostMapping("/create")
    public ResponseEntity<?> createAuction(@Valid @RequestBody AuctionRequestDTO dto) {
        try {
            AuctionSession session = sellerService.createAuction(dto);
            return ResponseEntity.ok("Đã gửi yêu cầu đấu giá cho món: " + session.getProduct().getName());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my-sessions/{sellerId}")
    public ResponseEntity<List<AuctionSession>> viewMySessions(@PathVariable Integer sellerId) {
        return ResponseEntity.ok(sellerService.getMySessions(sellerId));
    }

    @DeleteMapping("/cancel-session/{sessionId}")
    public ResponseEntity<?> cancelAuction(@PathVariable Integer sessionId, @RequestParam Integer sellerId) {
        try {
            sellerService.cancelSession(sessionId, sellerId);
            return ResponseEntity.ok("Đã hủy phiên thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/stats/{sellerId}")
    public ResponseEntity<SellerStatsDTO> getStats(@PathVariable Integer sellerId) {
        return ResponseEntity.ok(sellerService.getSellerStats(sellerId));
    }
}