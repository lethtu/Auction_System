package com.auction.server.repository;

import com.auction.server.model.AutoBidConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutoBidConfigRepository extends JpaRepository<AutoBidConfig, Integer> {

    /**
     * Get all active configs for a session, sorted by maxBid descending.
     * Top 2 elements = Winner [0] and Challenger [1].
     */
    List<AutoBidConfig> findBySessionIdAndActiveTrueOrderByMaxBidDesc(Integer sessionId);

    /**
     * Find existing config for a user in a session (for upsert instead of creating new).
     */
    Optional<AutoBidConfig> findBySessionIdAndBidderIdAndActiveTrue(Integer sessionId, Integer bidderId);
}
