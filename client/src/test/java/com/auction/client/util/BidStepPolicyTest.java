package com.auction.client.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BidStepPolicyTest {

    @Test
    void getDynamicStepPrice_nullOrSmallPrice_returnsSmallestIncrement() {
        assertEquals(new BigDecimal("10000"), BidStepPolicy.getDynamicStepPrice(null));
        assertEquals(new BigDecimal("10000"), BidStepPolicy.getDynamicStepPrice(new BigDecimal("99999")));
    }

    @Test
    void getDynamicStepPrice_usesExpectedBoundaries() {
        assertEquals(new BigDecimal("20000"), BidStepPolicy.getDynamicStepPrice(new BigDecimal("100000")));
        assertEquals(new BigDecimal("50000"), BidStepPolicy.getDynamicStepPrice(new BigDecimal("500000")));
        assertEquals(new BigDecimal("100000"), BidStepPolicy.getDynamicStepPrice(new BigDecimal("1000000")));
        assertEquals(new BigDecimal("200000"), BidStepPolicy.getDynamicStepPrice(new BigDecimal("5000000")));
        assertEquals(new BigDecimal("500000"), BidStepPolicy.getDynamicStepPrice(new BigDecimal("10000000")));
        assertEquals(new BigDecimal("1000000"), BidStepPolicy.getDynamicStepPrice(new BigDecimal("50000000")));
    }
}
