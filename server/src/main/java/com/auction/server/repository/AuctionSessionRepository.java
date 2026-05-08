package com.auction.server.repository;

import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionSessionRepository extends JpaRepository<AuctionSession, Integer> {

    List<AuctionSession> findByStatus(AuctionStatus status);

    List<AuctionSession> findBySeller_Id(Integer sellerId);

    Page<AuctionSession> findByStatus(AuctionStatus status, Pageable pageable);

    Optional<AuctionSession> findById(Integer ItemAuctionId);
    // SCHEDULER QUERIES - FETCH & UPDATE ĐỂ AN TOÀN CHO ĐA LUỒNG

    // Hàm 1: Lấy các phiên đang PENDING nhưng đã tới giờ mở (startTime <= now)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AuctionSession> findByStatusAndStartTimeLessThanEqual(AuctionStatus status, LocalDateTime now);

    // Hàm 2: Lấy các phiên đang ACTIVE nhưng đã hết giờ (endTime <= now)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AuctionSession> findByStatusAndEndTimeLessThanEqual(AuctionStatus status, LocalDateTime now);
}