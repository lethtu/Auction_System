package com.auction.server.service;

import com.auction.server.dto.AuctionRequestDTO;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.Product;
import com.auction.server.model.Seller;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.ProductRepository;
import com.auction.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SellerService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuctionSessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public AuctionSession createAuction(AuctionRequestDTO dto) {
        Seller seller = (Seller) userRepository.findById(dto.getSellerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán"));

        Product product = new Product();
        product.setName(dto.getProductName());
        product.setType(dto.getProductType());
        product.setImageUrl(dto.getImageUrl());
        product.setDescription(dto.getDescription());
        Product savedProduct = productRepository.save(product);

        AuctionSession session = new AuctionSession();
        session.setProduct(savedProduct);
        session.setSeller(seller);
        session.setStartingPrice(dto.getStartingPrice());
        session.setCurrentPrice(dto.getStartingPrice());
        session.setStepPrice(dto.getStepPrice());
        session.setStartTime(LocalDateTime.now());
        session.setEndTime(dto.getEndTime());
        session.setStatus("PENDING");

        return sessionRepository.save(session);
    }

    public List<AuctionSession> getMySessions(Integer sellerId) {
        return sessionRepository.findBySeller_Id(sellerId);
    }

    public void cancelSession(Integer sessionId, Integer sellerId) {
        AuctionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Phiên đấu giá không tồn tại"));

        if (!session.getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("Bạn không có quyền hủy phiên này");
        }

        if (!"PENDING".equals(session.getStatus())) {
            throw new RuntimeException("Chỉ được hủy phiên ở trạng thái chờ duyệt");
        }

        session.setStatus("CANCELED");
        sessionRepository.save(session);
    }
}