package com.auction.server.controller;

import com.auction.server.model.AuctionSession;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/seller")
public class SellerController {

    @PostMapping("/create-session")
    public String createAuctionSession(@RequestBody AuctionSession newSession) {

        String productName = (newSession.getProduct() != null) ? newSession.getProduct().getName() : "không xác định";
        return "Người bán đã tạo phiên đấu giá thành công cho món: " + productName;
    }

    @GetMapping("/my-sessions/{sellerId}")
    public List<AuctionSession> viewMySessions(@PathVariable Integer sellerId) {
        return new ArrayList<>();
    }


    @DeleteMapping("/cancel-session/{sessionId}")
    public String cancelAuction(@PathVariable Integer sessionId) {
        return "Đã hủy phiên đấu giá có ID: " + sessionId;
    }
}