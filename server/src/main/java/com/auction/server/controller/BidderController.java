package com.auction.server.controller;

import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.view.ApiResponse;
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

        // Sắp xếp ưu tiên hiển thị những cái mới nhất (theo startTime giảm dần)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));

        // Gọi hàm phân trang từ Repository với trạng thái ACTIVE
        Page<AuctionSession> activeSessions = auctionSessionRepository.findByStatus(AuctionStatus.ACTIVE, pageable);

        // Gói dữ liệu vào ApiResponse chuẩn form
        return new ApiResponse<>(200, "Lấy danh sách đấu giá thành công", activeSessions);
    }

    // API Đặt giá
    @PostMapping("/place-bid")
    public synchronized ResponseEntity<?> placeBid(@RequestParam Integer sessionId,
                                                   @RequestParam Integer bidderId,
                                                   @RequestParam BigDecimal bidAmount) {
        // Đã sửa sessionRepository -> auctionSessionRepository
        AuctionSession session = auctionSessionRepository.findById(sessionId).orElse(null);
        User bidder = userRepository.findById(bidderId).orElse(null);

        if (session == null || bidder == null) {
            return ResponseEntity.badRequest().body("Invalid session or user");
        }

        // Đã sửa "ACTIVE".equals() -> Kiểm tra bằng Enum chuẩn
        if (session.getStatus() != AuctionStatus.ACTIVE) {
            return ResponseEntity.badRequest().body("Auction is not active");
        }

        BigDecimal minBid = session.getCurrentPrice().add(session.getStepPrice());
        if (bidAmount.compareTo(minBid) < 0) {
            return ResponseEntity.badRequest().body("Bid amount must be at least " + minBid);
        }

        session.setCurrentPrice(bidAmount);
        // Đã sửa sessionRepository -> auctionSessionRepository
        auctionSessionRepository.save(session);

        Bid bid = new Bid();
        bid.setSession(session);
        bid.setBidder(bidder);
        bid.setAmount(bidAmount);
        bid.setTime(LocalDateTime.now());
        bidRepository.save(bid);

        return ResponseEntity.ok("Bid placed successfully");
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