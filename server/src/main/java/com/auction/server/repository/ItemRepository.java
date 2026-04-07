package com.auction.server.repository;

import com.auction.server.model.Item; // Đảm bảo em đã đổi tên class thành Item viết hoa
import com.auction.server.model.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {

    // Hàm cũ của em
    List<Item> findByStatus(String status);

    // 1. Cập nhật trạng thái PENDING -> ACTIVE
    @Modifying
    @Query("UPDATE Item i SET i.status = :newStatus WHERE i.status = :oldStatus AND i.startTime <= :now")
    int updateStatusToActive(
            @Param("oldStatus") AuctionStatus oldStatus,
            @Param("newStatus") AuctionStatus newStatus,
            @Param("now") LocalDateTime now
    );

    // 2. Cập nhật trạng thái ACTIVE -> ENDED
    @Modifying
    @Query("UPDATE Item i SET i.status = :newStatus WHERE i.status = :oldStatus AND i.endTime <= :now")
    int updateStatusToEnded(
            @Param("oldStatus") AuctionStatus oldStatus,
            @Param("newStatus") AuctionStatus newStatus,
            @Param("now") LocalDateTime now
    );
}