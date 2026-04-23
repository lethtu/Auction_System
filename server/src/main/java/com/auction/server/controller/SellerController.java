package com.auction.server.controller;

import com.auction.server.dto.AuctionRequestDTO;
import com.auction.server.dto.SellerStatsDTO;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.model.AuctionSession;
import com.auction.server.service.SellerService;
import com.auction.server.view.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller")
public class SellerController {

    @Autowired
    private SellerService sellerService;

    @PostMapping("/create")
    public ApiResponse<?> createAuction(@Valid @RequestBody AuctionRequestDTO dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getDefaultMessage())
                    .orElse("Dữ liệu không hợp lệ");
            return new ApiResponse<>(400, message, null);
        }

        try {
            AuctionSession session = sellerService.createAuction(dto);
            return new ApiResponse<>(200,
                    "Đã gửi yêu cầu đấu giá cho món: " + session.getProduct().getName(),
                    session.getId());
        } catch (Exception e) {
            return new ApiResponse<>(400, e.getMessage(), null);
        }
    }

    @GetMapping("/my-sessions/{sellerId}")
    public ApiResponse<List<SessionResponseDTO>> viewMySessions(
            @PathVariable Integer sellerId,
            @RequestParam(required = false) String status
    ) {
        try {
            List<SessionResponseDTO> data = sellerService.getMySessions(sellerId, status);
            return new ApiResponse<>(200, "Lấy danh sách phiên thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(400, e.getMessage(), null);
        }
    }

    @GetMapping("/session-detail/{sessionId}")
    public ApiResponse<?> getSessionDetail(
            @PathVariable Integer sessionId,
            @RequestParam Integer sellerId
    ) {
        try {
            return new ApiResponse<>(200, "Lấy chi tiết phiên thành công",
                    sellerService.getSessionDetail(sessionId, sellerId));
        } catch (Exception e) {
            return new ApiResponse<>(400, e.getMessage(), null);
        }
    }

    @PutMapping("/update-session/{sessionId}")
    public ApiResponse<?> updatePendingSession(
            @PathVariable Integer sessionId,
            @RequestParam Integer sellerId,
            @Valid @RequestBody AuctionRequestDTO dto,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getDefaultMessage())
                    .orElse("Dữ liệu không hợp lệ");
            return new ApiResponse<>(400, message, null);
        }

        try {
            dto.setSellerId(sellerId);
            AuctionSession updatedSession = sellerService.updatePendingSession(sessionId, sellerId, dto);
            return new ApiResponse<>(200,
                    "Đã cập nhật phiên chờ duyệt cho món: " + updatedSession.getProduct().getName(),
                    updatedSession.getId());
        } catch (Exception e) {
            return new ApiResponse<>(400, e.getMessage(), null);
        }
    }

    @DeleteMapping("/cancel-session/{sessionId}")
    public ApiResponse<?> cancelAuction(@PathVariable Integer sessionId, @RequestParam Integer sellerId) {
        try {
            sellerService.cancelSession(sessionId, sellerId);
            return new ApiResponse<>(200, "Đã hủy phiên thành công", null);
        } catch (Exception e) {
            return new ApiResponse<>(400, e.getMessage(), null);
        }
    }

    @GetMapping("/stats/{sellerId}")
    public ApiResponse<?> getStats(@PathVariable Integer sellerId) {
        try {
            SellerStatsDTO stats = sellerService.getSellerStats(sellerId);
            return new ApiResponse<>(200, "Lấy thống kê thành công", stats);
        } catch (Exception e) {
            return new ApiResponse<>(400, e.getMessage(), null);
        }
    }
}