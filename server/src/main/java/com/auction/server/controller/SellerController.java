package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.dto.SellerStatsDTO;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.service.SellerService;
import com.auction.server.util.SessionManager;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/seller")
public class SellerController {

    private static final Logger logger = LoggerFactory.getLogger(SellerController.class);

    private static final int BAD_REQUEST_STATUS = 400;

    private static final String LOG_CREATE_AUCTION = "Creating auction session";
    private static final String LOG_VIEW_MY_SESSIONS = "Fetching seller's session list";
    private static final String LOG_GET_SESSION_DETAIL = "Fetching session details";
    private static final String LOG_UPDATE_PENDING_SESSION = "Updating pending auction session";
    private static final String LOG_CANCEL_AUCTION = "Canceling auction session";
    private static final String LOG_GET_STATS = "Fetching seller statistics";

    private static final String SUCCESS_CREATE_AUCTION = "Auction session created successfully.";
    private static final String SUCCESS_VIEW_MY_SESSIONS = "Session list retrieved successfully";
    private static final String SUCCESS_GET_SESSION_DETAIL = "Session details retrieved successfully";
    private static final String SUCCESS_UPDATE_PENDING_SESSION = "Pending session updated successfully.";
    private static final String SUCCESS_CANCEL_AUCTION = "Session canceled successfully";
    private static final String SUCCESS_GET_STATS = "Statistics retrieved successfully";

    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @PostMapping("/create-auction")
    public ApiResponse<SessionResponseDTO> createAuction(
            HttpServletRequest request,
            @RequestBody CreateAuctionRequest createRequest
    ) {
        return handleRequest(
                LOG_CREATE_AUCTION,
                SUCCESS_CREATE_AUCTION,
                () -> {
                    SessionManager.SessionUser actor = (SessionManager.SessionUser) request.getAttribute("sessionUser");
                    Integer secureSellerId = actor.getUserId();
                    createRequest.setSellerId(secureSellerId);
                    return sellerService.createAuctionSession(createRequest);
                }
        );
    }

    @GetMapping("/my-sessions/{sellerId}")
    public ApiResponse<List<SessionResponseDTO>> viewMySessions(
            HttpServletRequest request,
            @PathVariable Integer sellerId,
            @RequestParam(required = false) String status
    ) {
        return handleRequest(
                LOG_VIEW_MY_SESSIONS,
                SUCCESS_VIEW_MY_SESSIONS,
                () -> {
                    SessionManager.SessionUser actor = (SessionManager.SessionUser) request.getAttribute("sessionUser");
                    Integer secureSellerId = actor.getUserId();
                    return sellerService.getMySessions(secureSellerId, status);
                }
        );
    }

    @GetMapping("/session-detail/{sessionId}")
    public ApiResponse<SessionResponseDTO> getSessionDetail(
            HttpServletRequest request,
            @PathVariable Integer sessionId,
            @RequestParam Integer sellerId
    ) {
        return handleRequest(
                LOG_GET_SESSION_DETAIL,
                SUCCESS_GET_SESSION_DETAIL,
                () -> {
                    SessionManager.SessionUser actor = (SessionManager.SessionUser) request.getAttribute("sessionUser");
                    Integer secureSellerId = actor.getUserId();
                    return sellerService.getSessionDetail(sessionId, secureSellerId);
                }
        );
    }

    /**
     * API to update an auction session
     */
    @PutMapping("/update-session/{sessionId}")
    public ApiResponse<SessionResponseDTO> updateSession(
            HttpServletRequest request,
            @PathVariable Integer sessionId,
            @RequestParam Integer sellerId,
            @RequestBody CreateAuctionRequest updateRequest
    ) {
        return handleRequest(
                LOG_UPDATE_PENDING_SESSION,
                SUCCESS_UPDATE_PENDING_SESSION,
                () -> {
                    SessionManager.SessionUser actor = (SessionManager.SessionUser) request.getAttribute("sessionUser");
                    Integer secureSellerId = actor.getUserId();
                    updateRequest.setSellerId(secureSellerId);
                    return sellerService.updateSession(sessionId, secureSellerId, updateRequest);
                }
        );
    }

    @DeleteMapping("/cancel-session/{sessionId}")
    public ApiResponse<Void> cancelAuction(
            HttpServletRequest request,
            @PathVariable Integer sessionId,
            @RequestParam Integer sellerId
    ) {
        return handleCommand(
                LOG_CANCEL_AUCTION,
                SUCCESS_CANCEL_AUCTION,
                () -> {
                    SessionManager.SessionUser actor = (SessionManager.SessionUser) request.getAttribute("sessionUser");
                    Integer secureSellerId = actor.getUserId();
                    sellerService.cancelSession(sessionId, secureSellerId);
                }
        );
    }

    @GetMapping("/stats/{sellerId}")
    public ApiResponse<SellerStatsDTO> getStats(
            HttpServletRequest request,
            @PathVariable Integer sellerId
    ) {
        return handleRequest(
                LOG_GET_STATS,
                SUCCESS_GET_STATS,
                () -> {
                    SessionManager.SessionUser actor = (SessionManager.SessionUser) request.getAttribute("sessionUser");
                    Integer secureSellerId = actor.getUserId();
                    return sellerService.getSellerStats(secureSellerId);
                }
        );
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> deleteItem(
            HttpServletRequest request,
            @PathVariable Integer itemId
    ) {
        return handleCommand(
                "Deleting product",
                "Product removed",
                () -> {
                    SessionManager.SessionUser actor = (SessionManager.SessionUser) request.getAttribute("sessionUser");
                    Integer secureSellerId = actor.getUserId();
                    sellerService.softDeleteItem(itemId, secureSellerId);
                }
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