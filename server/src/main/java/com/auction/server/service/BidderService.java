package com.auction.server.service;

import com.auction.server.model.User;
import com.auction.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class BidderService {
    private static final Logger logger = LoggerFactory.getLogger(BidderService.class);

    private static final String BIDDER_ROLE = "bidder";
    private static final String SELLER_ROLE = "seller";

    private static final String USER_NOT_FOUND_MESSAGE = "Người dùng không tồn tại";
    private static final String INVALID_ROLE_MESSAGE = "Tài khoản không phải là BIDDER hoặc đã là SELLER";
    private static final String UPDATE_FAILED_MESSAGE = "Cập nhật thất bại";
    private static final String UPDATE_SUCCESS_MESSAGE = "Đã nâng cấp tài khoản thành công";

    private final UserRepository userRepository;

    public BidderService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public Map<String, Object> upToSeller(Integer userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            User user = userRepository.findById(userId).orElse(null);

            if (user == null) {
                logger.error("Lỗi: Không tìm thấy ID {}", userId);
                return buildResponse(false, USER_NOT_FOUND_MESSAGE);
            }

            String currentRole = normalizeRole(user.getAccountType());

            if (!BIDDER_ROLE.equals(currentRole)) {
                logger.warn("User {} không thể nâng cấp vì đang là {}", userId, currentRole);
                return buildResponse(false, INVALID_ROLE_MESSAGE);
            }

            int updated = userRepository.updateRoleById(userId, SELLER_ROLE);

            if (updated == 0) {
                logger.error("Không thể cập nhật role cho User {}", userId);
                return buildResponse(false, UPDATE_FAILED_MESSAGE);
            }

            logger.info("User {} đã nâng cấp lên seller thành công", userId);
            return buildResponse(true, UPDATE_SUCCESS_MESSAGE);

        } catch (Exception e) {
            logger.error("Lỗi hệ thống khi nâng cấp User {}", userId, e);
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return response;
        }
    }

    private Map<String, Object> buildResponse(boolean success, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        return response;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }

        return role.trim().toLowerCase(Locale.ROOT);
    }
}