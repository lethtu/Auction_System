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

        assertEquals("Invalid auction session data", ex.getMessage());
    }

    @Test
    void emptyNameThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setName(" ");

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Product name cannot be empty", ex.getMessage());
    }

    @Test
    void emptyTypeThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setType(" ");

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Product type cannot be empty", ex.getMessage());
    }

    @Test
    void tooLongDescriptionThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setDescription("a".repeat(1001));

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Description cannot exceed 1000 characters", ex.getMessage());
    }

    @Test
    void zeroStartingPriceThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setStartingPrice(BigDecimal.ZERO);

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Starting price must be greater than 0", ex.getMessage());
    }

    @Test
    void negativeStartingPriceThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setStartingPrice(new BigDecimal("-1000"));

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Starting price must be greater than 0", ex.getMessage());
    }

    @Test
    void nullStepPriceThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setStepPrice(null);

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Step price must be greater than 0", ex.getMessage());
    }

    @Test
    void zeroStepPriceThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setStepPrice(BigDecimal.ZERO);

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Step price must be greater than 0", ex.getMessage());
    }




    @Test
    void reservePriceBelowStartingPriceThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setReservePrice(new BigDecimal("999999"));

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("Reserve price cannot be less than starting price", ex.getMessage());
    }

    @Test
    void nullEndTimeThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setEndTime(null);

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("End time must be in the future", ex.getMessage());
    }

    @Test
    void endTimeInPastThrowsException() {
        CreateAuctionRequest request = validRequest();
        request.setEndTime(LocalDateTime.now().minusMinutes(1));

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("End time must be in the future", ex.getMessage());
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

        assertEquals("End time must be after start time", ex.getMessage());
    }

    @Test
    void endTimeEqualStartTimeThrowsException() {
        CreateAuctionRequest request = validRequest();
        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        request.setStartTime(startTime);
        request.setEndTime(startTime);

        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals("End time must be after start time", ex.getMessage());
    }

    private CreateAuctionRequest validRequest() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setName("Laptop Gaming");
        request.setType("electronics");
        request.setDescription("Good condition");
        request.setSellerId(1);
        request.setStartingPrice(new BigDecimal("1000000"));
        request.setStepPrice(new BigDecimal("100000"));
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusDays(1));
        return request;
    }
}
