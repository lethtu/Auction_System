package com.auction.server.validator;

import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.exception.InvalidItemException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SellerValidationTest {

    @Test
    void validRequestDoesNotThrow() {
        CreateAuctionRequest request = validRequest();
        assertDoesNotThrow(() -> SellerAuctionValidator.validate(request));
    }

    @Test
    void emptyNameThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setName(" ");
        assertThrows(InvalidItemException.class, () -> SellerAuctionValidator.validate(request));
    }

    @Test
    void zeroStartingPriceThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setStartingPrice(BigDecimal.ZERO);
        assertThrows(InvalidItemException.class, () -> SellerAuctionValidator.validate(request));
    }

    @Test
    void negativeStartingPriceThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setStartingPrice(new BigDecimal("-1000"));
        assertThrows(InvalidItemException.class, () -> SellerAuctionValidator.validate(request));
    }

    @Test
    void endTimeBeforeStartTimeThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setStartTime(LocalDateTime.now().plusDays(3));
        request.setEndTime(LocalDateTime.now().plusDays(2));
        assertThrows(InvalidItemException.class, () -> SellerAuctionValidator.validate(request));
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
