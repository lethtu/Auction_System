package com.auction.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.server.model.AuctionStatus;
import com.auction.server.repository.AuctionSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuctionSchedulerService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionSchedulerService.class);
    @Autowired
    private AuctionSessionRepository auctionSessionRepository;

    /**
     * Hàm này sẽ tự động chạy lặp lại mỗi 5 giây.
     * @Transactional là BẮT BUỘC vì chúng ta đang thực hiện lệnh @Modifying (UPDATE) vào Database.
     */
    @Transactional
    @Scheduled(fixedRate = 5000) // Quét 5 giây 1 lần
    public void scanAndUpdateAuctionStatus() {
        LocalDateTime now = LocalDateTime.now();

        // Bước 1: Mở phiên (PENDING -> ACTIVE)
        int activatedCount = auctionSessionRepository.updateStatusToActive(
                AuctionStatus.PENDING,
                AuctionStatus.ACTIVE,
                now
        );

        // Bước 2: Đóng phiên (ACTIVE -> ENDED)
        int endedCount = auctionSessionRepository.updateStatusToEnded(
                AuctionStatus.ACTIVE,
                AuctionStatus.ENDED,
                now
        );

        // In ra log để theo dõi (chỉ in khi có sự thay đổi để tránh rác console)
        if (activatedCount > 0 || endedCount > 0) {
            logger.info("[SCHEDULER] Lúc " + now +
                    " | Đã mở " + activatedCount + " phiên | Đã đóng " + endedCount + " phiên.");
        }
    }
}