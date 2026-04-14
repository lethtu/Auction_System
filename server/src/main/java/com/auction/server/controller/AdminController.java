package com.auction.server.controller;

import com.auction.server.model.AuctionSession;
import com.auction.server.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/pending")
    public ResponseEntity<List<AuctionSession>> getPendingSessions() {
        return ResponseEntity.ok(adminService.getPendingSessions());
    }

    @PostMapping("/approve/{sessionId}")
    public ResponseEntity<?> approveSession(@PathVariable Integer sessionId) {
        try {
            adminService.approveSession(sessionId);
            return ResponseEntity.ok("Phê duyệt thành công! Phiên đấu giá đã bắt đầu.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reject/{sessionId}")
    public ResponseEntity<?> rejectSession(@PathVariable Integer sessionId) {
        try {
            adminService.rejectSession(sessionId);
            return ResponseEntity.ok("Đã từ chối phiên đấu giá.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}