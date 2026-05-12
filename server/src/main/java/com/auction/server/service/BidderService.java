package com.auction.server.service;

import com.auction.server.model.User;
import com.auction.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class BidderService {
    private static final Logger logger = LoggerFactory.getLogger(BidderService.class);

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Map<String, Object> upToSeller(Integer userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            User user = userRepository.findById(userId).orElse(null);

            if (user == null) {
                logger.error("Lỗi: Không tìm thấy ID {}", userId);
                response.put("success", false);
                response.put("message", "Người dùng không tồn tại");
                return response;
            }

            String currentRole = user.getAccountType();
            if (!"BIDDER".equals(currentRole)) {
                logger.warn("User {} không thể nâng cấp vì đang là {}", userId, currentRole);
                response.put("success", false);
                response.put("message", "Tài khoản không phải là BIDDER hoặc đã là SELLER");
                return response;
            }

            // Dùng native UPDATE để thay đổi cột discriminator "role" trực tiếp trong DB
            // Vì Hibernate không cho phép thay đổi discriminator qua JPA entity thông thường
            int updated = userRepository.updateRoleById(userId, "SELLER");
            if (updated == 0) {
                logger.error("Không thể cập nhật role cho User {}", userId);
                response.put("success", false);
                response.put("message", "Cập nhật thất bại");
                return response;
            }

            logger.info("User {} đã nâng cấp lên SELLER thành công", userId);
            response.put("success", true);
            response.put("message", "Đã nâng cấp tài khoản thành công");

        } catch (Exception e) {
            logger.error("Lỗi hệ thống khi nâng cấp User {}", userId, e);
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
        }

        return response;
    }
}