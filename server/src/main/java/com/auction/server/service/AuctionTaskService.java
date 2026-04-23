package com.auction.server.service;

import com.auction.server.model.AuctionStatus;
import com.auction.server.repository.AuctionSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuctionTaskService {

    @Autowired
    private AuctionSessionRepository sessionRepository;

    // Chạy mỗi 1 phút để quét các phiên hết hạn
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void closeExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();

        // Dùng luôn hàm Bulk Update đã có trong Repository: Chuyển ACTIVE -> ENDED
        int count = sessionRepository.updateStatusToEnded(
                AuctionStatus.ACTIVE,
                AuctionStatus.ENDED,
                now
        );

        if (count > 0) {
            System.out.println("🤖 Robot: Đã đóng tự động " + count + " phiên đấu giá hết hạn.");
        }
    }
}