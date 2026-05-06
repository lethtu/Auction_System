package com.auction.server.util;

import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Seller;
import com.auction.server.model.User;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SellerSessionGuard {
    private static final Logger logger = LoggerFactory.getLogger(SellerSessionGuard.class);

    private final UserRepository userRepository;
    private final AuctionSessionRepository auctionSessionRepository;

    public SellerSessionGuard(
            UserRepository userRepository,
            AuctionSessionRepository auctionSessionRepository
    ) {
        this.userRepository = userRepository;
        this.auctionSessionRepository = auctionSessionRepository;
    }

    public Seller getSellerById(Integer sellerId) {
        User user = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán"));

        if (!(user instanceof Seller)) {
            logger.error("Người dùng {} không phải seller", sellerId);
            throw new RuntimeException("Người dùng này không phải seller");
        }

        return (Seller) user;
    }

    public AuctionSession getSessionById(Integer sessionId) {
        return auctionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Phiên đấu giá không tồn tại"));
    }

    public void validateSessionOwner(AuctionSession session, Integer sellerId, String errorMessage) {
        if (session.getSeller() == null || !session.getSeller().getId().equals(sellerId)) {
            logger.error("Seller {} không có quyền với phiên {}", sellerId, session.getId());
            throw new RuntimeException(errorMessage);
        }
    }

    public void validatePendingSession(AuctionSession session) {
        if (session.getStatus() != AuctionStatus.PENDING) {
            throw new RuntimeException("Chỉ được thao tác với phiên đang chờ duyệt");
        }
    }
}