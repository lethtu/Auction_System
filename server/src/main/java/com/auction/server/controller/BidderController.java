package com.auction.server.controller;

import com.auction.server.model.*;
import com.auction.server.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/bidder")
public class BidderController {

    @Autowired
    private AuctionSessionRepository sessionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/place-bid")
    public synchronized ResponseEntity<?> placeBid(@RequestParam Integer sessionId,
                                                   @RequestParam Integer bidderId,
                                                   @RequestParam BigDecimal bidAmount) {
        AuctionSession session = sessionRepository.findById(sessionId).orElse(null);
        User bidder = userRepository.findById(bidderId).orElse(null);

        if (session == null || bidder == null) {
            return ResponseEntity.badRequest().body("Invalid session or user");
        }

        if (!"ACTIVE".equals(session.getStatus())) {
            return ResponseEntity.badRequest().body("Auction is not active");
        }

        BigDecimal minBid = session.getCurrentPrice().add(session.getStepPrice());
        if (bidAmount.compareTo(minBid) < 0) {
            return ResponseEntity.badRequest().body("Bid amount must be at least " + minBid);
        }

        session.setCurrentPrice(bidAmount);
        sessionRepository.save(session);

        Bid bid = new Bid();
        bid.setSession(session);
        bid.setBidder(bidder);
        bid.setAmount(bidAmount);
        bid.setTime(LocalDateTime.now());
        bidRepository.save(bid);

        return ResponseEntity.ok("Bid placed successfully");
    }

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