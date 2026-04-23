package com.auction.server.repository;

import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionSessionRepository extends JpaRepository<AuctionSession, Integer> {

    //Tùng từng dùng String, tôi ép dùng Enum cho chuẩn
    List<AuctionSession> findByStatus(AuctionStatus status);

    // 1. Cập nhật trạng thái PENDING -> ACTIVE
    @Modifying
    @Query("UPDATE AuctionSession a SET a.status = :newStatus WHERE a.status = :oldStatus AND a.startTime <= :now")
    int updateStatusToActive(
            @Param("oldStatus") AuctionStatus oldStatus,
            @Param("newStatus") AuctionStatus newStatus,
            @Param("now") LocalDateTime now
    );
    List<AuctionSession> findBySeller_Id(Integer sellerId);

    // 2. Cập nhật trạng thái ACTIVE -> ENDED
    @Modifying
    @Query("UPDATE AuctionSession a SET a.status = :newStatus WHERE a.status = :oldStatus AND a.endTime <= :now")
    int updateStatusToEnded(
            @Param("oldStatus") AuctionStatus oldStatus,
            @Param("newStatus") AuctionStatus newStatus,
            @Param("now") LocalDateTime now
    );

    Page<AuctionSession> findByStatus(AuctionStatus status, Pageable pageable);
}