package com.auction.server.util;

import com.auction.server.model.AuctionSession;
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
        if (sellerId == null) {
            throw new IllegalArgumentException("Seller not found");
        }

        User user = userRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found"));

        if (!(user instanceof Seller)) {
            logger.error("User {} is not a seller", sellerId);
            throw new IllegalArgumentException("This user is not a seller");
        }

        return (Seller) user;
    }

    public AuctionSession getSessionById(Integer sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Auction session does not exist");
        }

        return auctionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Auction session does not exist"));
    }

    public void validateSessionOwner(AuctionSession session, Integer sellerId, String errorMessage) {
        if (session.getSeller() == null || !session.getSeller().getId().equals(sellerId)) {
            logger.error("Seller {} does not have permission for session {}", sellerId, session.getId());
            throw new IllegalArgumentException(errorMessage);
        }
    }
}