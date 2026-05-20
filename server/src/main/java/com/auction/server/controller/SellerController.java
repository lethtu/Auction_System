package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.dto.SellerStatsDTO;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.service.SellerService;
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

    private static final String LOG_CREATE_AUCTION = "Đang tạo phiên đấu giá";
    private static final String LOG_VIEW_MY_SESSIONS = "Đang lấy danh sách phiên của seller";
    private static final String LOG_GET_SESSION_DETAIL = "Đang lấy chi tiết phiên";
    private static final String LOG_UPDATE_PENDING_SESSION = "Đang sửa phiên đấu giá chờ duyệt";
    private static final String LOG_CANCEL_AUCTION = "Đang hủy phiên đấu giá";
    private static final String LOG_GET_STATS = "Đang thống kê seller";

    private static final String SUCCESS_CREATE_AUCTION = "Tạo phiên đấu giá thành công.";
    private static final String SUCCESS_VIEW_MY_SESSIONS = "Lấy danh sách thành công";
    private static final String SUCCESS_GET_SESSION_DETAIL = "Lấy chi tiết thành công";
    private static final String SUCCESS_UPDATE_PENDING_SESSION = "Đã cập nhật phiên chờ duyệt thành công.";
    private static final String SUCCESS_CANCEL_AUCTION = "Đã hủy phiên thành công";
    private static final String SUCCESS_GET_STATS = "Lấy thống kê thành công";

    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @PostMapping("/create-auction")
    public ApiResponse<SessionResponseDTO> createAuction(@RequestBody CreateAuctionRequest request) {
        return handleRequest(
                LOG_CREATE_AUCTION,
                SUCCESS_CREATE_AUCTION,
                () -> sellerService.createAuctionSession(request)
        );
    }

    @GetMapping("/my-sessions/{sellerId}")
    public ApiResponse<List<SessionResponseDTO>> viewMySessions(
            @PathVariable Integer sellerId,
            @RequestParam(required = false) String status
    ) {
        return handleRequest(
                LOG_VIEW_MY_SESSIONS,
                SUCCESS_VIEW_MY_SESSIONS,
                () -> sellerService.getMySessions(sellerId, status)
        );
    }

    @GetMapping("/session-detail/{sessionId}")
    public ApiResponse<SessionResponseDTO> getSessionDetail(
            @PathVariable Integer sessionId,
            @RequestParam Integer sellerId
    ) {
        return handleRequest(
                LOG_GET_SESSION_DETAIL,
                SUCCESS_GET_SESSION_DETAIL,
                () -> sellerService.getSessionDetail(sessionId, sellerId)
        );
    }

    /**
     * API Sửa phiên đấu giá
     */
    @PutMapping("/update-session/{sessionId}")
    public ApiResponse<SessionResponseDTO> updateSession(
            @PathVariable Integer sessionId,
            @RequestParam Integer sellerId,
            @RequestBody CreateAuctionRequest request
    ) {
        return handleRequest(
                LOG_UPDATE_PENDING_SESSION,
                SUCCESS_UPDATE_PENDING_SESSION,
                () -> {
                    request.setSellerId(sellerId);
                    return sellerService.updateSession(sessionId, sellerId, request);
                }
        );
    }

    @DeleteMapping("/cancel-session/{sessionId}")
    public ApiResponse<Void> cancelAuction(
            @PathVariable Integer sessionId,
            @RequestParam Integer sellerId
    ) {
        return handleCommand(
                LOG_CANCEL_AUCTION,
                SUCCESS_CANCEL_AUCTION,
                () -> sellerService.cancelSession(sessionId, sellerId)
        );
    }

    @GetMapping("/stats/{sellerId}")
    public ApiResponse<SellerStatsDTO> getStats(@PathVariable Integer sellerId) {
        return handleRequest(
                LOG_GET_STATS,
                SUCCESS_GET_STATS,
                () -> sellerService.getSellerStats(sellerId)
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