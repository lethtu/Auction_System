package com.auction.server.service;

import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.dto.UserResponseDTO;
import com.auction.server.mapper.SessionResponseMapper;
import com.auction.server.model.Admin;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Item;
import com.auction.server.model.User;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final String ERROR_ADMIN_NOT_FOUND = "Admin not found";
    private static final String ERROR_NOT_ADMIN = "This user is not an Administrator";
    private static final String ERROR_SESSION_NOT_FOUND = "Auction session not found";
    private static final String ERROR_TARGET_USER_NOT_FOUND = "Target user not found";
    private static final String ERROR_RESTORE_USER_NOT_FOUND = "User to restore not found";
    private static final String ERROR_PRODUCT_NOT_FOUND = "Product not found";
    private static final String ERROR_ITEM_REPOSITORY_NOT_READY = "Product data repository not configured";
    private static final String ERROR_SELF_BAN = "Cannot ban your own admin account";
    private static final String ERROR_BAN_ADMIN = "Cannot ban another Admin account";
    private static final String ERROR_REJECT_REASON_REQUIRED = "Please enter a rejection reason";
    private static final String ERROR_APPROVE_NOT_PENDING = "This session has already been processed or is not in pending status";
    private static final String ERROR_REJECT_NOT_PENDING = "Can only reject sessions in pending status";
    private static final String ERROR_CANCEL_FINISHED_SESSION = "This session has already ended or been canceled";

    private final AuctionSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    public AdminService(
            AuctionSessionRepository sessionRepository,
            UserRepository userRepository
    ) {
        this(sessionRepository, userRepository, null);
    }

    @Autowired
    public AdminService(
            AuctionSessionRepository sessionRepository,
            UserRepository userRepository,
            ItemRepository itemRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
    }

    public List<SessionResponseDTO> getPendingSessions() {
        return sessionRepository.findByStatus(AuctionStatus.DRAFT)
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

        if (session.getStatus() != AuctionStatus.DRAFT) {
            throw new IllegalArgumentException(ERROR_APPROVE_NOT_PENDING);
        }

        LocalDateTime now = LocalDateTime.now();
        if (session.getStartTime() == null) {
            session.setStartTime(now);
        }

        session.setStatus(resolveApprovedStatus(session, now));
        session.setApprovedAt(now);
        session.setApprovedByAdminId(admin.getId());
        clearRejectionInfo(session);

        sessionRepository.save(session);
    }

    @Transactional
    public void rejectSession(Integer sessionId, Integer adminId, String reason) {
        String normalizedReason = normalizeRequiredReason(reason);
        Admin admin = checkAdminPermission(adminId);
        AuctionSession session = getSessionById(sessionId);

        if (session.getStatus() != AuctionStatus.DRAFT) {
            throw new IllegalArgumentException(ERROR_REJECT_NOT_PENDING);
        }

        session.setStatus(AuctionStatus.CANCELED);
        session.setRejectedAt(LocalDateTime.now());
        session.setRejectedByAdminId(admin.getId());
        session.setRejectReason(normalizedReason);
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
    public void restoreUser(Integer targetUserId, Integer adminId) {
        checkAdminPermission(adminId);

        User target = getUserById(targetUserId, ERROR_RESTORE_USER_NOT_FOUND);
        target.setBanned(false);
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

    @Transactional
    public void hideProduct(Integer productId, Integer adminId) {
        setProductHidden(productId, adminId, true);
    }

    @Transactional
    public void showProduct(Integer productId, Integer adminId) {
        setProductHidden(productId, adminId, false);
    }

    private void setProductHidden(Integer productId, Integer adminId, boolean hidden) {
        checkAdminPermission(adminId);

        Item item = getItemById(productId);
        item.setHidden(hidden);
        itemRepository.save(item);
    }

    private AuctionStatus resolveApprovedStatus(AuctionSession session, LocalDateTime now) {
        LocalDateTime startTime = session.getStartTime();

        if (startTime != null && startTime.isAfter(now)) {
            return AuctionStatus.COMING;
        }

        return AuctionStatus.ACTIVE;
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

    private Item getItemById(Integer productId) {
        if (itemRepository == null) {
            throw new IllegalStateException(ERROR_ITEM_REPOSITORY_NOT_READY);
        }

        if (productId == null) {
            throw new IllegalArgumentException(ERROR_PRODUCT_NOT_FOUND);
        }

        return itemRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(ERROR_PRODUCT_NOT_FOUND));
    }

    private Admin checkAdminPermission(Integer adminId) {
        User user = getUserById(adminId, ERROR_ADMIN_NOT_FOUND);

        if (!(user instanceof Admin admin)) {
            logger.warn("{} is not an administrator", adminId);
            throw new IllegalArgumentException(ERROR_NOT_ADMIN);
        }

        return admin;
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