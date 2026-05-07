package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.dto.UserResponseDTO;
import com.auction.server.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/pending")
    public ApiResponse<List<SessionResponseDTO>> getPendingSessions() {
        return handleRequest(
                "Đang lấy danh sách chờ duyệt",
                "Lấy danh sách phiên chờ duyệt thành công",
                adminService::getPendingSessions
        );
    }

    @GetMapping("/sessions")
    public ApiResponse<List<SessionResponseDTO>> getAllSessions(
            @RequestParam(required = false) String status
    ) {
        return handleRequest(
                "Đang lấy danh sách phiên",
                "Lấy danh sách phiên thành công",
                () -> adminService.getAllSessions(status)
        );
    }

    @GetMapping("/session-detail/{sessionId}")
    public ApiResponse<SessionResponseDTO> getSessionDetail(@PathVariable Integer sessionId) {
        return handleRequest(
                "Đang lấy chi tiết phiên " + sessionId,
                "Lấy chi tiết phiên thành công",
                () -> adminService.getSessionDetail(sessionId)
        );
    }

    @PostMapping("/approve/{sessionId}")
    public ApiResponse<Void> approveSession(
            @PathVariable Integer sessionId,
            @RequestParam Integer adminId
    ) {
        return handleRequest(
                "Đang phê duyệt phiên " + sessionId,
                "Phê duyệt thành công! Phiên đấu giá đã bắt đầu.",
                () -> {
                    adminService.approveSession(sessionId, adminId);
                    return null;
                }
        );
    }

    @PostMapping("/reject/{sessionId}")
    public ApiResponse<Void> rejectSession(
            @PathVariable Integer sessionId,
            @RequestParam Integer adminId,
            @RequestParam String reason
    ) {
        return handleRequest(
                "Đang từ chối phiên đấu giá " + sessionId,
                "Đã từ chối phiên đấu giá.",
                () -> {
                    adminService.rejectSession(sessionId, adminId, reason);
                    return null;
                }
        );
    }


    @PostMapping("/ban-user/{userId}")
    public ApiResponse<Void> banUser(
            @PathVariable Integer userId,
            @RequestParam Integer adminId
    ) {
        return handleRequest(
                "Đang khóa tài khoản user " + userId,
                "Đã khóa tài khoản user.",
                () -> {
                    adminService.banUser(userId, adminId);
                    return null;
                }
        );
    }

    @PostMapping("/cancel-auction/{sessionId}")
    public ApiResponse<Void> cancelAuction(
            @PathVariable Integer sessionId,
            @RequestParam Integer adminId
    ) {
        return handleRequest(
                "Đang hủy phiên đấu giá " + sessionId,
                "Đã hủy phiên đấu giá.",
                () -> {
                    adminService.cancelAuction(sessionId, adminId);
                    return null;
                }
        );
    }

    @GetMapping("/users")
    public ApiResponse<List<UserResponseDTO>> getAllUsers(
            @RequestParam(required = false) String role
    ) {
        return handleRequest(
                "Đang lấy danh sách người dùng",
                "Lấy danh sách người dùng thành công",
                () -> adminService.getAllUsers(role)
        );
    }

    private <T> ApiResponse<T> handleRequest(
            String logMessage,
            String successMessage,
            Supplier<T> action
    ) {
        try {
            logger.info(logMessage);
            return ApiResponse.success(successMessage, action.get());

        } catch (IllegalArgumentException e) {
            logger.warn("{} thất bại: {}", logMessage, e.getMessage());
            return ApiResponse.error(400, e.getMessage());

        } catch (Exception e) {
            logger.error("{} thất bại: {}", logMessage, e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }
}