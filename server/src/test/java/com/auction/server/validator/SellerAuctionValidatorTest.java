package com.auction.server.validator;

import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.exception.InvalidItemException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SellerAuctionValidatorTest {

    @Test
    void validateRejectsNullRequest() {
        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(null)
        );

        assertEquals("Invalid auction session data", ex.getMessage());
    }

    @Test
    void validateAcceptsValidPublishedAuction() {
        CreateAuctionRequest request = validPublishedRequest();

        assertDoesNotThrow(() -> SellerAuctionValidator.validate(request));
    }

    @Test
    void validateRejectsMissingNameAndType() {
        CreateAuctionRequest missingName = validPublishedRequest();
        missingName.setName("   ");
        assertValidationMessage(missingName, "Product name cannot be empty");

        CreateAuctionRequest missingType = validPublishedRequest();
        missingType.setType("   ");
        assertValidationMessage(missingType, "Product type cannot be empty");
    }

    @Test
    void validateRejectsInvalidTypeAndLongDescription() {
        CreateAuctionRequest invalidType = validPublishedRequest();
        invalidType.setType("book");
        assertValidationMessage(invalidType, "Invalid product type");

        CreateAuctionRequest longDescription = validPublishedRequest();
        longDescription.setDescription("x".repeat(1001));
        assertValidationMessage(longDescription, "Description cannot exceed 1000 characters");
    }

    @Test
    void validateRejectsInvalidPublishedPrices() {
        CreateAuctionRequest missingStartingPrice = validPublishedRequest();
        missingStartingPrice.setStartingPrice(null);
        assertValidationMessage(missingStartingPrice, "Starting price must be greater than 0");

        CreateAuctionRequest zeroStepPrice = validPublishedRequest();
        zeroStepPrice.setStepPrice(BigDecimal.ZERO);
        assertValidationMessage(zeroStepPrice, "Step price must be greater than 0");

        CreateAuctionRequest negativeReserve = validPublishedRequest();
        negativeReserve.setReservePrice(new BigDecimal("-1"));
        assertValidationMessage(negativeReserve, "Reserve price cannot be negative");

        CreateAuctionRequest reserveLessThanStart = validPublishedRequest();
        reserveLessThanStart.setReservePrice(new BigDecimal("50"));
        assertValidationMessage(reserveLessThanStart, "Reserve price cannot be less than starting price");
    }

    @Test
    void validateRejectsInvalidPublishedTime() {
        CreateAuctionRequest pastEnd = validPublishedRequest();
        pastEnd.setEndTime(LocalDateTime.now().minusMinutes(1));
        assertValidationMessage(pastEnd, "End time must be in the future");

        CreateAuctionRequest endBeforeStart = validPublishedRequest();
        endBeforeStart.setStartTime(LocalDateTime.now().plusDays(3));
        endBeforeStart.setEndTime(LocalDateTime.now().plusDays(2));
        assertValidationMessage(endBeforeStart, "End time must be after start time");
    }

    @Test
    void validateDraftAllowsMissingStartingPriceAndSkipsEndTimeRequirement() {
        CreateAuctionRequest draft = validPublishedRequest();
        draft.setStatus("DRAFT");
        draft.setStartingPrice(null);
        draft.setReservePrice(null);
        draft.setEndTime(null);

        assertDoesNotThrow(() -> SellerAuctionValidator.validate(draft));
        assertEquals(BigDecimal.ZERO, draft.getStartingPrice());
    }

    @Test
    void validateDraftRejectsNegativeStartingPriceInvalidStepAndReserveBelowStart() {
        CreateAuctionRequest negativeStartingPrice = validPublishedRequest();
        negativeStartingPrice.setStatus("DRAFT");
        negativeStartingPrice.setStartingPrice(new BigDecimal("-1"));
        assertValidationMessage(negativeStartingPrice, "Starting price cannot be negative");

        CreateAuctionRequest invalidStep = validPublishedRequest();
        invalidStep.setStatus("DRAFT");
        invalidStep.setStepPrice(BigDecimal.ZERO);
        assertValidationMessage(invalidStep, "Step price must be greater than 0");

        CreateAuctionRequest reserveBelowStart = validPublishedRequest();
        reserveBelowStart.setStatus("DRAFT");
        reserveBelowStart.setReservePrice(new BigDecimal("50"));
        assertValidationMessage(reserveBelowStart, "Reserve price cannot be less than starting price");
    }

    private static void assertValidationMessage(CreateAuctionRequest request, String expectedMessage) {
        InvalidItemException ex = assertThrows(
                InvalidItemException.class,
                () -> SellerAuctionValidator.validate(request)
        );

        assertEquals(expectedMessage, ex.getMessage());
    }

    private static CreateAuctionRequest validPublishedRequest() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setName("Vintage Camera");
        request.setType(" Electronics ");
        request.setDescription("A working vintage camera");
        request.setStatus("ACTIVE");
        request.setStartingPrice(new BigDecimal("100"));
        request.setStepPrice(new BigDecimal("10"));
        request.setReservePrice(new BigDecimal("120"));
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));
        return request;
    }
}
