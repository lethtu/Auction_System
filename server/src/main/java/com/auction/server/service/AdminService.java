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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    private static final String DEFAULT_ROLE = "user";
    private static final String ADMIN_ROLE = "admin";

    private final AuctionSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AdminService(
            AuctionSessionRepository sessionRepository,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public AdminService(AuctionSessionRepository sessionRepository, UserRepository userRepository) {
        this(sessionRepository, userRepository, null);
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
        if (jdbcTemplate == null) {
            return findUsersByRole(role)
                    .stream()
                    .map(this::mapToUserResponseDTO)
                    .toList();
        }

        String sql = """
                SELECT id, username, COALESCE(fullname, '') AS display_name,
                       email, role, balance, banned
                FROM users
                """;

        if (role == null || role.trim().isEmpty()) {
            return jdbcTemplate.query(sql + " ORDER BY id", this::mapUserRow);
        }

        return jdbcTemplate.query(
                sql + " WHERE LOWER(role) = ? ORDER BY id",
                this::mapUserRow,
                normalizeRole(role)
        );
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

        if (jdbcTemplate == null) {
            banUserWithRepository(targetUserId);
            return;
        }

        UserSummary target = findUserSummaryById(targetUserId);

        if (isAdminRole(target.role())) {
            throw new RuntimeException("Không được khóa tài khoản Admin khác");
        }

        int updated = jdbcTemplate.update("UPDATE users SET banned = TRUE WHERE id = ?", targetUserId);

        if (updated == 0) {
            throw new RuntimeException("Không tìm thấy user cần khóa");
        }
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
            AuctionStatus enumStatus = AuctionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
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

        String normalizedRole = normalizeRole(role);

        return users.stream()
                .filter(user -> normalizedRole.equals(normalizeRole(user.getAccountType())))
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
        if (adminId == null) {
            throw new RuntimeException("Không tìm thấy admin");
        }

        User user = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy admin"));

        if (!(user instanceof Admin admin)) {
            logger.error("{} không phải là quản trị viên", adminId);
            throw new RuntimeException("Người này không phải là Quản trị viên");
        }

        return admin;
    }

    private void banUserWithRepository(Integer targetUserId) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user cần khóa"));

        if (target instanceof Admin) {
            throw new RuntimeException("Không được khóa tài khoản Admin khác");
        }

        target.setBanned(true);
        userRepository.save(target);
    }

    private UserSummary findUserSummaryById(Integer userId) {
        List<UserSummary> users = jdbcTemplate.query(
                "SELECT role FROM users WHERE id = ?",
                (rs, rowNum) -> new UserSummary(rs.getString("role")),
                userId
        );

        if (users.isEmpty()) {
            throw new RuntimeException("Không tìm thấy user cần khóa");
        }

        return users.get(0);
    }

    private UserResponseDTO mapUserRow(ResultSet rs, int rowNum) throws SQLException {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(rs.getInt("id"));
        dto.setUsername(nullToEmpty(rs.getString("username")));
        dto.setFullname(nullToEmpty(rs.getString("display_name")));
        dto.setEmail(nullToEmpty(rs.getString("email")));
        dto.setAccountType(normalizeRole(rs.getString("role")));
        dto.setBalance(defaultMoney(rs.getBigDecimal("balance")));
        dto.setBanned(rs.getBoolean("banned"));
        return dto;
    }

    private UserResponseDTO mapToUserResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getFullname(),
                user.getEmail(),
                normalizeRole(user.getAccountType()),
                defaultMoney(user.getBalance()),
                user.isBanned()
        );
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return DEFAULT_ROLE;
        }

        return role.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isAdminRole(String role) {
        return ADMIN_ROLE.equals(normalizeRole(role));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record UserSummary(String role) {
    }
}