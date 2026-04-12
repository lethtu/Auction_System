package com.auction.server.service;

import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Product;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.ProductRepository; // Đảm bảo em đã tạo Repo này
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SellerService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuctionSessionRepository auctionSessionRepository;

    /**
     * Hàm xử lý tạo phiên đấu giá.
     * Sử dụng @Transactional để đảm bảo tính toàn vẹn dữ liệu giữa Product và AuctionSession.
     */
    @Transactional
    public AuctionSession createAuctionSession(CreateAuctionRequest request) {
        // --- BẮT ĐẦU ĐOẠN VALIDATION CẦN THÊM VÀO ---
        LocalDateTime now = LocalDateTime.now();

        if (request.getStartTime().isBefore(now)) {
            throw new IllegalArgumentException("Thời gian bắt đầu không được nằm trong quá khứ.");
        }
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu.");
        }
        if (request.getStartingPrice() < 0) {
            throw new IllegalArgumentException("Giá khởi điểm không được là số âm.");
        }
        // --- KẾT THÚC ĐOẠN VALIDATION ---
        // 1. Khởi tạo và lưu Product
        Product product = new Product();
        product.setName(request.getName());
        product.setType(request.getType());
        product.setImagePath(request.getImagePath());

        // Save product xuống DB để lấy ID
        Product savedProduct = productRepository.save(product);

        // 2. Khởi tạo và lưu AuctionSession
        AuctionSession session = new AuctionSession();
        session.setProduct(savedProduct); // Map Product vừa lưu vào Session

        // Khởi tạo các giá trị ban đầu cho phiên đấu giá
        session.setCurrentPrice(request.getStartingPrice()); // Giá hiện tại bằng giá khởi điểm
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setStatus(AuctionStatus.PENDING); // Trạng thái mặc định là chờ diễn ra

        // Save session và trả về kết quả
        return auctionSessionRepository.save(session);
    }
}