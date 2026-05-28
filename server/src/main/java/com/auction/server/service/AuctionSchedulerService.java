package com.auction.server.service;

import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.repository.AuctionSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.json.JSONObject;
import com.auction.server.socket.SocketServer;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuctionSchedulerService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionSchedulerService.class);

    private final AuctionSessionRepository auctionSessionRepository;
    private final AuctionService auctionService;

    public AuctionSchedulerService(AuctionSessionRepository auctionSessionRepository,
                                   AuctionService auctionService) {
        this.auctionSessionRepository = auctionSessionRepository;
        this.auctionService = auctionService;
    }

    @Scheduled(fixedDelay = 5000)
    public void scanAndUpdateAuctionStatus() {
        LocalDateTime now = LocalDateTime.now();

        // Step 1: Open sessions (COMING -> ACTIVE)
        List<AuctionSession> comingSessions = auctionSessionRepository
                .findByStatusAndStartTimeLessThanEqual(AuctionStatus.COMING, now);

        if (!comingSessions.isEmpty()) {
            for (AuctionSession session : comingSessions) {
                session.setStatus(AuctionStatus.ACTIVE);
            }
            auctionSessionRepository.saveAll(comingSessions);
        }

        // Step 2: Close sessions (ACTIVE -> ENDED or CANCELED if min rate not met)
        // Use AuctionService.endSession() to properly handle winner deduction + seller credit
        List<AuctionSession> activeSessions = auctionSessionRepository
                .findByStatusAndEndTimeLessThanEqual(AuctionStatus.ACTIVE, now);

        if (!activeSessions.isEmpty()) {
            for (AuctionSession session : activeSessions) {
                try {
                    auctionService.endSession(session.getId());
                } catch (Exception e) {
                    logger.error("[SCHEDULER] Error ending session {}: {}", session.getId(), e.getMessage(), e);
                }
            }

            // Broadcast AUCTION_ENDED event for each newly closed session
            for (AuctionSession session : activeSessions) {
                try {
                    // Re-fetch to get latest status
                    AuctionSession updated = auctionSessionRepository.findById(session.getId()).orElse(null);
                    if (updated != null && updated.getId() != null) {
                        JSONObject endEvent = new JSONObject();
                        endEvent.put("type", "AUCTION_ENDED");
                        endEvent.put("sessionId", updated.getId());
                        endEvent.put("finalStatus", updated.getStatus().name());
                        boolean hasWinner = updated.getStatus() == AuctionStatus.PAID
                                && updated.getHighestBidderId() != null;
                        endEvent.put("hasWinner", hasWinner);
                        if (hasWinner) {
                            endEvent.put("winnerId", updated.getHighestBidderId());
                            endEvent.put("highestBidderId", updated.getHighestBidderId());
                        }
                        if (updated.getCurrentPrice() != null) {
                            endEvent.put("finalPrice", updated.getCurrentPrice());
                        }
                        if (updated.getItem() != null) {
                            endEvent.put("itemName", updated.getItem().getName());
                        }
                        SocketServer.broadcastToAll("EVENT:" + endEvent.toString());
                    }
                } catch (Exception e) {
                    logger.error("[SCHEDULER] Error broadcasting AUCTION_ENDED for session {}: {}",
                            session != null ? session.getId() : "null", e.getMessage());
                }
            }
        }

        // Logging (Only print when there are changes to avoid console clutter)
        if (!comingSessions.isEmpty() || !activeSessions.isEmpty()) {
            logger.info("[SCHEDULER] At " + now +
                    " | Opened " + comingSessions.size() + " COMING sessions | Closed " + activeSessions.size() + " sessions.");
        }
    }
}
