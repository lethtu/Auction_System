package com.auction.server.repository;

import com.auction.server.model.AutoBidConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutoBidConfigRepository extends JpaRepository<AutoBidConfig, Integer> {

    /**
     * Lấy tất cả config đang active của 1 phiên, sắp xếp maxBid giảm dần.
     * Top 2 phần tử = Winner [0] và Challenger [1].
     */
    List<AutoBidConfig> findBySessionIdAndActiveTrueOrderByMaxBidDesc(Integer sessionId);

    /**
     * Tìm config hiện có của 1 user trong 1 phiên (để upsert thay vì tạo mới).
     */
    Optional<AutoBidConfig> findBySessionIdAndBidderIdAndActiveTrue(Integer sessionId, Integer bidderId);
}
