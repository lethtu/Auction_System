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

    private static final String LOG_GET_PENDING_SESSIONS = "Fetching pending sessions";
    private static final String LOG_GET_ALL_SESSIONS = "Fetching session list";
    private static final String LOG_GET_SESSION_DETAIL = "Fetching session details ";
    private static final String LOG_APPROVE_SESSION = "Approving session ";
    private static final String LOG_REJECT_SESSION = "Rejecting auction session ";
    private static final String LOG_BAN_USER = "Banning user account ";
    private static final String LOG_RESTORE_USER = "Restoring user account ";
    private static final String LOG_CANCEL_AUCTION = "Canceling auction session ";
    private static final String LOG_HIDE_PRODUCT = "Hiding product ";
    private static final String LOG_SHOW_PRODUCT = "Showing product ";
    private static final String LOG_GET_ALL_USERS = "Fetching user list";

    private static final String SUCCESS_GET_PENDING_SESSIONS = "Pending sessions retrieved successfully";
    private static final String SUCCESS_GET_ALL_SESSIONS = "Session list retrieved successfully";
    private static final String SUCCESS_GET_SESSION_DETAIL = "Session details retrieved successfully";
    private static final String SUCCESS_APPROVE_SESSION = "Approved successfully! The auction session has started.";
    private static final String SUCCESS_REJECT_SESSION = "Auction session rejected.";
    private static final String SUCCESS_BAN_USER = "User account has been banned.";
    private static final String SUCCESS_RESTORE_USER = "User account has been restored.";
    private static final String SUCCESS_CANCEL_AUCTION = "Auction session has been canceled.";
    private static final String SUCCESS_HIDE_PRODUCT = "Product has been hidden.";
    private static final String SUCCESS_SHOW_PRODUCT = "Product is now visible.";
    private static final String SUCCESS_GET_ALL_USERS = "User list retrieved successfully";

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

    @PostMapping({"/restore-user/{userId}", "/unban-user/{userId}", "/users/{userId}/restore"})
    public ApiResponse<Void> restoreUser(
            @PathVariable Integer userId,
            @RequestParam Integer adminId
    ) {
        return handleCommand(
                LOG_RESTORE_USER + userId,
                SUCCESS_RESTORE_USER,
                () -> adminService.restoreUser(userId, adminId)
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
            logger.warn("{} failed: {}", logMessage, e.getMessage());
            return ApiResponse.error(BAD_REQUEST_STATUS, e.getMessage());

        } catch (Exception e) {
            logger.error("{} failed: {}", logMessage, e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }
}