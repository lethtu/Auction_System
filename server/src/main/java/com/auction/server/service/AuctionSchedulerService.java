package com.auction.server.service;

import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.repository.AuctionSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.json.JSONObject;
import com.auction.server.socket.SocketServer;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuctionSchedulerService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionSchedulerService.class);

    private final AuctionSessionRepository auctionSessionRepository;

    public AuctionSchedulerService(AuctionSessionRepository auctionSessionRepository) {
        this.auctionSessionRepository = auctionSessionRepository;
    }

    @Transactional
    @Scheduled(fixedRate = 5000)
    public void scanAndUpdateAuctionStatus() {
        LocalDateTime now = LocalDateTime.now();

        // Bước 1: Mở phiên (COMING -> ACTIVE)
        List<AuctionSession> comingSessions = auctionSessionRepository
                .findByStatusAndStartTimeLessThanEqual(AuctionStatus.COMING, now);

        if (!comingSessions.isEmpty()) {
            for (AuctionSession session : comingSessions) {
                session.setStatus(AuctionStatus.ACTIVE);
            }
            auctionSessionRepository.saveAll(comingSessions);
        }

        // Bước 2: Đóng phiên (ACTIVE -> ENDED hoặc CANCELED nếu không đạt min rate)
        List<AuctionSession> activeSessions = auctionSessionRepository
                .findByStatusAndEndTimeLessThanEqual(AuctionStatus.ACTIVE, now);

        if (!activeSessions.isEmpty()) {
            for (AuctionSession session : activeSessions) {
                if (Boolean.TRUE.equals(session.getApplyMinRate()) && session.getMinRate() != null) {
                    if (session.getCurrentPrice() != null && session.getCurrentPrice().compareTo(session.getMinRate()) >= 0) {
                        session.setStatus(AuctionStatus.ENDED);
                    } else {
                        session.setStatus(AuctionStatus.CANCELED);
                        logger.info("Phiên ID {} bị hủy do giá cuối ({}) không đạt min rate ({})",
                                session.getId(), session.getCurrentPrice(), session.getMinRate());
                    }
                } else {
                    session.setStatus(AuctionStatus.ENDED);
                }
            }
            auctionSessionRepository.saveAll(activeSessions);

            // Broadcast event AUCTION_ENDED cho từng phiên vừa đóng
            for (AuctionSession session : activeSessions) {
                try {
                    if (session != null && session.getId() != null) {
                        JSONObject endEvent = new JSONObject();
                        endEvent.put("type", "AUCTION_ENDED");
                        endEvent.put("sessionId", session.getId());
                        SocketServer.broadcastToAll("EVENT:" + endEvent.toString());
                    }
                } catch (Exception e) {
                    logger.error("[SCHEDULER] Lỗi khi broadcast AUCTION_ENDED cho phiên {}: {}",
                            session != null ? session.getId() : "null", e.getMessage());
                }
            }
        }

        // Logging (Chỉ in ra khi có sự thay đổi để tránh rác console)
        if (!comingSessions.isEmpty() || !activeSessions.isEmpty()) {
            logger.info("[SCHEDULER] Lúc " + now +
                    " | Đã mở " + comingSessions.size() + " phiên COMING | Đã đóng " + activeSessions.size() + " phiên.");
        }
    }
}
