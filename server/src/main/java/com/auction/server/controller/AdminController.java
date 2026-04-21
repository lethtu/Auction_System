package com.auction.server.controller;

import com.auction.server.model.AuctionSession;
import com.auction.server.model.User;
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

    @GetMapping("/sessions")
    public ResponseEntity<List<AuctionSession>> getAllSessions(
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(adminService.getAllSessions(status));
    }

    @GetMapping("/session-detail/{sessionId}")
    public ResponseEntity<?> getSessionDetail(@PathVariable Integer sessionId) {
        try {
            return ResponseEntity.ok(adminService.getSessionDetail(sessionId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/approve/{sessionId}")
    public ResponseEntity<?> approveSession(@PathVariable Integer sessionId, @RequestParam Integer adminId) {
        try {
            adminService.approveSession(sessionId, adminId);
            return ResponseEntity.ok("Phê duyệt thành công! Phiên đấu giá đã bắt đầu.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reject/{sessionId}")
    public ResponseEntity<?> rejectSession(
            @PathVariable Integer sessionId,
            @RequestParam Integer adminId,
            @RequestParam String reason
    ) {
        try {
            adminService.rejectSession(sessionId, adminId, reason);
            return ResponseEntity.ok("Đã từ chối phiên đấu giá.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }
}