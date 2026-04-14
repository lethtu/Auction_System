package com.auction.server.service;

import com.auction.server.model.Admin;
import com.auction.server.model.AdminRole;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.User;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminService {

    @Autowired
    private AuctionSessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    private Admin checkAdminPermission(Integer adminId) {
        User user = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        if (!(user instanceof Admin)) {
            throw new RuntimeException("Người này không phải là Quản trị viên");
        }

        Admin admin = (Admin) user;

        if (admin.getRole() == AdminRole.SUPPORT) {
            throw new RuntimeException("LỖI QUYỀN HẠN: Nhân viên Hỗ trợ không được phép duyệt/từ chối phiên đấu giá!");
        }

        return admin;
    }

    public List<AuctionSession> getPendingSessions() {
        return sessionRepository.findByStatus("PENDING");
    }

    public void approveSession(Integer sessionId, Integer adminId) {
        checkAdminPermission(adminId);

        AuctionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên đấu giá"));

        if (!"PENDING".equals(session.getStatus())) {
            throw new RuntimeException("Phiên này đã được xử lý hoặc không ở trạng thái chờ duyệt");
        }

        session.setStatus("ACTIVE");
        session.setStartTime(LocalDateTime.now());
        sessionRepository.save(session);
    }

    public void rejectSession(Integer sessionId, Integer adminId) {
        checkAdminPermission(adminId);

        AuctionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên đấu giá"));

        if (!"PENDING".equals(session.getStatus())) {
            throw new RuntimeException("Chỉ được từ chối các phiên đang ở trạng thái chờ duyệt");
        }

        session.setStatus("REJECTED");
        sessionRepository.save(session);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}