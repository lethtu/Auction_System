package com.auction.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.repository.AuctionSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuctionSchedulerService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionSchedulerService.class);

    @Autowired
    private AuctionSessionRepository auctionSessionRepository;

    /**
     * Hàm này sẽ tự động chạy lặp lại mỗi 5 giây.
     * @Transactional đảm bảo tính toàn vẹn của Transaction khi kéo dữ liệu và lưu lại.
     */
    @Transactional
    @Scheduled(fixedRate = 5000) // Quét 5 giây 1 lần
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

        // Bước 2: Đóng phiên (ACTIVE -> ENDED)
        List<AuctionSession> activeSessions = auctionSessionRepository
                .findByStatusAndEndTimeLessThanEqual(AuctionStatus.ACTIVE, now);

        if (!activeSessions.isEmpty()) {
            for (AuctionSession session : activeSessions) {
                session.setStatus(AuctionStatus.ENDED);
            }
            auctionSessionRepository.saveAll(activeSessions);
        }

        // Logging (Chỉ in ra khi có sự thay đổi để tránh rác console)
        if (!pendingSessions.isEmpty() || !activeSessions.isEmpty()) {
            logger.info("[SCHEDULER] Lúc " + now +
                    " | Đã mở " + pendingSessions.size() + " phiên | Đã đóng " + activeSessions.size() + " phiên.");
        }
    }
}