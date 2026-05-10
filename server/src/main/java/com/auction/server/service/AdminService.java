package com.auction.server.service;

import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.dto.UserResponseDTO;
import com.auction.server.mapper.SessionResponseMapper;
import com.auction.server.model.Admin;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Seller;
import com.auction.server.model.User;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    private final AuctionSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public AdminService(AuctionSessionRepository sessionRepository, UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    public List<SessionResponseDTO> getPendingSessions() {
        return sessionRepository.findByStatus(AuctionStatus.PENDING)
                .stream()
                .map(SessionResponseMapper::toDTO)
                .toList();
    }

    public List<SessionResponseDTO> getAllSessions(String status) {
        return findSessionsByStatus(status)
                .stream()
                .map(SessionResponseMapper::toDTO)
                .toList();
    }

    public SessionResponseDTO getSessionDetail(Integer sessionId) {
        return SessionResponseMapper.toDTO(getSessionById(sessionId));
    }

    public List<UserResponseDTO> getAllUsers(String role) {
        return findUsersByRole(role)
                .stream()
                .map(this::mapToUserResponseDTO)
                .toList();
    }

    @Transactional
    public void approveSession(Integer sessionId, Integer adminId) {
        Admin admin = checkAdminPermission(adminId);
        AuctionSession session = getSessionById(sessionId);
        validatePendingSession(session, "Phiên này đã được xử lý hoặc không ở trạng thái chờ duyệt");

        LocalDateTime now = LocalDateTime.now();
        session.setStatus(AuctionStatus.ACTIVE);
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
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối");
        }

        AuctionSession session = getSessionById(sessionId);
        validatePendingSession(session, "Chỉ được từ chối các phiên đang ở trạng thái chờ duyệt");

        LocalDateTime now = LocalDateTime.now();
        session.setStatus(AuctionStatus.REJECTED);
        session.setRejectedAt(now);
        session.setRejectedByAdminId(admin.getId());
        session.setRejectReason(reason.trim());
        session.setApprovedAt(null);
        session.setApprovedByAdminId(null);
        session.setStartTime(null);

        sessionRepository.save(session);
    }

    @Transactional
    public void banUser(Integer targetUserId, Integer adminId) {
        checkAdminPermission(adminId);

        if (targetUserId == null || targetUserId.equals(adminId)) {
            throw new RuntimeException("Không thể khóa chính tài khoản admin hiện tại");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user cần khóa"));

        if (target instanceof Admin) {
            throw new RuntimeException("Không được khóa tài khoản Admin khác");
        }

        target.setBanned(true);
        userRepository.save(target);
    }

    @Transactional
    public void cancelAuction(Integer sessionId, Integer adminId) {
        checkAdminPermission(adminId);
        AuctionSession session = getSessionById(sessionId);

        if (session.getStatus() == AuctionStatus.ENDED || session.getStatus() == AuctionStatus.CANCELED) {
            throw new RuntimeException("Phiên này đã kết thúc hoặc đã bị hủy");
        }

        session.setStatus(AuctionStatus.CANCELED);
        sessionRepository.save(session);
    }

    private List<AuctionSession> findSessionsByStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return sessionRepository.findAll();
        }

        try {
            AuctionStatus enumStatus = AuctionStatus.valueOf(status.trim().toUpperCase());
            return sessionRepository.findByStatus(enumStatus);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    private List<User> findUsersByRole(String role) {
        List<User> users = userRepository.findAll();

        if (role == null || role.trim().isEmpty()) {
            return users;
        }

        return users.stream()
                .filter(user -> user.getAccountType() != null
                        && user.getAccountType().equalsIgnoreCase(role.trim()))
                .toList();
    }

    private AuctionSession getSessionById(Integer sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên đấu giá"));
    }

    private void validatePendingSession(AuctionSession session, String errorMessage) {
        if (session.getStatus() != AuctionStatus.PENDING) {
            logger.error("Phiên {} không ở trạng thái chờ duyệt", session.getId());
            throw new RuntimeException(errorMessage);
        }
    }

    private Admin checkAdminPermission(Integer adminId) {
        User user = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy admin"));

        if (!(user instanceof Admin admin)) {
            logger.error("{} không phải là quản trị viên", adminId);
            throw new RuntimeException("Người này không phải là Quản trị viên");
        }

        return admin;
    }

    private UserResponseDTO mapToUserResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullname(user.getFullname());
        dto.setEmail(user.getEmail());
        dto.setAccountType(user.getAccountType());
        dto.setBalance(user.getBalance());
        dto.setBanned(user.isBanned());

        if (user instanceof Seller seller) {
            dto.setShopName(seller.getShopName());
        }

        return dto;
    }
}