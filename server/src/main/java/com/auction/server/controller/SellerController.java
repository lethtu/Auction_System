package com.auction.server.controller;

import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.model.AuctionSession;
import com.auction.server.service.SellerService;
import com.auction.server.view.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

// @RestController: Báo cho Spring biết đây là nơi tiếp nhận API và sẽ trả về dữ liệu dạng JSON
@RestController
// @RequestMapping: Thiết lập đường dẫn gốc cho toàn bộ API trong class này
@RequestMapping("/api/seller")
public class SellerController {

    // Tiêm (Inject) Service vào Controller để sử dụng
    @Autowired
    private SellerService sellerService;

    /**
     * API tạo phiên đấu giá mới.
     * Endpoint: POST http://localhost:8080/api/seller/create-auction
     * @RequestBody: Ép Spring Boot tự động chuyển JSON từ Client gửi lên thành object CreateAuctionRequest
     */
    @PostMapping("/create-auction")
    public ApiResponse<AuctionSession> createAuction(@RequestBody CreateAuctionRequest request) {
        try {
            // 1. Gọi Service xử lý nghiệp vụ tạo phiên
            AuctionSession newSession = sellerService.createAuctionSession(request);

            // 2. Nếu thành công, trả về status 200 kèm data
            return new ApiResponse<>(200, "Tạo phiên đấu giá thành công!", newSession);

        } catch (Exception e) {
            // 3. Nếu có lỗi (ví dụ: mất kết nối DB), bắt lỗi và trả về status 500
            e.printStackTrace(); // In lỗi ra console server để dev dễ debug
            return new ApiResponse<>(500, "Lỗi hệ thống khi tạo phiên đấu giá: " + e.getMessage(), null);
        }
    }

    // Nơi đây sau này ta có thể viết thêm các API khác của Seller như:
    // @GetMapping("/my-auctions") -> Xem danh sách hàng mình đang bán
    // @PutMapping("/cancel-auction/{id}") -> Hủy phiên đấu giá
}