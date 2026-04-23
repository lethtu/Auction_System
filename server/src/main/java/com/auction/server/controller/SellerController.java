package com.auction.server.controller;

import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.dto.SellerStatsDTO;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.model.AuctionSession;
import com.auction.server.service.SellerService;
import com.auction.server.view.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// @RestController: Báo cho Spring biết đây là nơi tiếp nhận API và sẽ trả về dữ liệu dạng JSON
@RestController
// @RequestMapping: Thiết lập đường dẫn gốc cho toàn bộ API trong class này
@RequestMapping("/api/seller")
public class SellerController {

    // Tiêm (Inject) Service vào Controller để sử dụng
    @Autowired
    private SellerService sellerService;

    /**
     * API tạo phiên đấu giá mới (Chuẩn của Khánh)
     */
    @PostMapping("/create-auction")
    public ApiResponse<AuctionSession> createAuction(@RequestBody CreateAuctionRequest request) {
        try {
            AuctionSession newSession = sellerService.createAuctionSession(request);
            // Đã sửa thành getItem().getName() thay vì getProduct() của Minh
            return new ApiResponse<>(200, "Tạo phiên đấu giá thành công cho món: " + newSession.getItem().getName(), newSession);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(500, "Lỗi hệ thống khi tạo phiên: " + e.getMessage(), null);
        }
    }

    /**
     * API Lấy danh sách phiên đấu giá của Seller (Tính năng của Minh, chuẩn hóa theo Khánh)
     */
    @GetMapping("/my-sessions/{sellerId}")
    public ApiResponse<List<SessionResponseDTO>> viewMySessions(
            @PathVariable Integer sellerId,
            @RequestParam(required = false) String status
    ) {
        try {
            List<SessionResponseDTO> data = sellerService.getMySessions(sellerId, status);
            return new ApiResponse<>(200, "Lấy danh sách thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(500, e.getMessage(), null);
        }
    }

    /**
     * API Xem chi tiết 1 phiên đấu giá
     */
    @GetMapping("/session-detail/{sessionId}")
    public ApiResponse<SessionResponseDTO> getSessionDetail(
            @PathVariable Integer sessionId,
            @RequestParam Integer sellerId
    ) {
        try {
            SessionResponseDTO data = sellerService.getSessionDetail(sessionId, sellerId);
            return new ApiResponse<>(200, "Lấy chi tiết thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(500, e.getMessage(), null);
        }
    }

    /**
     * API Sửa phiên đấu giá đang chờ duyệt
     */
    @PutMapping("/update-session/{sessionId}")
    public ApiResponse<AuctionSession> updatePendingSession(
            @PathVariable Integer sessionId,
            @RequestParam Integer sellerId,
            @RequestBody CreateAuctionRequest request // Đổi AuctionRequestDTO thành chuẩn
    ) {
        try {
            request.setSellerId(sellerId);
            AuctionSession updatedSession = sellerService.updatePendingSession(sessionId, sellerId, request);
            return new ApiResponse<>(200, "Đã cập nhật phiên chờ duyệt cho món: " + updatedSession.getItem().getName(), updatedSession);
        } catch (Exception e) {
            return new ApiResponse<>(500, e.getMessage(), null);
        }
    }

    /**
     * API Hủy phiên đấu giá
     */
    @DeleteMapping("/cancel-session/{sessionId}")
    public ApiResponse<String> cancelAuction(
            @PathVariable Integer sessionId,
            @RequestParam Integer sellerId
    ) {
        try {
            sellerService.cancelSession(sessionId, sellerId);
            return new ApiResponse<>(200, "Đã hủy phiên thành công", null);
        } catch (Exception e) {
            return new ApiResponse<>(500, e.getMessage(), null);
        }
    }

    /**
     * API Thống kê cho Seller
     */
    @GetMapping("/stats/{sellerId}")
    public ApiResponse<SellerStatsDTO> getStats(@PathVariable Integer sellerId) {
        try {
            SellerStatsDTO stats = sellerService.getSellerStats(sellerId);
            return new ApiResponse<>(200, "Lấy thống kê thành công", stats);
        } catch (Exception e) {
            return new ApiResponse<>(500, e.getMessage(), null);
        }
    }
}