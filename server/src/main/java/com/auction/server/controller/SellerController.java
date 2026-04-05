package com.auction.server.controller;

import com.auction.server.model.Item;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/seller")
public class SellerController {

    // Phương thức 1: Đăng đồ lên đấu giá
    @PostMapping("/create-item")
    public String createAuctionItem(@RequestBody Item newItem) {
        // Logic thực tế sau này team sẽ gọi Repository để lưu vào Database
        // itemRepository.save(newItem);
        return "Người bán đã đăng thành công món: " + newItem.getName();
    }

    // Phương thức 2: Xem các món đồ mình đã đăng
    @GetMapping("/my-items/{sellerId}")
    public List<Item> viewMyListedItems(@PathVariable Integer sellerId) {
        // Tạm thời trả về list rỗng để team ghép code sau
        return new ArrayList<>();
    }

    // Phương thức 3: Hủy phiên đấu giá
    @DeleteMapping("/cancel/{itemId}")
    public String cancelAuction(@PathVariable Integer itemId) {
        return "Đã hủy phiên đấu giá cho vật phẩm ID: " + itemId;
    }
}