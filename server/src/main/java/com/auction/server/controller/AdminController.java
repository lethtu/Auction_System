package com.auction.server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.dto.UserResponseDTO;
import com.auction.server.service.AdminService;
import com.auction.server.view.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminService adminService;

    @GetMapping("/pending")
    public ApiResponse<List<SessionResponseDTO>> getPendingSessions() {
        try {
            logger.info("Đang lấy danh sách chờ duyệt");
            List<SessionResponseDTO> data = adminService.getPendingSessions();
            return new ApiResponse<>(200, "Lấy danh sách phiên chờ duyệt thành công", data);
        } catch (Exception e) {
            logger.error("Lỗi không mong muốn: {}", e.getMessage(), e);
            return new ApiResponse<>(500, e.getMessage(), null);
        }
    }

    @GetMapping("/sessions")
    public ApiResponse<List<SessionResponseDTO>> getAllSessions(
            @RequestParam(required = false) String status
    ) {
        try {
            logger.info("Đang lấy danh sách phiên");
            List<SessionResponseDTO> data = adminService.getAllSessions(status);
            return new ApiResponse<>(200, "Lấy danh sách phiên thành công", data);
        } catch (Exception e) {
            logger.error("Lỗi không mong muốn: {}", e.getMessage(), e);
            return new ApiResponse<>(500, e.getMessage(), null);
        }
    }

    @GetMapping("/session-detail/{sessionId}")
    public ApiResponse<SessionResponseDTO> getSessionDetail(@PathVariable Integer sessionId) {
        try {
            logger.info("Đang lấy chi tiết phiên: {}", sessionId);
            SessionResponseDTO data = adminService.getSessionDetail(sessionId);
            return new ApiResponse<>(200, "Lấy chi tiết phiên thành công", data);
        } catch (Exception e) {
            logger.error("Lỗi không mong muốn: {}", e.getMessage(), e);
            return new ApiResponse<>(500, e.getMessage(), null);
        }
    }

    @PostMapping("/approve/{sessionId}")
    public ApiResponse<String> approveSession(@PathVariable Integer sessionId, @RequestParam Integer adminId) {
        try {
            logger.info("Đang phê duyệt phiên: {}", sessionId);
            adminService.approveSession(sessionId, adminId);
            return new ApiResponse<>(200, "Phê duyệt thành công! Phiên đấu giá đã bắt đầu.", null);
        } catch (Exception e) {
            logger.error("Lỗi không mong muốn: {}", e.getMessage(), e);
            return new ApiResponse<>(500, e.getMessage(), null);
        }
    }

    @PostMapping("/reject/{sessionId}")
    public ApiResponse<String> rejectSession(
            @PathVariable Integer sessionId,
            @RequestParam Integer adminId,
            @RequestParam String reason
    ) {
        try {
            logger.info("Đang từ chối phiên đấu giá: {}", sessionId);
            adminService.rejectSession(sessionId, adminId, reason);
            return new ApiResponse<>(200, "Đã từ chối phiên đấu giá.", null);
        } catch (Exception e) {
            logger.error("Lỗi không mong muốn: {}", e.getMessage(), e);
            return new ApiResponse<>(500, e.getMessage(), null);
        }
    }

    public ApiResponse<List<UserResponseDTO>> getAllUsers(
            @RequestParam(required = false) String role
    ) {
        try {
            logger.info("Đang lấy danh sách người dùng");
            List<UserResponseDTO> data = adminService.getAllUsers(role);
            return new ApiResponse<>(200, "Lấy danh sách người dùng thành công", data);
        } catch (Exception e) {
            logger.error("Lỗi không mong muốn: {}", e.getMessage(), e);
            return new ApiResponse<>(500, e.getMessage(), null);
        }
    }
}