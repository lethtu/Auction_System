package com.auction.server.service;

import com.auction.server.model.AuctionSession;
import com.auction.server.repository.AuctionSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuctionService {

    @Autowired
    private AuctionSessionRepository auctionSessionRepository;

    // Thay vì lấy hết, giờ ta chỉ lấy những phiên đang "Mở" cho sảnh đấu giá
    public List<AuctionSession> getActiveSessions() {
        return auctionSessionRepository.findByStatus("ACTIVE");
    }

    public AuctionSession getSessionById(Integer id) {
        return auctionSessionRepository.findById(id).orElse(null);
    }

    // Thêm hàm này để sau này phục vụ SellerController nhé
    public List<AuctionSession> getSessionsBySeller(Integer sellerId) {
        return auctionSessionRepository.findBySeller_Id(sellerId);
    }
}