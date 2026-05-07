package com.auction.server.validator;

import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.exception.InvalidItemException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class SellerAuctionValidator {

    private SellerAuctionValidator() {
    }

    public static void validate(CreateAuctionRequest request) {
        if (request == null) {
            throw new InvalidItemException("Dữ liệu phiên đấu giá không hợp lệ");
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new InvalidItemException("Tên sản phẩm không được để trống");
        }

        if (request.getType() == null || request.getType().trim().isEmpty()) {
            throw new InvalidItemException("Loại sản phẩm không được để trống");
        }

        if (request.getDescription() != null && request.getDescription().length() > 1000) {
            throw new InvalidItemException("Mô tả không được quá 1000 ký tự");
        }

        if (request.getStartingPrice() == null || request.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidItemException("Giá khởi điểm phải lớn hơn 0");
        }

        if (request.getStepPrice() == null || request.getStepPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidItemException("Bước giá phải lớn hơn 0");
        }

        LocalDateTime now = LocalDateTime.now();

        if (request.getStartTime() != null && request.getStartTime().isBefore(now)) {
            throw new InvalidItemException("Thời gian bắt đầu không được nằm trong quá khứ");
        }

        if (request.getEndTime() == null || !request.getEndTime().isAfter(now)) {
            throw new InvalidItemException("Thời gian kết thúc phải ở tương lai");
        }

        if (request.getStartTime() != null && request.getEndTime().isBefore(request.getStartTime())) {
            throw new InvalidItemException("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu");
        }
    }
}
