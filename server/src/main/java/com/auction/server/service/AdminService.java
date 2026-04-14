package com.auction.server.service;

import com.auction.server.model.AuctionSession;
import com.auction.server.repository.AuctionSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminService {

    @Autowired
    private AuctionSessionRepository sessionRepository;

    public List<AuctionSession> getPendingSessions() {
        return sessionRepository.findByStatus("PENDING");
    }

    public void approveSession(Integer sessionId) {
        AuctionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên đấu giá"));

        if (!"PENDING".equals(session.getStatus())) {
            throw new RuntimeException("Phiên này đã được xử lý hoặc không ở trạng thái chờ duyệt");
        }

        session.setStatus("ACTIVE");
        session.setStartTime(LocalDateTime.now());
        sessionRepository.save(session);
    }

    public void rejectSession(Integer sessionId) {
        AuctionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên đấu giá"));

        session.setStatus("REJECTED");
        sessionRepository.save(session);
    }
}