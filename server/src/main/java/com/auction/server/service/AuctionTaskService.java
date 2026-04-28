package com.auction.server.service;

<<<<<<< HEAD
import com.auction.server.model.AuctionSession;
=======
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.server.model.AuctionStatus;
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
import com.auction.server.repository.AuctionSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
<<<<<<< HEAD
import java.util.List;

@Service
public class AuctionTaskService {

    @Autowired
    private AuctionSessionRepository sessionRepository;

=======

@Service
public class AuctionTaskService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);
    @Autowired
    private AuctionSessionRepository sessionRepository;

    // Chạy mỗi 1 phút để quét các phiên hết hạn
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void closeExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
<<<<<<< HEAD
        List<AuctionSession> expiredSessions = sessionRepository.findActiveExpiredSessions(now);

        for (AuctionSession session : expiredSessions) {
            session.setStatus("FINISHED");
            sessionRepository.save(session);
            System.out.println("🤖 Closed Auction Session ID: " + session.getId());
=======

        // Dùng luôn hàm Bulk Update đã có trong Repository: Chuyển ACTIVE -> ENDED
        int count = sessionRepository.updateStatusToEnded(
                AuctionStatus.ACTIVE,
                AuctionStatus.ENDED,
                now
        );

        if (count > 0) {
            logger.info("🤖 Robot: Đã đóng tự động " + count + " phiên đấu giá hết hạn.");
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
        }
    }
}