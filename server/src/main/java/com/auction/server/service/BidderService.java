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

    private static final String USER_NOT_FOUND_MESSAGE = "User does not exist";
    private static final String INVALID_ROLE_MESSAGE = "Account is not a BIDDER or is already a SELLER";
    private static final String UPDATE_FAILED_MESSAGE = "Upgrade failed";
    private static final String UPDATE_SUCCESS_MESSAGE = "Account upgraded successfully";

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
                logger.error("Error: User ID {} not found", userId);
                return buildResponse(false, USER_NOT_FOUND_MESSAGE);
            }

            String currentRole = normalizeRole(user.getAccountType());

            if (!BIDDER_ROLE.equals(currentRole)) {
                logger.warn("User {} cannot be upgraded because current role is {}", userId, currentRole);
                return buildResponse(false, INVALID_ROLE_MESSAGE);
            }

            int updated = userRepository.updateRoleById(userId, SELLER_ROLE);

            if (updated == 0) {
                logger.error("Could not update role for User {}", userId);
                return buildResponse(false, UPDATE_FAILED_MESSAGE);
            }

            logger.info("User {} upgraded to seller successfully", userId);
            return buildResponse(true, UPDATE_SUCCESS_MESSAGE);

        } catch (Exception e) {
            logger.error("System error while upgrading User {}", userId, e);
            response.put("success", false);
            response.put("message", "System error: " + e.getMessage());
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