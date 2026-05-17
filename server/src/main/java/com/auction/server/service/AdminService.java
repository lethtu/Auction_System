package com.auction.server.service;

import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.dto.UserResponseDTO;
import com.auction.server.mapper.SessionResponseMapper;
import com.auction.server.model.Admin;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.User;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    private static final String DEFAULT_ROLE = "user";

    private static final String ERROR_ADMIN_NOT_FOUND = "Không tìm thấy admin";
    private static final String ERROR_NOT_ADMIN = "Người này không phải là Quản trị viên";
    private static final String ERROR_SESSION_NOT_FOUND = "Không tìm thấy phiên đấu giá";
    private static final String ERROR_TARGET_USER_NOT_FOUND = "Không tìm thấy user cần khóa";
    private static final String ERROR_SELF_BAN = "Không thể khóa chính tài khoản admin hiện tại";
    private static final String ERROR_BAN_ADMIN = "Không được khóa tài khoản Admin khác";
    private static final String ERROR_REJECT_REASON_REQUIRED = "Vui lòng nhập lý do từ chối";
    private static final String ERROR_CANCEL_FINISHED_SESSION = "Phiên này đã kết thúc hoặc đã bị hủy";

    private final AuctionSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public AdminService(
            AuctionSessionRepository sessionRepository,
            UserRepository userRepository
    ) {
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

        clearRejectionInfo(session);

        sessionRepository.save(session);
    }

    @Transactional
    public void rejectSession(Integer sessionId, Integer adminId, String reason) {
        Admin admin = checkAdminPermission(adminId);
        String cleanReason = normalizeRequiredReason(reason);

        AuctionSession session = getSessionById(sessionId);

        validatePendingSession(session, "Chỉ được từ chối các phiên đang ở trạng thái chờ duyệt");

        LocalDateTime now = LocalDateTime.now();

        session.setStatus(AuctionStatus.REJECTED);
        session.setRejectedAt(now);
        session.setRejectedByAdminId(admin.getId());
        session.setRejectReason(cleanReason);

        clearApprovalInfo(session);

        sessionRepository.save(session);
    }

    @Transactional
    public void banUser(Integer targetUserId, Integer adminId) {
        Admin admin = checkAdminPermission(adminId);

        if (targetUserId == null || targetUserId.equals(admin.getId())) {
            throw new IllegalArgumentException(ERROR_SELF_BAN);
        }

        User target = getUserById(targetUserId, ERROR_TARGET_USER_NOT_FOUND);

        if (target instanceof Admin) {
            throw new IllegalArgumentException(ERROR_BAN_ADMIN);
        }

        target.setBanned(true);
        userRepository.save(target);
    }

    @Transactional
    public void cancelAuction(Integer sessionId, Integer adminId) {
        checkAdminPermission(adminId);

        AuctionSession session = getSessionById(sessionId);

        if (session.getStatus() == AuctionStatus.ENDED || session.getStatus() == AuctionStatus.CANCELED) {
            throw new IllegalArgumentException(ERROR_CANCEL_FINISHED_SESSION);
        }

        session.setStatus(AuctionStatus.CANCELED);
        sessionRepository.save(session);
    }

    private List<AuctionSession> findSessionsByStatus(String status) {
        if (!hasText(status)) {
            return sessionRepository.findAll();
        }

        try {
            AuctionStatus enumStatus = AuctionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            return sessionRepository.findByStatus(enumStatus);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    private List<User> findUsersByRole(String role) {
        List<User> users = userRepository.findAll();

        if (!hasText(role)) {
            return users;
        }

        String normalizedRole = normalizeRole(role);

        return users.stream()
                .filter(user -> normalizedRole.equals(normalizeRole(user.getAccountType())))
                .toList();
    }

    private AuctionSession getSessionById(Integer sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException(ERROR_SESSION_NOT_FOUND);
        }

        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(ERROR_SESSION_NOT_FOUND));
    }

    private User getUserById(Integer userId, String errorMessage) {
        if (userId == null) {
            throw new IllegalArgumentException(errorMessage);
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(errorMessage));
    }

    private Admin checkAdminPermission(Integer adminId) {
        User user = getUserById(adminId, ERROR_ADMIN_NOT_FOUND);

        if (!(user instanceof Admin admin)) {
            logger.warn("{} không phải là quản trị viên", adminId);
            throw new IllegalArgumentException(ERROR_NOT_ADMIN);
        }

        return admin;
    }

    private void validatePendingSession(AuctionSession session, String errorMessage) {
        if (session.getStatus() != AuctionStatus.PENDING) {
            logger.warn("Phiên {} không ở trạng thái chờ duyệt", session.getId());
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String normalizeRequiredReason(String reason) {
        if (!hasText(reason)) {
            throw new IllegalArgumentException(ERROR_REJECT_REASON_REQUIRED);
        }

        return reason.trim();
    }

    private void clearApprovalInfo(AuctionSession session) {
        session.setApprovedAt(null);
        session.setApprovedByAdminId(null);
        session.setStartTime(null);
    }

    private void clearRejectionInfo(AuctionSession session) {
        session.setRejectedAt(null);
        session.setRejectedByAdminId(null);
        session.setRejectReason(null);
    }

    private UserResponseDTO mapToUserResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                nullToEmpty(user.getUsername()),
                nullToEmpty(user.getFullname()),
                nullToEmpty(user.getEmail()),
                normalizeRole(user.getAccountType()),
                defaultMoney(user.getBalance()),
                user.isBanned()
        );
    }

    private String normalizeRole(String role) {
        if (!hasText(role)) {
            return DEFAULT_ROLE;
        }

        return role.trim().toLowerCase(Locale.ROOT);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}