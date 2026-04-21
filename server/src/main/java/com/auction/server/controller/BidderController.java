package com.auction.server.controller;

import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.view.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bidder")
public class BidderController {

    @Autowired
    private AuctionSessionRepository auctionSessionRepository;

    // API lấy danh sách đang đấu giá (có phân trang)
    // Thay đổi kiểu trả về thành ApiResponse bọc lấy Page<AuctionSession>
    @GetMapping("/active-sessions")
    public ApiResponse<Page<AuctionSession>> getActiveSessions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        // Sắp xếp ưu tiên hiển thị những cái mới nhất (theo startTime giảm dần)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));

        // Gọi hàm phân trang từ Repository với trạng thái ACTIVE
        Page<AuctionSession> activeSessions = auctionSessionRepository.findByStatus(AuctionStatus.ACTIVE, pageable);

        // Gói dữ liệu vào ApiResponse chuẩn form
        return new ApiResponse<>(200, "Lấy danh sách đấu giá thành công", activeSessions);
    }
}