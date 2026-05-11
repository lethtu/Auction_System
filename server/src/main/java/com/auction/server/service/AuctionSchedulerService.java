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

        List<AuctionSession> pendingSessions = auctionSessionRepository
                .findByStatusAndStartTimeLessThanEqual(AuctionStatus.PENDING, now);

        for (AuctionSession session : pendingSessions) {
            session.setStatus(AuctionStatus.ACTIVE);
        }

        if (!pendingSessions.isEmpty()) {
            auctionSessionRepository.saveAll(pendingSessions);
        }

        List<AuctionSession> activeSessions = auctionSessionRepository
                .findByStatusAndEndTimeLessThanEqual(AuctionStatus.ACTIVE, now);

        for (AuctionSession session : activeSessions) {
            session.setStatus(AuctionStatus.ENDED);
        }

        if (!activeSessions.isEmpty()) {
            auctionSessionRepository.saveAll(activeSessions);
        }

        if (!pendingSessions.isEmpty() || !activeSessions.isEmpty()) {
            logger.info(
                    "[SCHEDULER] Lúc {} | Đã mở {} phiên | Đã đóng {} phiên.",
                    now,
                    pendingSessions.size(),
                    activeSessions.size()
            );
        }
    }
}