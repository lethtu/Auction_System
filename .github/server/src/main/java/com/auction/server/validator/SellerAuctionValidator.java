package com.auction.server.validator;

import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.exception.InvalidItemException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public final class SellerAuctionValidator {
    private static final Set<String> ALLOWED_TYPES = Set.of("electronics", "art", "vehicle");
    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    private SellerAuctionValidator() {
    }

    public static void validate(CreateAuctionRequest request) {
        if (request == null) {
            throw new InvalidItemException("Invalid auction session data");
        }

        validateRequiredText(request.getName(), "Product name cannot be empty");
        validateItemType(request.getType());
        validateDescription(request.getDescription());

        boolean isDraft = "DRAFT".equalsIgnoreCase(request.getStatus());
        if (isDraft) {
            if (request.getStartingPrice() == null) {
                request.setStartingPrice(BigDecimal.ZERO);
            }
            if (request.getStartingPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidItemException("Starting price cannot be negative");
            }
            if (request.getStepPrice() != null && request.getStepPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidItemException("Step price must be greater than 0");
            }
            if (request.getReservePrice() != null) {
                validateReservePrice(request.getReservePrice(), request.getStartingPrice());
            }
        } else {
            validatePositivePrice(request.getStartingPrice(), "Starting price must be greater than 0");
            validatePositivePrice(request.getStepPrice(), "Step price must be greater than 0");
            validateReservePrice(request.getReservePrice(), request.getStartingPrice());
            validateAuctionTime(request.getStartTime(), request.getEndTime());
        }
    }

    private static void validateRequiredText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidItemException(message);
        }
    }

    private static void validateItemType(String type) {
        validateRequiredText(type, "Product type cannot be empty");

        if (!ALLOWED_TYPES.contains(type.trim().toLowerCase())) {
            throw new InvalidItemException("Invalid product type");
        }
    }

    private static void validateDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidItemException("Description cannot exceed 1000 characters");
        }
    }

    private static void validatePositivePrice(BigDecimal price, String message) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidItemException(message);
        }
    }

    private static void validateReservePrice(BigDecimal reservePrice, BigDecimal startingPrice) {
        if (reservePrice == null) {
            return;
        }

        if (reservePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidItemException("Reserve price cannot be negative");
        }

        if (reservePrice.compareTo(BigDecimal.ZERO) > 0 && startingPrice != null && reservePrice.compareTo(startingPrice) < 0) {
            throw new InvalidItemException("Reserve price cannot be less than starting price");
        }
    }

    private static void validateAuctionTime(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();

        if (endTime == null || !endTime.isAfter(now)) {
            throw new InvalidItemException("End time must be in the future");
        }

        if (startTime != null && !endTime.isAfter(startTime)) {
            throw new InvalidItemException("End time must be after start time");
        }
    }
}
