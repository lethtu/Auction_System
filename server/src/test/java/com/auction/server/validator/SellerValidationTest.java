package com.auction.server.validator;

import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.exception.InvalidItemException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SellerValidationTest {

    @Test
    void validRequestDoesNotThrow() {
        assertDoesNotThrow(() -> SellerAuctionValidator.validate(validRequest()));
    }

    @Test
    void nullRequestThrowsException() {
        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(null)
        );

        assertEquals("Dữ liệu phiên đấu giá không hợp lệ", ex.getMessage());
    }

    @Test
    void emptyNameThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setName(" ");

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Tên sản phẩm không được để trống", ex.getMessage());
    }

    @Test
    void emptyTypeThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setType(" ");

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Loại sản phẩm không được để trống", ex.getMessage());
    }

    @Test
    void tooLongDescriptionThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setDescription("a".repeat(1001));

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Mô tả không được quá 1000 ký tự", ex.getMessage());
    }

    @Test
    void zeroStartingPriceThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setStartingPrice(BigDecimal.ZERO);

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Giá khởi điểm phải lớn hơn 0", ex.getMessage());
    }

    @Test
    void negativeStartingPriceThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setStartingPrice(new BigDecimal("-1000"));

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Giá khởi điểm phải lớn hơn 0", ex.getMessage());
    }

    @Test
    void nullStepPriceThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setStepPrice(null);

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Bước giá phải lớn hơn 0", ex.getMessage());
    }

    @Test
    void zeroStepPriceThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setStepPrice(BigDecimal.ZERO);

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Bước giá phải lớn hơn 0", ex.getMessage());
    }

    @Test
    void startTimeInPastThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setStartTime(LocalDateTime.now().minusMinutes(1));

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Thời gian bắt đầu không được nằm trong quá khứ", ex.getMessage());
    }

    @Test
    void nullEndTimeThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setEndTime(null);

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Thời gian kết thúc phải ở tương lai", ex.getMessage());
    }

    @Test
    void endTimeInPastThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setEndTime(LocalDateTime.now().minusMinutes(1));

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Thời gian kết thúc phải ở tương lai", ex.getMessage());
    }

    @Test
    void endTimeBeforeStartTimeThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setStartTime(LocalDateTime.now().plusDays(3));
        request.setEndTime(LocalDateTime.now().plusDays(2));

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu", ex.getMessage());
    }

    private CreateAuctionRequest validRequest() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setName("Laptop Gaming");
        request.setType("electronics");
        request.setDescription("Máy còn tốt");
        request.setSellerId(1);
        request.setStartingPrice(new BigDecimal("1000000"));
        request.setStepPrice(new BigDecimal("100000"));
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusDays(1));
        return request;
    }
}
