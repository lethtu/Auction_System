package com.auction.server.repository;

import com.auction.server.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Integer> {
    List<Bid> findBySessionIdOrderByAmountDesc(Integer sessionId);
    List<Bid> findBySessionIdOrderByTimeAsc(Integer sessionId);
    int countBySessionId(Integer sessionId);
}