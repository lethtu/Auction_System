package com.auction.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.server.dto.BidHistoryDTO;
import com.auction.server.dto.BidResponse;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Bid;
import com.auction.server.model.User;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import com.auction.server.repository.BidRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuctionService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);



    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionSessionRepository auctionSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.auction.server.repository.AutoBidConfigRepository autoBidConfigRepository;

    public List<AuctionSession> getActiveSessions() {
        return auctionSessionRepository.findByStatus(AuctionStatus.ACTIVE)
                .stream()
                .filter(this::isProductVisible)
                .toList();
    }

    public AuctionSession getSessionById(Integer id) {
        AuctionSession session = auctionSessionRepository.findById(id).orElse(null);
        if (session != null) {
            session.setTotalBids(bidRepository.countBySessionId(session.getId()));
        }
        return session;
    }

    public List<AuctionSession> getSessionsBySeller(Integer sellerId) {
        return auctionSessionRepository.findBySeller_Id(sellerId);
    }

    private void finalizeWinnerDeduction(AuctionSession session) {
        if (session.getHighestBidderId() != null && session.getCurrentPrice() != null) {
            User winner = userRepository.findById(session.getHighestBidderId()).orElse(null);
            if (winner != null) {
                winner.setBalance(winner.getBalance().subtract(session.getCurrentPrice()));
                winner.setFrozenBalance(winner.getFrozenBalance().subtract(session.getCurrentPrice()));
                userRepository.save(winner);
                logger.info("Finalized winning bid deduction of {} for Winner ID={}",
                        session.getCurrentPrice(), winner.getId());
            }
        }
    }

    @Transactional
    public boolean endSession(Integer sessionId) {
        Optional<AuctionSession> sessionOpt = auctionSessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            AuctionSession session = sessionOpt.get();
            if (Boolean.TRUE.equals(session.getApplyMinRate()) && session.getMinRate() != null) {
                if (session.getCurrentPrice() != null && session.getCurrentPrice().compareTo(session.getMinRate()) >= 0) {
                    session.setStatus(AuctionStatus.ENDED);
                    finalizeWinnerDeduction(session);
                } else {
                    session.setStatus(AuctionStatus.CANCELED);
                    logger.info("Session ID {} canceled because final price ({}) did not meet min rate ({})",
                            session.getId(), session.getCurrentPrice(), session.getMinRate());

                    // ============ RELEASE FROZEN BALANCE WHEN SESSION IS CANCELED ============
                    if (session.getHighestBidderId() != null) {
                        User topBidder = userRepository.findById(session.getHighestBidderId()).orElse(null);
                        if (topBidder != null && session.getCurrentPrice() != null) {
                            topBidder.setFrozenBalance(topBidder.getFrozenBalance().subtract(session.getCurrentPrice()));
                            userRepository.save(topBidder);
                            logger.info("Released frozen balance {} for User ID={} due to CANCELED session",
                                    session.getCurrentPrice(), topBidder.getId());
                        } else {
                            logger.warn("Cannot release frozen balance: Top Bidder ID={} does not exist in DB",
                                    session.getHighestBidderId());
                        }
                    }
                    // =========================================================================
                }
            } else {
                session.setStatus(AuctionStatus.ENDED);
                finalizeWinnerDeduction(session);
            }
            auctionSessionRepository.save(session);
            logger.info("Auction session ID: {} ended with status: {}", sessionId, session.getStatus());
            return true;
        }
        return false;
    }

    private boolean isProductVisible(AuctionSession session) {
        return session != null && (session.getItem() == null || !session.getItem().isHidden());
    }

    private BigDecimal calculateMinimumNextBid(BigDecimal currentPrice) {
        if (currentPrice == null) return BigDecimal.ZERO;
        
        if (currentPrice.compareTo(new BigDecimal("100000")) < 0) {
            return currentPrice.add(new BigDecimal("10000")); // + 10k
        } else if (currentPrice.compareTo(new BigDecimal("500000")) < 0) {
            return currentPrice.add(new BigDecimal("20000")); // + 20k
        } else if (currentPrice.compareTo(new BigDecimal("1000000")) < 0) {
            return currentPrice.add(new BigDecimal("50000")); // + 50k
        } else if (currentPrice.compareTo(new BigDecimal("5000000")) < 0) {
            return currentPrice.add(new BigDecimal("100000")); // + 100k
        } else {
            return currentPrice.add(new BigDecimal("200000")); // + 200k
        }
    }

    @Transactional
    public BidResponse updateBid(Integer ItemAuctionId, Integer BidderId, BigDecimal newBidAmount) {
        Optional<AuctionSession> itemOptional = auctionSessionRepository.findByIdForUpdate(ItemAuctionId);

        if (itemOptional.isEmpty()) {
            logger.error("Product not found with ID: {}", ItemAuctionId);
            return new BidResponse(false, "ERROR: Product not found with ID: " + ItemAuctionId, new BigDecimal("0"), null, null);
        }

        AuctionSession item = itemOptional.get();
        logger.info("AuctionService.updateBid - Session found: ID={}, status={}", ItemAuctionId, item.getStatus());

        if (!isProductVisible(item)) {
            logger.error("Bid failed: product for session {} is hidden", ItemAuctionId);
            return new BidResponse(false, "Error: This product is currently hidden", BigDecimal.ZERO, null, null);
        }

        BigDecimal currentPrice = item.getCurrentPrice() == null ? BigDecimal.ZERO : item.getCurrentPrice();
        logger.info("Current price for session {}: {}", ItemAuctionId, currentPrice);
        
        BigDecimal minimumRequiredBid;
        if (item.getStepPrice() != null && item.getStepPrice().compareTo(BigDecimal.ZERO) > 0) {
            minimumRequiredBid = currentPrice.add(item.getStepPrice());
        } else {
            minimumRequiredBid = calculateMinimumNextBid(currentPrice);
        }
        logger.info("Minimum required bid for session {}: {}", ItemAuctionId, minimumRequiredBid);

        if (item.getStartTime() != null && LocalDateTime.now().isBefore(item.getStartTime())) {
            logger.error("Bid failed: Auction session {} has not started yet.", ItemAuctionId);
            return new BidResponse(false, "ERROR: This auction session has not started yet!", currentPrice, null);
        }

        if (item.getStatus().equals(AuctionStatus.ACTIVE)){
            if (newBidAmount.compareTo(minimumRequiredBid) < 0) {
                logger.error("Bid failed from UserId: {} with price: {} but system requires minimum: {}", BidderId, newBidAmount, minimumRequiredBid);
                return new BidResponse(
                        false,
                        "FAILED: The next valid bid must be at least " + minimumRequiredBid + " or higher!",
                        currentPrice,
                        null,
                        item.getHighestBidderId()
                );
            }

            User bidder = userRepository.findById(BidderId).orElse(null);
            if (bidder == null) {
                logger.error("Failed: User not found with ID = {}", BidderId);
                return new BidResponse(false, "Error: User account does not exist!", currentPrice, null, item.getHighestBidderId());
            }

            // ============ HOLD BALANCE: Anti Joy-Bidding ============
            // Step 1: Find Old Top Bidder (current highest bidder)
            User oldTopBidder = null;
            BigDecimal oldBidAmount = BigDecimal.ZERO;
            if (item.getHighestBidderId() != null) {
                oldTopBidder = userRepository.findById(item.getHighestBidderId()).orElse(null);
                oldBidAmount = currentPrice; // current price is the amount Old Top bid
            }

            // Step 2: Validate available balance
            BigDecimal available = bidder.getAvailableBalance();
            BigDecimal neededAmount = newBidAmount;
            if (oldTopBidder != null && oldTopBidder.getId().equals(BidderId)) {
                neededAmount = newBidAmount.subtract(oldBidAmount);
            }

            logger.info("Validating balance for Bidder ID={}: balance={}, frozen={}, available={}", 
                    BidderId, bidder.getBalance(), bidder.getFrozenBalance(), available);

            if (available.compareTo(neededAmount) < 0) {
                logger.warn("Bid failed: User ID={} has available balance {} but needs {}",
                        BidderId, available, neededAmount);
                return new BidResponse(false, "Insufficient available balance to place this bid",
                        currentPrice, null, item.getHighestBidderId());
            }

            // Step 3: Freeze New Bidder's balance
            bidder.setFrozenBalance(bidder.getFrozenBalance().add(neededAmount));
            userRepository.save(bidder);

            // Step 4: Release Old Top Bidder's frozen balance (if different from New Bidder)
            if (oldTopBidder != null && !oldTopBidder.getId().equals(BidderId)) {
                oldTopBidder.setFrozenBalance(oldTopBidder.getFrozenBalance().subtract(oldBidAmount));
                userRepository.save(oldTopBidder);
                logger.info("Released frozen bid of {} for Old Top Bidder ID={}", oldBidAmount, oldTopBidder.getId());
            }
            // ========================================================

            LocalDateTime time = LocalDateTime.now();
            item.setCurrentPrice(newBidAmount);
            item.setHighestBidderId(BidderId);

            // ==========================================
            // ANTI-SNIPING ALGORITHM - ARMORED
            // ==========================================
            // ==========================================
            LocalDateTime currentEndTime = item.getEndTime();
            String updatedEndTimeStr = null;

            // NULL CHECK: If endTime is null (due to sloppy test data), skip extension logic
            if (currentEndTime != null) {
                try {
                    long secondsLeft = java.time.Duration.between(time, currentEndTime).getSeconds();

                    // If remaining time is under 60 seconds, automatically add 60 seconds
                    if (secondsLeft < 60 && secondsLeft >= 0) {
                        LocalDateTime newEndTime = currentEndTime.plusSeconds(60);
                        item.setEndTime(newEndTime);
                        updatedEndTimeStr = newEndTime.toString();
                        logger.info("Anti-Sniping: Extended session {} by 60s. New end time: {}", ItemAuctionId, newEndTime);
                    }
                } catch (Exception e) {
                    // Guard against rare time calculation errors to avoid crashing the bidding flow
                    logger.warn("Warning: Anti-sniping calculation error for session {}: {}", ItemAuctionId, e.getMessage());
                }
            }
            // ==========================================

            item.setWinner(bidder); // IMPORTANT: Record the current leader/winner
            Bid bid = new Bid(item, bidder, newBidAmount, time);
            item.addBid(bid);

            try {
                logger.info("Attempting to save bid and update session...");
                bidRepository.save(bid);
                auctionSessionRepository.save(item);
                logger.info("Saved Bid to DB. Updated new price for AuctionItem {} to {} by {}", ItemAuctionId, newBidAmount, BidderId);
                int bidCount = Math.toIntExact(bidRepository.countBySessionId(ItemAuctionId));
                // Return with updated end time (if any) and actual bid count from DB
                return new BidResponse(
                        true,
                        "SUCCESS: You are now leading the auction!",
                        newBidAmount,
                        updatedEndTimeStr,
                        BidderId,
                        bidCount,
                        time.toString(),
                        bid.getId()
                );
            }
            catch (Exception e) {
                logger.error("Error saving to Database: ", e);
                return new BidResponse(false, "SYSTEM ERROR: Could not save data. Please try again.", currentPrice, null, item.getHighestBidderId());
            }
        }
        else {
            // TASK 7: Throw Exception if session has ended or been canceled
            if (item.getStatus() == AuctionStatus.ENDED || item.getStatus() == AuctionStatus.CANCELED) {
                logger.warn("Bid blocked: Session {} is closed.", ItemAuctionId);
                // Ensure package import matches your project
                throw new com.auction.server.exception.AuctionClosedException("Error: This auction session has ended or been canceled!", ItemAuctionId);
            }

            // If session is just PENDING (not open yet), return false normally
            logger.error("Error: Auction session: {} currently has status: {}", ItemAuctionId, item.getStatus());
            return new BidResponse(false, "Error: This session is not open for bidding", currentPrice, null, item.getHighestBidderId());
        }
    }

    // ==========================================
    // AUTO-BID ENGINE O(1)
    // ==========================================

    /**
     * Register or update an Auto-bid configuration in the Database (upsert).
     */
    @Transactional
    public void registerAutoBid(Integer sessionId, Integer bidderId, BigDecimal maxBid, BigDecimal increment) throws Exception {
        User bidder = userRepository.findById(bidderId).orElse(null);
        if (bidder == null) {
            throw new Exception("User account does not exist.");
        }
        BigDecimal available = bidder.getAvailableBalance();
        if (available.compareTo(maxBid) < 0) {
            throw new Exception("Your available balance is insufficient to set this max bid. Please deposit more funds.");
        }

        Optional<com.auction.server.model.AutoBidConfig> existing =
                autoBidConfigRepository.findBySessionIdAndBidderIdAndActiveTrue(sessionId, bidderId);

        com.auction.server.model.AutoBidConfig config;
        if (existing.isPresent()) {
            config = existing.get();
            config.setMaxBid(maxBid);
            config.setIncrement(increment);
            logger.info("AUTO-BID updated: sessionId={}, bidderId={}, maxBid={}, increment={}",
                    sessionId, bidderId, maxBid, increment);
        } else {
            config = new com.auction.server.model.AutoBidConfig(sessionId, bidderId, maxBid, increment);
            logger.info("AUTO-BID registered: sessionId={}, bidderId={}, maxBid={}, increment={}",
                    sessionId, bidderId, maxBid, increment);
        }

        autoBidConfigRepository.save(config);
    }

    /**
     * O(1) Algorithm: Calculate the final price based on Top 2 auto-bidders.
     * Formula: newPrice = min(challenger.maxBid + winner.increment, winner.maxBid)
     *
     * @param sessionId the auction session
     * @return BidResponse if auto-bid succeeds, null if no action needed
     */
    @Transactional
    public BidResponse resolveAutoBids(Integer sessionId) {
        // 1. Get session with pessimistic lock
        Optional<AuctionSession> sessionOpt = auctionSessionRepository.findByIdForUpdate(sessionId);
        if (sessionOpt.isEmpty()) {
            return null;
        }

        AuctionSession session = sessionOpt.get();
        if (!session.getStatus().equals(AuctionStatus.ACTIVE)) {
            return null;
        }

        BigDecimal currentPrice = session.getCurrentPrice() == null ? BigDecimal.ZERO : session.getCurrentPrice();

        // 2. Query Top 2 auto-bid configs (sorted by maxBid DESC)
        List<com.auction.server.model.AutoBidConfig> configs =
                autoBidConfigRepository.findBySessionIdAndActiveTrueOrderByMaxBidDesc(sessionId);

        if (configs.isEmpty()) {
            return null; // No auto-bid configs exist
        }

        com.auction.server.model.AutoBidConfig winner = configs.get(0); // highest maxBid

        // If winner is already the highest bidder -> no auto-bid needed
        if (winner.getBidderId().equals(session.getHighestBidderId())) {
            // Winner is already leading, check if any challenger needs to counter
            if (configs.size() < 2) {
                return null; // Only 1 auto-bidder and already leading
            }
            // Swap: challenger becomes the one who needs to counter
            com.auction.server.model.AutoBidConfig challenger = configs.get(1);
            // Challenger needs to bid currentPrice + challenger.increment
            BigDecimal challengerBid = currentPrice.add(challenger.getIncrement());
            if (challengerBid.compareTo(challenger.getMaxBid()) > 0) {
                // Challenger out of maxBid -> deactivate
                challenger.setActive(false);
                autoBidConfigRepository.save(challenger);
                logger.info("AUTO-BID expired: bidderId={} exceeded maxBid={}", challenger.getBidderId(), challenger.getMaxBid());
                return null;
            }
            // Challenger bids, then winner will counter on next resolve
            // But we calculate O(1) immediately:
            BigDecimal newPrice = challengerBid.min(winner.getMaxBid());
            // Winner needs to counter if challenger bid succeeds
            if (newPrice.compareTo(currentPrice) > 0) {
                BigDecimal finalPrice = newPrice.add(winner.getIncrement()).min(winner.getMaxBid());
                if (finalPrice.compareTo(newPrice) > 0) {
                    // Winner counters successfully
                    return executeAutoBid(session, winner, configs, finalPrice, currentPrice);
                } else {
                    // Winner out of room -> challenger wins at newPrice
                    return executeAutoBid(session, challenger, configs, newPrice, currentPrice);
                }
            }
            return null;
        }

        // 3. Determine Challenger
        BigDecimal challengerMax;
        if (configs.size() >= 2) {
            challengerMax = configs.get(1).getMaxBid(); // Second highest maxBid
        } else {
            challengerMax = currentPrice; // No opponent -> current price
        }

        // 4. O(1) FORMULA: Calculate final price
        BigDecimal newPrice = challengerMax.add(winner.getIncrement()).min(winner.getMaxBid());

        // 5. Ensure newPrice > currentPrice
        if (newPrice.compareTo(currentPrice) <= 0) {
            return null;
        }

        // Ensure newPrice >= currentPrice + stepPrice (meets minimum step price)
        BigDecimal stepPrice = session.getStepPrice() == null ? BigDecimal.ONE : session.getStepPrice();
        BigDecimal minimumBid = currentPrice.add(stepPrice);
        if (newPrice.compareTo(minimumBid) < 0) {
            newPrice = minimumBid.min(winner.getMaxBid());
            if (newPrice.compareTo(minimumBid) < 0) {
                return null; // Winner doesn't have enough maxBid for minimum step price
            }
        }

        return executeAutoBid(session, winner, configs, newPrice, currentPrice);
    }

    /**
     * Execute auto-bid: update session, save bid to DB, deactivate expired configs.
     */
    private BidResponse executeAutoBid(
            AuctionSession session,
            com.auction.server.model.AutoBidConfig winner,
            List<com.auction.server.model.AutoBidConfig> allConfigs,
            BigDecimal newPrice,
            BigDecimal previousPrice
    ) {
        // Check user exists
        User bidder = userRepository.findById(winner.getBidderId()).orElse(null);
        if (bidder == null) {
            logger.error("AUTO-BID: User ID={} not found", winner.getBidderId());
            return null;
        }

        // Find Old Top Bidder
        User oldTopBidder = null;
        BigDecimal oldBidAmount = BigDecimal.ZERO;
        if (session.getHighestBidderId() != null) {
            oldTopBidder = userRepository.findById(session.getHighestBidderId()).orElse(null);
            oldBidAmount = previousPrice;
        }

        // ============ HOLD BALANCE: Auto-bid Anti Joy-Bidding ============
        // Validate auto-bidder available balance
        BigDecimal available = bidder.getAvailableBalance();
        BigDecimal neededAmount = newPrice;
        if (oldTopBidder != null && oldTopBidder.getId().equals(winner.getBidderId())) {
            neededAmount = newPrice.subtract(oldBidAmount);
        }

        if (available.compareTo(neededAmount) < 0) {
            logger.warn("AUTO-BID: User ID={} insufficient available balance ({}) for auto-bid needed ({})",
                    winner.getBidderId(), available, neededAmount);
            winner.setActive(false);
            autoBidConfigRepository.save(winner);
            return null;
        }

        // Freeze auto-bidder balance (add to frozenBalance)
        bidder.setFrozenBalance(bidder.getFrozenBalance().add(neededAmount));
        userRepository.save(bidder);

        // Release Old Top Bidder's frozen balance (if different from auto-bidder)
        if (oldTopBidder != null && !oldTopBidder.getId().equals(winner.getBidderId())) {
            oldTopBidder.setFrozenBalance(oldTopBidder.getFrozenBalance().subtract(oldBidAmount));
            userRepository.save(oldTopBidder);
            logger.info("AUTO-BID: Released frozen bid of {} for Old Top Bidder ID={}", oldBidAmount, oldTopBidder.getId());
        }
        // ==================================================================

        LocalDateTime time = LocalDateTime.now();
        session.setCurrentPrice(newPrice);
        session.setHighestBidderId(winner.getBidderId());

        // Anti-sniping
        String updatedEndTimeStr = null;
        LocalDateTime currentEndTime = session.getEndTime();
        if (currentEndTime != null) {
            try {
                long secondsLeft = java.time.Duration.between(time, currentEndTime).getSeconds();
                if (secondsLeft < 60 && secondsLeft >= 0) {
                    LocalDateTime newEndTime = currentEndTime.plusSeconds(60);
                    session.setEndTime(newEndTime);
                    updatedEndTimeStr = newEndTime.toString();
                    logger.info("AUTO-BID Anti-Sniping: Extended session {} by 60s", session.getId());
                }
            } catch (Exception e) {
                logger.warn("AUTO-BID: Anti-sniping calculation error: {}", e.getMessage());
            }
        }

        // Save Bid to DB
        Bid bid = new Bid(session, bidder, newPrice, time);
        session.addBid(bid);

        try {
            bidRepository.save(bid);
            auctionSessionRepository.save(session);

            int bidCount = Math.toIntExact(bidRepository.countBySessionId(session.getId()));

            // Deactivate configs that have exceeded maxBid
            for (com.auction.server.model.AutoBidConfig cfg : allConfigs) {
                if (newPrice.compareTo(cfg.getMaxBid()) >= 0 && !cfg.getId().equals(winner.getId())) {
                    cfg.setActive(false);
                    autoBidConfigRepository.save(cfg);
                    logger.info("AUTO-BID deactivated: bidderId={} exceeded maxBid={}",
                            cfg.getBidderId(), cfg.getMaxBid());
                }
            }

            logger.info("AUTO-BID O(1) successful: sessionId={}, winner={}, newPrice={}",
                    session.getId(), winner.getBidderId(), newPrice);

            return new BidResponse(
                    true,
                    "Auto-bid: Price has been automatically raised!",
                    newPrice,
                    updatedEndTimeStr,
                    winner.getBidderId(),
                    bidCount,
                    time.toString(),
                    bid.getId()
            );
        } catch (Exception e) {
            logger.error("AUTO-BID: Error saving to DB", e);
            return null;
        }
    }

    // ==========================================
    // BID HISTORY (CHART DATA)
    // ==========================================

    /**
     * Get bid history of a session, sorted by time ascending.
     * Used for REST endpoint GET /api/auctions/{id}/bid-history.
     */
    public List<BidHistoryDTO> getBidHistory(Integer sessionId) {
        List<Bid> bids = bidRepository.findBySessionIdOrderByTimeAsc(sessionId);
        return bids.stream().map(this::toBidHistoryDTO).toList();
    }

    private BidHistoryDTO toBidHistoryDTO(Bid bid) {
        Integer sessionId = bid.getSession() != null ? bid.getSession().getId() : null;
        Integer bidderId = bid.getBidder() != null ? bid.getBidder().getId() : null;
        String maskedCode = generateMaskedCode(sessionId, bidderId);

        return new BidHistoryDTO(
                bid.getId(),
                sessionId,
                bidderId,
                maskedCode,
                bid.getAmount(),
                bid.getTime() != null ? bid.getTime().toString() : null
        );
    }

    /**
     * Generate anonymous bidder code in #XXXX format.
     * Same bidder + same session = same code.
     * Different sessions may have different codes (due to sessionId in hash).
     */
    private String generateMaskedCode(Integer sessionId, Integer bidderId) {
        if (bidderId == null) return "#????";
        int hash = java.util.Objects.hash(sessionId, bidderId, "BidPop");
        return String.format("#%04X", Math.abs(hash) % 0xFFFF);
    }
}