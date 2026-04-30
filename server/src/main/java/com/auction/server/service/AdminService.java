package com.auction.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.dto.UserResponseDTO;
import com.auction.server.model.*;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    @Autowired
    private AuctionSessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    private Admin checkAdminPermission(Integer adminId) {
        User user = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        if (!(user instanceof Admin)) {
            logger.error("{} không phải là quản trị viên", adminId);
            throw new RuntimeException("Người này không phải là Quản trị viên");
        }

        Admin admin = (Admin) user;

        if (admin.getRole() == AdminRole.SUPPORT) {
            logger.error("LỖI QUYỀN HẠN: Nhân viên Hỗ trợ không được phép duyệt/từ chối phiên đấu giá!");
            throw new RuntimeException("LỖI QUYỀN HẠN: Nhân viên Hỗ trợ không được phép duyệt/từ chối phiên đấu giá!");
        }

        return admin;
    }

    public List<SessionResponseDTO> getPendingSessions() {
        // Sửa: Dùng Enum AuctionStatus.PENDING thay vì String "PENDING"
        return sessionRepository.findByStatus(AuctionStatus.PENDING)
                .stream()
                .map(this::mapToSessionResponseDTO)
                .toList();
    }

    public List<SessionResponseDTO> getAllSessions(String status) {
        List<AuctionSession> sessions = sessionRepository.findAll();

        if (status != null && !status.trim().isEmpty()) {
            try {
                // Sửa: Chuyển String từ client gửi lên thành Enum để so sánh
                AuctionStatus filterStatus = AuctionStatus.valueOf(status.trim().toUpperCase());
                sessions = sessions.stream()
                        .filter(session -> session.getStatus() == filterStatus)
                        .toList();
            } catch (IllegalArgumentException e) {
                // Nếu status gửi lên không hợp lệ, trả về danh sách trống hoặc xử lý tùy ý
                return List.of();
            }
        }

        return sessions.stream()
                .map(this::mapToSessionResponseDTO)
                .toList();
    }

    public SessionResponseDTO getSessionDetail(Integer sessionId) {
        AuctionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên đấu giá"));

        return mapToSessionResponseDTO(session);
    }

    @Transactional
    public void approveSession(Integer sessionId, Integer adminId) {
        Admin admin = checkAdminPermission(adminId);

        AuctionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên đấu giá"));

        // Sửa: So sánh Enum bằng ==
        if (session.getStatus() != AuctionStatus.PENDING) {
            logger.error("Phiên {} đã được xử lý hoặc không ở trạng thái chờ duyệt", sessionId);
            throw new RuntimeException("Phiên này đã được xử lý hoặc không ở trạng thái chờ duyệt");
        }

        LocalDateTime now = LocalDateTime.now();

        session.setStatus(AuctionStatus.ACTIVE); // Sửa: Gán Enum
        session.setStartTime(now);
        session.setApprovedAt(now);
        session.setApprovedByAdminId(admin.getId());

        // Đảm bảo các field này đã được khai báo chuẩn trong AuctionSession
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

        if (session.getStatus() != AuctionStatus.PENDING) {
            logger.error("Chỉ được từ chối các phiên đang ở trạng thái chờ duyệt");
            throw new RuntimeException("Chỉ được từ chối các phiên đang ở trạng thái chờ duyệt");
        }

        LocalDateTime now = LocalDateTime.now();

        session.setStatus(AuctionStatus.REJECTED); // Sửa: Gán Enum REJECTED
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

    private SessionResponseDTO mapToSessionResponseDTO(AuctionSession session) {
        SessionResponseDTO dto = new SessionResponseDTO();

        dto.setId(session.getId());

        if (session.getItem() != null) {
            dto.setProductId(session.getItem().getId());
            dto.setProductName(session.getItem().getName());
            dto.setProductType(session.getItem().getType());
            dto.setImageUrl(session.getItem().getImagePath()); // Sửa: getImagePath()
            dto.setDescription(session.getItem().getDescription());
        }

        if (session.getSeller() != null) {
            dto.setSellerId(session.getSeller().getId());
            dto.setSellerUsername(session.getSeller().getUsername());
            dto.setSellerFullname(session.getSeller().getFullname());
        }

        dto.setStartingPrice(session.getStartingPrice());
        dto.setCurrentPrice(session.getCurrentPrice());
        dto.setStepPrice(session.getStepPrice());

        dto.setCreatedAt(session.getCreatedAt());
        dto.setStartTime(session.getStartTime());
        dto.setEndTime(session.getEndTime());
        dto.setApprovedAt(session.getApprovedAt());
        dto.setRejectedAt(session.getRejectedAt());

        // Sửa: Convert Enum sang String để đưa vào DTO
        if (session.getStatus() != null) {
            dto.setStatus(session.getStatus().name());
        }

        dto.setRejectReason(session.getRejectReason());

        dto.setApprovedByAdminId(session.getApprovedByAdminId());
        dto.setRejectedByAdminId(session.getRejectedByAdminId());

        return dto;
    }
}