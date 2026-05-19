package com.auction.server.repository;

import com.auction.server.model.AuctionSession;
import com.auction.server.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Integer> {
    List<Bid> findBySessionIdOrderByAmountDesc(Integer sessionId);
    int countBySessionId(Integer sessionId);

    @Query("SELECT DISTINCT b.session FROM Bid b WHERE b.bidder.id = :bidderId")
    List<AuctionSession> findDistinctSessionsByBidderId(@Param("bidderId") Integer bidderId);
}
