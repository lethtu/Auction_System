package com.auction.server.service;

import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.repository.AuctionSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // Bước 1: Mở phiên (PENDING -> ACTIVE)
        List<AuctionSession> pendingSessions = auctionSessionRepository
                .findByStatusAndStartTimeLessThanEqual(AuctionStatus.PENDING, now);

        if (!pendingSessions.isEmpty()) {
            for (AuctionSession session : pendingSessions) {
                session.setStatus(AuctionStatus.ACTIVE);
            }
            auctionSessionRepository.saveAll(pendingSessions);
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
            logger.info(
                    "[SCHEDULER] Lúc {} | Đã đóng {} phiên ACTIVE quá hạn.",
                    now,
                    activeSessions.size()
            );
        }
    }
}
