package com.auction.server.repository;

import com.auction.server.model.AuctionSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuctionSessionRepository extends JpaRepository<AuctionSession, Integer> {

    List<AuctionSession> findByStatus(String status);

    List<AuctionSession> findBySeller_Id(Integer sellerId);

    @Query("SELECT s FROM AuctionSession s WHERE s.status = 'ACTIVE' AND s.endTime <= :now")
    List<AuctionSession> findActiveExpiredSessions(@Param("now") LocalDateTime now);
}