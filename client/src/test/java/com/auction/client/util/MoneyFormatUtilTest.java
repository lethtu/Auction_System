package com.auction.client.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyFormatUtilTest {

    @Test
    void formatGrouped_nullValue_returnsZeroText() {
        assertEquals("0", MoneyFormatUtil.formatGrouped(null));
    }

    @Test
    void formatGrouped_bigAmount_usesVietnameseGroupingSeparator() {
        assertEquals("1.234.567", MoneyFormatUtil.formatGrouped(new BigDecimal("1234567")));
    }

    @Test
    void formatVndSuffix_appendsDongSymbol() {
        assertEquals("1.234.567 \u20ab", MoneyFormatUtil.formatVndSuffix(new BigDecimal("1234567")));
    }

    @Test
    void formatVndCode_bigAmount_usesVietnameseGroupingAndVndCode() {
        assertEquals("1.234.567 VND", MoneyFormatUtil.formatVndCode(new BigDecimal("1234567")));
    }

    @Test
    void formatVndCode_nullValue_returnsZeroVnd() {
        assertEquals("0 VND", MoneyFormatUtil.formatVndCode(null));
    }

    @Test
    void parseMoneyInput_removesCurrencySymbolSpacesAndSeparators() {
        assertEquals(new BigDecimal("1234567"), MoneyFormatUtil.parseMoneyInput(" \u20ab 1.234,567 "));
        assertEquals(new BigDecimal("900000"), MoneyFormatUtil.parseMoneyInput("900.000 \u0111"));
    }

    @Test
    void parseMoneyInput_invalidValue_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> MoneyFormatUtil.parseMoneyInput("abc"));
    }
}
