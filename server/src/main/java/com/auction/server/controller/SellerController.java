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

    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @PostMapping("/create-auction")
    public ApiResponse<SessionResponseDTO> createAuction(@RequestBody CreateAuctionRequest request) {
        return handleRequest(
                "Đang tạo phiên đấu giá",
                "Tạo phiên đấu giá thành công.",
                () -> sellerService.createAuctionSession(request)
        );
    }

    @GetMapping("/my-sessions/{sellerId}")
    public ApiResponse<List<SessionResponseDTO>> viewMySessions(
            @PathVariable Integer sellerId,
            @RequestParam(required = false) String status
    ) {
        return handleRequest(
                "Đang lấy danh sách phiên của seller",
                "Lấy danh sách thành công",
                () -> sellerService.getMySessions(sellerId, status)
        );
    }

    @GetMapping("/session-detail/{sessionId}")
    public ApiResponse<SessionResponseDTO> getSessionDetail(
            @PathVariable Integer sessionId,
            @RequestParam Integer sellerId
    ) {
        return handleRequest(
                "Đang lấy chi tiết phiên",
                "Lấy chi tiết thành công",
                () -> sellerService.getSessionDetail(sessionId, sellerId)
        );
    }

    @PutMapping("/update-session/{sessionId}")
    public ApiResponse<SessionResponseDTO> updatePendingSession(
            @PathVariable Integer sessionId,
            @RequestParam Integer sellerId,
            @RequestBody CreateAuctionRequest request
    ) {
        request.setSellerId(sellerId);

        return handleRequest(
                "Đang sửa phiên đấu giá chờ duyệt",
                "Đã cập nhật phiên chờ duyệt thành công.",
                () -> sellerService.updatePendingSession(sessionId, sellerId, request)
        );
    }

    @DeleteMapping("/cancel-session/{sessionId}")
    public ApiResponse<Void> cancelAuction(
            @PathVariable Integer sessionId,
            @RequestParam Integer sellerId
    ) {
        return handleRequest(
                "Đang huỷ phiên đấu giá",
                "Đã hủy phiên thành công",
                () -> {
                    sellerService.cancelSession(sessionId, sellerId);
                    return null;
                }
        );
    }

    @GetMapping("/stats/{sellerId}")
    public ApiResponse<SellerStatsDTO> getStats(@PathVariable Integer sellerId) {
        return handleRequest(
                "Đang thống kê seller",
                "Lấy thống kê thành công",
                () -> sellerService.getSellerStats(sellerId)
        );
    }

    private <T> ApiResponse<T> handleRequest(
            String logMessage,
            String successMessage,
            Supplier<T> action
    ) {
        try {
            logger.info(logMessage);
            T data = action.get();
            return ApiResponse.success(successMessage, data);

        } catch (IllegalArgumentException e) {
            logger.warn("{} thất bại: {}", logMessage, e.getMessage());
            return ApiResponse.error(400, e.getMessage());

        } catch (Exception e) {
            logger.error("{} thất bại: {}", logMessage, e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }
}