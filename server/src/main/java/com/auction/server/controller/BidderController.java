package com.auction.server.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bidder")
public class BidderController {

    // Phương thức 1: Nạp tiền vào ví
    @PostMapping("/deposit")
    public String depositMoney(@RequestParam Integer bidderId, @RequestParam Double amount) {
        // Logic gọi Database cộng tiền vào balance của user
        return "Đã nạp thành công " + amount + " vào tài khoản ID " + bidderId;
    }

    // Phương thức 2: Đặt giá (Đấu giá)
    @PostMapping("/place-bid")
    public String placeBid(@RequestParam Integer itemId, @RequestParam Integer bidderId, @RequestParam Double bidAmount) {
        // Logic kiểm tra số dư và giá hiện tại
        return "Người dùng " + bidderId + " đã đặt giá " + bidAmount + " cho vật phẩm " + itemId;
    }
}