package com.auction.server.repository;

import com.auction.server.model.AuctionSession; // Import class mới
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuctionSessionRepository extends JpaRepository<AuctionSession, Integer> {

    List<AuctionSession> findByStatus(String status);
}