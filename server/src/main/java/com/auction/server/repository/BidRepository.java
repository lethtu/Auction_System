package com.auction.server.repository;

import com.auction.server.model.Bid;
import com.auction.server.model.AuctionSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Integer> {
    List<Bid> findBySessionIdOrderByAmountDesc(Integer sessionId);
    List<Bid> findBySessionIdOrderByAmountAsc(Integer sessionId);
    List<Bid> findBySessionIdOrderByTimeAsc(Integer sessionId);
    int countBySessionId(Integer sessionId);

    @Query("SELECT DISTINCT b.session FROM Bid b WHERE b.bidder.id = :bidderId")
    List<AuctionSession> findDistinctSessionsByBidderId(@Param("bidderId") Integer bidderId);

    @Query("""
            SELECT DISTINCT s FROM Bid b
            JOIN b.session s
            JOIN FETCH s.item
            LEFT JOIN FETCH s.seller
            WHERE b.bidder.id = :bidderId
            """)
    List<AuctionSession> findSessionsByBidderId(@Param("bidderId") Integer bidderId);

    @Query("SELECT MAX(b.amount) FROM Bid b WHERE b.session.id = :sessionId AND b.bidder.id = :bidderId")
    java.math.BigDecimal findMaxBidAmountBySessionIdAndBidderId(@Param("sessionId") Integer sessionId, @Param("bidderId") java.lang.Integer bidderId);

    @Query("""
            SELECT b.session.id, COUNT(b.id),
                   MAX(CASE WHEN b.bidder.id = :bidderId THEN b.amount ELSE NULL END)
            FROM Bid b
            WHERE b.session.id IN :sessionIds
            GROUP BY b.session.id
            """)
    List<Object[]> findSessionStatsForBidder(
            @Param("sessionIds") Collection<Integer> sessionIds,
            @Param("bidderId") Integer bidderId
    );
}
