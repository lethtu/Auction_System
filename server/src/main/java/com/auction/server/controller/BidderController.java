package com.auction.server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.dto.ApiResponse;
import com.auction.server.model.*;
import com.auction.server.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/bidder")
public class BidderController {
    private static final Logger logger = LoggerFactory.getLogger(BidderController.class);

    @Autowired
    private AuctionSessionRepository auctionSessionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private UserRepository userRepository;

    // API lấy danh sách đang đấu giá (có phân trang)
    @GetMapping("/active-sessions")
    public ApiResponse<Page<AuctionSession>> getActiveSessions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        logger.info("Đang lấy danh sách đấu giá");
        // Sắp xếp ưu tiên hiển thị những cái mới nhất (theo startTime giảm dần)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));

        // Gọi hàm phân trang từ Repository với trạng thái ACTIVE
        Page<AuctionSession> activeSessions = auctionSessionRepository.findByStatus(AuctionStatus.ACTIVE, pageable);

        // Gói dữ liệu vào ApiResponse chuẩn form
        return new ApiResponse<>(200, "Lấy danh sách đấu giá thành công", activeSessions);
    }

    // API Nạp tiền
    @PostMapping("/deposit")
    public ResponseEntity<?> depositMoney(@RequestParam Integer bidderId, @RequestParam BigDecimal amount) {
        return userRepository.findById(bidderId)
                .map(user -> {
                    user.setBalance(user.getBalance().add(amount));
                    userRepository.save(user);
                    return ResponseEntity.ok("New balance: " + user.getBalance());
                })
                .orElse(ResponseEntity.notFound().build());
    }
}