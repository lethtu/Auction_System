package com.auction.server.service;

import com.auction.server.dto.UserResponseDTO;
import com.auction.server.model.Admin;
import com.auction.server.model.AdminRole;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.Seller;
import com.auction.server.model.User;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<AuctionSession> getAllSessions(String status) {
        List<AuctionSession> sessions = sessionRepository.findAll();

        if (status == null || status.trim().isEmpty()) {
            return sessions;
        }

        String normalizedStatus = status.trim();

        return sessions.stream()
                .filter(session -> session.getStatus() != null
                        && session.getStatus().equalsIgnoreCase(normalizedStatus))
                .toList();
    }

    public AuctionSession getSessionDetail(Integer sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên đấu giá"));
    }

    @Transactional
    public void approveSession(Integer sessionId, Integer adminId) {
        Admin admin = checkAdminPermission(adminId);

        AuctionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên đấu giá"));

        if (!"PENDING".equalsIgnoreCase(session.getStatus())) {
            throw new RuntimeException("Phiên này đã được xử lý hoặc không ở trạng thái chờ duyệt");
        }

        LocalDateTime now = LocalDateTime.now();

        session.setStatus("ACTIVE");
        session.setStartTime(now);
        session.setApprovedAt(now);
        session.setApprovedByAdminId(admin.getId());

        session.setRejectedAt(null);
        session.setRejectedByAdminId(null);
        session.setRejectReason(null);

        sessionRepository.save(session);
    }

    @Transactional
    public void rejectSession(Integer sessionId, Integer adminId, String reason) {
        Admin admin = checkAdminPermission(adminId);

        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("Vui lòng nhập lý do từ chối");
        }

        AuctionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên đấu giá"));

        if (!"PENDING".equalsIgnoreCase(session.getStatus())) {
            throw new RuntimeException("Chỉ được từ chối các phiên đang ở trạng thái chờ duyệt");
        }

        LocalDateTime now = LocalDateTime.now();

        session.setStatus("REJECTED");
        session.setRejectedAt(now);
        session.setRejectedByAdminId(admin.getId());
        session.setRejectReason(reason.trim());

        session.setApprovedAt(null);
        session.setApprovedByAdminId(null);
        session.setStartTime(null);

        sessionRepository.save(session);
    }

    public List<UserResponseDTO> getAllUsers(String role) {
        List<User> users = userRepository.findAll();

        if (role != null && !role.trim().isEmpty()) {
            String normalizedRole = role.trim();

            users = users.stream()
                    .filter(user -> user.getAccountType() != null
                            && user.getAccountType().equalsIgnoreCase(normalizedRole))
                    .toList();
        }

        return users.stream()
                .map(this::mapToUserResponseDTO)
                .toList();
    }

    private UserResponseDTO mapToUserResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullname(user.getFullname());
        dto.setEmail(user.getEmail());
        dto.setAccountType(user.getAccountType());
        dto.setBalance(user.getBalance());

        if (user instanceof Seller seller) {
            dto.setShopName(seller.getShopName());
        }

        if (user instanceof Admin admin) {
            dto.setEmployeeCode(admin.getEmployeeCode());
            dto.setAdminRole(admin.getRole() != null ? admin.getRole().name() : null);
        }

        return dto;
    }
}