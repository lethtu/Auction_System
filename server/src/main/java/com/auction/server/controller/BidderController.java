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
import com.auction.server.service.BidderService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bidder")
public class BidderController {
    private static final Logger logger = LoggerFactory.getLogger(BidderController.class);
    @Autowired
    private BidderService bidderService;

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


    @GetMapping("/my-bidding-sessions")
    public ApiResponse<List<AuctionSession>> getMyBiddingSessions(@RequestParam Integer bidderId) {
        if (bidderId == null || bidderId <= 0) {
            return new ApiResponse<>(400, "bidderId không hợp lệ", List.of());
        }

        List<AuctionSession> sessions = bidRepository.findDistinctSessionsByBidderId(bidderId);
        return new ApiResponse<>(200, "Lấy danh sách phiên người dùng đang đấu giá thành công", sessions);
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

    @PostMapping("/up-to-seller")
    public ApiResponse<String> upToSeller(@RequestParam Integer userId) {
        Map<String, Object> result = bidderService.upToSeller(userId);
        boolean isSuccess = (boolean) result.get("success");
        String message = (String) result.get("message");
        if (isSuccess) {
            return new ApiResponse<>(200, message, "SUCCESS");
        } else {
            return new ApiResponse<>(400, message, "FAILED");
        }
    }
}