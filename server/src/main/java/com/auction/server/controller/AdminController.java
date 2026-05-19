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

    private static final String LOG_GET_PENDING_SESSIONS = "Đang lấy danh sách chờ duyệt";
    private static final String LOG_GET_ALL_SESSIONS = "Đang lấy danh sách phiên";
    private static final String LOG_GET_SESSION_DETAIL = "Đang lấy chi tiết phiên ";
    private static final String LOG_APPROVE_SESSION = "Đang phê duyệt phiên ";
    private static final String LOG_REJECT_SESSION = "Đang từ chối phiên đấu giá ";
    private static final String LOG_BAN_USER = "Đang khóa tài khoản user ";
    private static final String LOG_CANCEL_AUCTION = "Đang hủy phiên đấu giá ";
    private static final String LOG_HIDE_PRODUCT = "Đang ẩn sản phẩm ";
    private static final String LOG_SHOW_PRODUCT = "Đang hiện sản phẩm ";
    private static final String LOG_GET_ALL_USERS = "Đang lấy danh sách người dùng";

    private static final String SUCCESS_GET_PENDING_SESSIONS = "Lấy danh sách phiên chờ duyệt thành công";
    private static final String SUCCESS_GET_ALL_SESSIONS = "Lấy danh sách phiên thành công";
    private static final String SUCCESS_GET_SESSION_DETAIL = "Lấy chi tiết phiên thành công";
    private static final String SUCCESS_APPROVE_SESSION = "Phê duyệt thành công! Phiên đấu giá đã bắt đầu.";
    private static final String SUCCESS_REJECT_SESSION = "Đã từ chối phiên đấu giá.";
    private static final String SUCCESS_BAN_USER = "Đã khóa tài khoản user.";
    private static final String SUCCESS_CANCEL_AUCTION = "Đã hủy phiên đấu giá.";
    private static final String SUCCESS_HIDE_PRODUCT = "Đã ẩn sản phẩm.";
    private static final String SUCCESS_SHOW_PRODUCT = "Đã hiện sản phẩm.";
    private static final String SUCCESS_GET_ALL_USERS = "Lấy danh sách người dùng thành công";

    private static final int BAD_REQUEST_STATUS = 400;

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/pending")
    public ApiResponse<List<SessionResponseDTO>> getPendingSessions() {
        return handleRequest(
                LOG_GET_PENDING_SESSIONS,
                SUCCESS_GET_PENDING_SESSIONS,
                adminService::getPendingSessions
        );
    }

    @GetMapping("/sessions")
    public ApiResponse<List<SessionResponseDTO>> getAllSessions(
            @RequestParam(required = false) String status
    ) {
        return handleRequest(
                LOG_GET_ALL_SESSIONS,
                SUCCESS_GET_ALL_SESSIONS,
                () -> adminService.getAllSessions(status)
        );
    }

    @GetMapping("/session-detail/{sessionId}")
    public ApiResponse<SessionResponseDTO> getSessionDetail(@PathVariable Integer sessionId) {
        return handleRequest(
                LOG_GET_SESSION_DETAIL + sessionId,
                SUCCESS_GET_SESSION_DETAIL,
                () -> adminService.getSessionDetail(sessionId)
        );
    }

    @PostMapping("/approve/{sessionId}")
    public ApiResponse<Void> approveSession(
            @PathVariable Integer sessionId,
            @RequestParam Integer adminId
    ) {
        return handleCommand(
                LOG_APPROVE_SESSION + sessionId,
                SUCCESS_APPROVE_SESSION,
                () -> adminService.approveSession(sessionId, adminId)
        );
    }

    @PostMapping("/reject/{sessionId}")
    public ApiResponse<Void> rejectSession(
            @PathVariable Integer sessionId,
            @RequestParam Integer adminId,
            @RequestParam String reason
    ) {
        return handleCommand(
                LOG_REJECT_SESSION + sessionId,
                SUCCESS_REJECT_SESSION,
                () -> adminService.rejectSession(sessionId, adminId, reason)
        );
    }

    @PostMapping("/ban-user/{userId}")
    public ApiResponse<Void> banUser(
            @PathVariable Integer userId,
            @RequestParam Integer adminId
    ) {
        return handleCommand(
                LOG_BAN_USER + userId,
                SUCCESS_BAN_USER,
                () -> adminService.banUser(userId, adminId)
        );
    }

    @PostMapping("/cancel-auction/{sessionId}")
    public ApiResponse<Void> cancelAuction(
            @PathVariable Integer sessionId,
            @RequestParam Integer adminId
    ) {
        return handleCommand(
                LOG_CANCEL_AUCTION + sessionId,
                SUCCESS_CANCEL_AUCTION,
                () -> adminService.cancelAuction(sessionId, adminId)
        );
    }

    @PostMapping("/hide-product/{productId}")
    public ApiResponse<Void> hideProduct(
            @PathVariable Integer productId,
            @RequestParam Integer adminId
    ) {
        return handleCommand(
                LOG_HIDE_PRODUCT + productId,
                SUCCESS_HIDE_PRODUCT,
                () -> adminService.hideProduct(productId, adminId)
        );
    }

    @PostMapping("/show-product/{productId}")
    public ApiResponse<Void> showProduct(
            @PathVariable Integer productId,
            @RequestParam Integer adminId
    ) {
        return handleCommand(
                LOG_SHOW_PRODUCT + productId,
                SUCCESS_SHOW_PRODUCT,
                () -> adminService.showProduct(productId, adminId)
        );
    }

    @GetMapping("/users")
    public ApiResponse<List<UserResponseDTO>> getAllUsers(
            @RequestParam(required = false) String role
    ) {
        return handleRequest(
                LOG_GET_ALL_USERS,
                SUCCESS_GET_ALL_USERS,
                () -> adminService.getAllUsers(role)
        );
    }

    private ApiResponse<Void> handleCommand(
            String logMessage,
            String successMessage,
            Runnable action
    ) {
        return handleRequest(
                logMessage,
                successMessage,
                () -> {
                    action.run();
                    return null;
                }
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
            return ApiResponse.error(BAD_REQUEST_STATUS, e.getMessage());

        } catch (Exception e) {
            logger.error("{} thất bại: {}", logMessage, e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }
}