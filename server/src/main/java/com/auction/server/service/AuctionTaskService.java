package com.auction.server.service;

import com.auction.server.model.AuctionSession;
import com.auction.server.repository.AuctionSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuctionTaskService {

    @Autowired
    private AuctionSessionRepository sessionRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void closeExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<AuctionSession> expiredSessions = sessionRepository.findActiveExpiredSessions(now);

        for (AuctionSession session : expiredSessions) {
            session.setStatus("FINISHED");
            sessionRepository.save(session);
            System.out.println("🤖 Closed Auction Session ID: " + session.getId());
        }
    }
}