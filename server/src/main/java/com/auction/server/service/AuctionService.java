package com.auction.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.server.dto.BidHistoryDTO;
import com.auction.server.dto.BidResponse;
import com.auction.server.dto.DeliveryInfoRequest;
import com.auction.server.exception.AuctionClosedException;
import com.auction.server.exception.BusinessException;
import com.auction.server.exception.InvalidBidException;
import com.auction.server.exception.PermissionDeniedException;
import com.auction.server.exception.ResourceNotFoundException;
import com.auction.server.exception.ValidationException;
import com.auction.server.model.AutoBidConfig;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Bid;
import com.auction.server.model.User;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.AutoBidConfigRepository;
import com.auction.server.repository.UserRepository;
import com.auction.server.repository.BidRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuctionService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);
    private static final BigDecimal DEFAULT_BID_INCREMENT = new BigDecimal("10000");

    private final BidRepository bidRepository;
    private final AuctionSessionRepository auctionSessionRepository;
    private final UserRepository userRepository;
    private final AutoBidConfigRepository autoBidConfigRepository;

    public AuctionService(
            BidRepository bidRepository,
            AuctionSessionRepository auctionSessionRepository,
            UserRepository userRepository,
            AutoBidConfigRepository autoBidConfigRepository) {
        this.bidRepository = bidRepository;
        this.auctionSessionRepository = auctionSessionRepository;
        this.userRepository = userRepository;
        this.autoBidConfigRepository = autoBidConfigRepository;
    }

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

    private boolean isProductVisible(AuctionSession session) {
        return session != null && session.getItem() != null && !session.getItem().isHidden();
    }

    @Transactional
    public void saveDeliveryInfo(Integer sessionId, Integer winnerId, DeliveryInfoRequest request) {
        if (request == null || isBlank(request.getRecipientName()) || isBlank(request.getPhoneNumber())
                || isBlank(request.getAddress())) {
            throw new ValidationException("Recipient, phone number and address are required.");
        }

        AuctionSession session = auctionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction session not found."));
        if (session.getStatus() != AuctionStatus.ENDED) {
            throw new BusinessException("Delivery details can be submitted only after the auction has ended.");
        }
        Integer authoritativeWinnerId = getWinningBid(session.getId())
                .map(bid -> bid.getBidder() == null ? null : bid.getBidder().getId())
                .orElse(session.getHighestBidderId());
        if (winnerId == null || !winnerId.equals(authoritativeWinnerId)) {
            throw new PermissionDeniedException("Only the auction winner can submit delivery information.");
        }

        session.setDeliveryRecipient(trimToLength(request.getRecipientName(), 150));
        session.setDeliveryPhone(trimToLength(request.getPhoneNumber(), 30));
        session.setDeliveryAddress(trimToLength(request.getAddress(), 500));
        session.setDeliveryNote(trimToLength(request.getNote(), 500));
        session.setDeliverySubmittedAt(LocalDateTime.now());
        auctionSessionRepository.save(session);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private void finalizeWinnerDeduction(AuctionSession session) {
        if (session.getHighestBidderId() != null && session.getCurrentPrice() != null) {
            BigDecimal winningPrice = session.getCurrentPrice();

            // Step 1: Deduct from winner's balance and release frozen balance
            User winner = userRepository.findById(session.getHighestBidderId()).orElse(null);
            if (winner != null) {
                winner.setBalance(winner.getBalance().subtract(winningPrice));
                winner.setFrozenBalance(winner.getFrozenBalance().subtract(winningPrice));
                userRepository.save(winner);
                logger.info("Finalized winning bid deduction of {} for Winner ID={}",
                        winningPrice, winner.getId());
            }

            // Step 2: Credit seller's balance with the winning price
            if (session.getSeller() != null) {
                User seller = userRepository.findById(session.getSeller().getId()).orElse(null);
                if (seller != null) {
                    seller.setBalance(seller.getBalance().add(winningPrice));
                    userRepository.save(seller);
                    logger.info("Credited seller ID={} with winning amount {}",
                            seller.getId(), winningPrice);
                } else {
                    logger.warn("Cannot credit seller: Seller ID={} not found in DB",
                            session.getSeller().getId());
                }
            } else {
                logger.warn("Cannot credit seller: Session ID={} has no seller assigned",
                        session.getId());
            }
        }
    }

    @Transactional
    public boolean endSession(Integer sessionId) {
        Optional<AuctionSession> sessionOpt = auctionSessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            AuctionSession session = sessionOpt.get();
            reconcileSessionResultFromBids(session);

            BigDecimal finalPrice = session.getCurrentPrice();
            BigDecimal reservePrice = session.getReservePrice();
            boolean reserveMet = true;
            if (reservePrice != null && reservePrice.compareTo(BigDecimal.ZERO) > 0) {
                if (finalPrice == null || finalPrice.compareTo(reservePrice) < 0) {
                    reserveMet = false;
                }
            }

            // Always end the session first
            session.setStatus(AuctionStatus.ENDED);

            if (session.getHighestBidderId() != null && finalPrice != null) {
                if (reserveMet) {
                    session.setStatus(AuctionStatus.PAID);
                    finalizeWinnerDeduction(session);
                    logger.info("Session ID {} ended as PAID because final price ({}) met reserve ({})",
                            session.getId(), finalPrice, reservePrice);
                } else {
                    // Reserve price NOT met: keep status as ENDED, but release the highest bidder's frozen balance
                    User topBidder = userRepository.findById(session.getHighestBidderId()).orElse(null);
                    if (topBidder != null) {
                        topBidder.setFrozenBalance(topBidder.getFrozenBalance().subtract(finalPrice));
                        userRepository.save(topBidder);
                        logger.info("Released frozen balance {} for User ID={} because reserve price ({}) was not met (final price: {})",
                                finalPrice, topBidder.getId(), reservePrice, finalPrice);
                    }
                }
            } else {
                logger.info("Session ID {} ended with no bids.", session.getId());
            }

            auctionSessionRepository.save(session);
            logger.info("Auction session ID: {} ended with status: {}", sessionId, session.getStatus());
            return true;
        }
        return false;
    }

    @Transactional
    public BidResponse updateBid(Integer sessionId, Integer bidderId, BigDecimal bidAmount) {
        AuctionSession session = auctionSessionRepository.findByIdForUpdate(sessionId).orElse(null);
        if (session == null) {
            return BidResponse.failure("Auction session not found.", BigDecimal.ZERO);
        }

        BigDecimal currentPrice = session.getCurrentPrice() == null
                ? BigDecimal.ZERO
                : session.getCurrentPrice();
        logger.info("AuctionService.updateBid - Session found: ID={}, status={}", sessionId, session.getStatus());
        logger.info("Current price for session {}: {}", sessionId, currentPrice);

        BigDecimal stepPrice = getEffectiveBidIncrement(session, currentPrice);
        BigDecimal minimumBid = currentPrice.add(stepPrice);
        logger.info("Minimum required bid for session {}: {}", sessionId, minimumBid);

        if (session.getStatus() == AuctionStatus.ENDED || session.getStatus() == AuctionStatus.CANCELED) {
            logger.warn("Bid blocked: Session {} is closed.", sessionId);
            throw new AuctionClosedException(
                    session.getStatus() == AuctionStatus.CANCELED
                            ? "This auction has been canceled."
                            : "This auction has ended.",
                    sessionId);
        }
        if (session.getStatus() != AuctionStatus.ACTIVE) {
            return BidResponse.failure("Auction is not open for bidding.", currentPrice);
        }
        if (isSessionSeller(session, bidderId)) {
            logger.warn("Bid blocked: Seller {} attempted to bid on their own session {}.", bidderId, sessionId);
            return BidResponse.failure("Sellers cannot bid on their own auction.", currentPrice);
        }
        if (bidAmount == null || bidAmount.compareTo(minimumBid) < 0) {
            logger.error("Bid failed from UserId: {} with price: {} but system requires minimum: {}",
                    bidderId, bidAmount, minimumBid);
            return BidResponse.failure("Bid must be at least " + minimumBid + ".", currentPrice);
        }

        User bidder = userRepository.findById(bidderId).orElse(null);
        if (bidder == null) {
            logger.error("Failed: User not found with ID = {}", bidderId);
            return BidResponse.failure("Bidder not found.", currentPrice);
        }

        Integer previousHighestBidderId = session.getHighestBidderId();
        boolean bidderAlreadyHighest = bidderId != null && bidderId.equals(previousHighestBidderId);
        BigDecimal amountToFreeze = bidderAlreadyHighest ? bidAmount.subtract(currentPrice) : bidAmount;
        logger.info("Validating balance for Bidder ID={}: balance={}, frozen={}, available={}",
                bidderId, bidder.getBalance(), bidder.getFrozenBalance(), bidder.getAvailableBalance());
        if (bidder.getAvailableBalance().compareTo(amountToFreeze) < 0) {
            return BidResponse.failure("Insufficient available balance.", currentPrice);
        }

        if (!bidderAlreadyHighest && previousHighestBidderId != null) {
            User previousBidder = userRepository.findById(previousHighestBidderId).orElse(null);
            if (previousBidder != null) {
                previousBidder.setFrozenBalance(previousBidder.getFrozenBalance().subtract(currentPrice));
                userRepository.save(previousBidder);
            }
        }
        bidder.setFrozenBalance(bidder.getFrozenBalance().add(amountToFreeze));
        userRepository.save(bidder);

        LocalDateTime bidTime = LocalDateTime.now();
        String updatedEndTime = extendEndTimeIfNeeded(session, bidTime, "Anti-Sniping");
        session.setCurrentPrice(bidAmount);
        session.setHighestBidderId(bidderId);

        Bid bid = new Bid(session, bidder, bidAmount, bidTime);
        session.addBid(bid);
        logger.info("Attempting to save bid and update session...");
        bidRepository.save(bid);
        auctionSessionRepository.save(session);
        int bidCount = Math.toIntExact(bidRepository.countBySessionId(sessionId));
        logger.info("Saved Bid to DB. Updated new price for AuctionItem {} to {} by {}",
                sessionId, bidAmount, bidderId);

        return new BidResponse(true, "Bid placed successfully.", bidAmount, updatedEndTime,
                bidderId, bidCount, bidTime.toString(), bid.getId(), previousHighestBidderId);
    }

    @Transactional
    public void registerAutoBid(Integer sessionId, Integer bidderId, BigDecimal maxBid, BigDecimal increment) {
        if (sessionId == null || bidderId == null || maxBid == null || increment == null
                || maxBid.signum() <= 0 || increment.signum() <= 0) {
            throw new ValidationException("Auto-bid amount and increment must be positive.");
        }
        AuctionSession session = auctionSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction session not found."));
        if (session.getStatus() != AuctionStatus.ACTIVE) {
            throw new BusinessException("Auto-bid is available only for active auctions.");
        }
        if (isSessionSeller(session, bidderId)) {
            throw new InvalidBidException("Sellers cannot enable auto-bid on their own auction.");
        }
        BigDecimal currentPrice = session.getCurrentPrice() == null ? BigDecimal.ZERO : session.getCurrentPrice();
        if (maxBid.compareTo(currentPrice) <= 0) {
            throw new InvalidBidException("Maximum bid must be higher than the current price.");
        }
        if (userRepository.findById(bidderId).isEmpty()) {
            throw new ResourceNotFoundException("Bidder not found.");
        }

        AutoBidConfig config = autoBidConfigRepository
                .findBySessionIdAndBidderIdAndActiveTrue(sessionId, bidderId)
                .orElseGet(() -> new AutoBidConfig(sessionId, bidderId, maxBid, increment));
        config.setMaxBid(maxBid);
        config.setIncrement(increment);
        config.setActive(true);
        autoBidConfigRepository.save(config);
    }

    @Transactional
    public BidResponse resolveAutoBids(Integer sessionId) {
        AuctionSession session = auctionSessionRepository.findByIdForUpdate(sessionId).orElse(null);
        if (session == null || session.getStatus() != AuctionStatus.ACTIVE) {
            return null;
        }
        List<AutoBidConfig> configs = autoBidConfigRepository
                .findBySessionIdAndActiveTrueOrderByMaxBidDesc(sessionId);
        if (configs == null || configs.isEmpty()) {
            return null;
        }

        AutoBidConfig winner = null;
        for (AutoBidConfig config : configs) {
            if (isSessionSeller(session, config.getBidderId())) {
                config.setActive(false);
                autoBidConfigRepository.save(config);
                logger.warn("AUTO-BID blocked: seller {} had an active config on session {}.",
                        config.getBidderId(), sessionId);
                continue;
            }
            winner = config;
            break;
        }
        if (winner == null) {
            return null;
        }
        if (winner.getBidderId().equals(session.getHighestBidderId())) {
            return null;
        }

        BigDecimal currentPrice = session.getCurrentPrice() == null ? BigDecimal.ZERO : session.getCurrentPrice();
        BigDecimal increment = winner.getIncrement() == null || winner.getIncrement().signum() <= 0
                ? DEFAULT_BID_INCREMENT
                : winner.getIncrement();
        increment = increment.max(getEffectiveBidIncrement(session, currentPrice));
        BigDecimal newPrice = currentPrice.add(increment);
        if (newPrice.compareTo(winner.getMaxBid()) > 0) {
            winner.setActive(false);
            autoBidConfigRepository.save(winner);
            return null;
        }
        return executeAutoBid(session, winner, configs, newPrice, currentPrice);
    }

    private String extendEndTimeIfNeeded(AuctionSession session, LocalDateTime bidTime, String source) {
        LocalDateTime currentEndTime = session.getEndTime();
        if (currentEndTime == null) {
            return null;
        }
        long secondsLeft = java.time.Duration.between(bidTime, currentEndTime).getSeconds();
        if (secondsLeft >= 0 && secondsLeft < 60) {
            LocalDateTime newEndTime = currentEndTime.plusSeconds(60);
            session.setEndTime(newEndTime);
            logger.info("{}: Extended session {} by 60s. New end time: {}", source, session.getId(), newEndTime);
            return newEndTime.toString();
        }
        return null;
    }

    private boolean isSessionSeller(AuctionSession session, Integer bidderId) {
        return session != null
                && session.getSeller() != null
                && bidderId != null
                && bidderId.equals(session.getSeller().getId());
    }

    private BigDecimal getEffectiveBidIncrement(AuctionSession session, BigDecimal currentPrice) {
        BigDecimal configuredIncrement = session != null && session.getStepPrice() != null
                && session.getStepPrice().signum() > 0
                ? session.getStepPrice()
                : DEFAULT_BID_INCREMENT;
        return configuredIncrement.max(getDynamicBidIncrement(currentPrice));
    }

    private BigDecimal getDynamicBidIncrement(BigDecimal currentPrice) {
        if (currentPrice == null || currentPrice.compareTo(new BigDecimal("100000")) < 0) {
            return new BigDecimal("10000");
        }
        if (currentPrice.compareTo(new BigDecimal("500000")) < 0) {
            return new BigDecimal("20000");
        }
        if (currentPrice.compareTo(new BigDecimal("1000000")) < 0) {
            return new BigDecimal("50000");
        }
        if (currentPrice.compareTo(new BigDecimal("5000000")) < 0) {
            return new BigDecimal("100000");
        }
        if (currentPrice.compareTo(new BigDecimal("10000000")) < 0) {
            return new BigDecimal("200000");
        }
        if (currentPrice.compareTo(new BigDecimal("50000000")) < 0) {
            return new BigDecimal("500000");
        }
        return new BigDecimal("1000000");
    }

    private Optional<Bid> getWinningBid(Integer sessionId) {
        if (sessionId == null) {
            return Optional.empty();
        }
        return bidRepository.findWinningBidsForSessions(List.of(sessionId)).stream().findFirst();
    }

    private void reconcileSessionResultFromBids(AuctionSession session) {
        getWinningBid(session.getId()).ifPresent(winningBid -> {
            User winner = winningBid.getBidder();
            if (winner == null) {
                return;
            }
            session.setCurrentPrice(winningBid.getAmount());
            session.setHighestBidderId(winner.getId());
            session.setWinner(winner);
        });
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
        if (isSessionSeller(session, winner.getBidderId())) {
            winner.setActive(false);
            autoBidConfigRepository.save(winner);
            logger.warn("AUTO-BID blocked: seller {} attempted to bid on their own session {}.",
                    winner.getBidderId(), session.getId());
            return null;
        }

        // Check user exists
        User bidder = userRepository.findById(winner.getBidderId()).orElse(null);
        if (bidder == null) {
            logger.error("AUTO-BID: User ID={} not found", winner.getBidderId());
            return null;
        }

        Integer previousHighestBidderId = session.getHighestBidderId();

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
        String updatedEndTimeStr = extendEndTimeIfNeeded(session, time, "AUTO-BID Anti-Sniping");

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
                    bid.getId(),
                    previousHighestBidderId
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
