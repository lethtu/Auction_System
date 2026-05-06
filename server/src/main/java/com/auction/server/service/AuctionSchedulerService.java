package com.auction.server.service;

import com.auction.server.model.AuctionStatus;
import com.auction.server.repository.AuctionSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

        int endedCount = auctionSessionRepository.updateStatusToEnded(
                AuctionStatus.ACTIVE,
                AuctionStatus.ENDED,
                now
        );

        if (endedCount > 0) {
            logger.info("[SCHEDULER] Lúc {} | Đã đóng {} phiên.", now, endedCount);
        }
    }
}