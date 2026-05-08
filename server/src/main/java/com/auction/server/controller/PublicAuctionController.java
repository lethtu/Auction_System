package com.auction.server.controller;

import com.auction.server.model.AuctionSession;
import com.auction.server.repository.AuctionSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auctions")
public class PublicAuctionController {

    @Autowired
    private AuctionSessionRepository auctionSessionRepository;

    // Endpoint public cho phép mọi người xem toàn bộ danh sách
    @GetMapping("/all")
    public ResponseEntity<?> getAllSessions() {
        List<AuctionSession> sessions = auctionSessionRepository.findAll();

        // Đóng gói data theo chuẩn JSON mà Client đang chờ (có status và data)
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Lấy toàn bộ phiên đấu giá thành công");
        response.put("data", sessions);

        return ResponseEntity.ok(response);
    }
}