package com.auction.server.repository;

import com.auction.server.model.AuctionSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuctionSessionRepository extends JpaRepository<AuctionSession, Integer> {
    List<AuctionSession> findByStatus(String status);
    List<AuctionSession> findBySeller_Id(Integer sellerId);
}