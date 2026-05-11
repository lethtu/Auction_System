package com.auction.server.repository;

import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionSessionRepository extends JpaRepository<AuctionSession, Integer> {

    List<AuctionSession> findByStatus(AuctionStatus status);

    Page<AuctionSession> findByStatus(AuctionStatus status, Pageable pageable);

    List<AuctionSession> findBySeller_Id(Integer sellerId);

    List<AuctionSession> findBySeller_IdAndStatus(Integer sellerId, AuctionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AuctionSession s WHERE s.id = :id")
    Optional<AuctionSession> findByIdForUpdate(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AuctionSession> findByStatusAndStartTimeLessThanEqual(AuctionStatus status, LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AuctionSession> findByStatusAndEndTimeLessThanEqual(AuctionStatus status, LocalDateTime now);

    @Modifying
    @Query("UPDATE AuctionSession a SET a.status = :newStatus WHERE a.status = :oldStatus AND a.endTime <= :now")
    int updateStatusToEnded(
            @Param("oldStatus") AuctionStatus oldStatus,
            @Param("newStatus") AuctionStatus newStatus,
            @Param("now") LocalDateTime now
    );
}