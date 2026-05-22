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
            throw new InvalidItemException("Dữ liệu phiên đấu giá không hợp lệ");
        }

        validateRequiredText(request.getName(), "Tên sản phẩm không được để trống");
        validateItemType(request.getType());
        validateDescription(request.getDescription());

        boolean isDraft = "DRAFT".equalsIgnoreCase(request.getStatus());
        if (isDraft) {
            if (request.getStartingPrice() == null) {
                request.setStartingPrice(BigDecimal.ZERO);
            }
            if (request.getStartingPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidItemException("Giá khởi điểm không được âm");
            }
            if (request.getStepPrice() != null && request.getStepPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidItemException("Bước giá phải lớn hơn 0");
            }
            if (request.getReservePrice() != null) {
                validateReservePrice(request.getReservePrice(), request.getStartingPrice());
            }
        } else {
            validatePositivePrice(request.getStartingPrice(), "Giá khởi điểm phải lớn hơn 0");
            validatePositivePrice(request.getStepPrice(), "Bước giá phải lớn hơn 0");
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
        validateRequiredText(type, "Loại sản phẩm không được để trống");

        if (!ALLOWED_TYPES.contains(type.trim().toLowerCase())) {
            throw new InvalidItemException("Loại sản phẩm không hợp lệ");
        }
    }

    private static void validateDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidItemException("Mô tả không được quá 1000 ký tự");
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
            throw new InvalidItemException("Giá sàn không được âm");
        }

        if (reservePrice.compareTo(BigDecimal.ZERO) > 0 && startingPrice != null && reservePrice.compareTo(startingPrice) < 0) {
            throw new InvalidItemException("Giá sàn không được nhỏ hơn giá khởi điểm");
        }
    }

    private static void validateAuctionTime(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();

        if (endTime == null || !endTime.isAfter(now)) {
            throw new InvalidItemException("Thời gian kết thúc phải ở tương lai");
        }

        if (startTime != null && !endTime.isAfter(startTime)) {
            throw new InvalidItemException("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu");
        }
    }
}
