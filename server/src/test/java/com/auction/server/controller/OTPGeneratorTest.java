package com.auction.server.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OTPGeneratorTest {

    @Test
    void generateOTP_zeroLength_returnsEmptyString() {
        assertEquals("", OTPGenerator.generateOTP(0));
    }

    @Test
    void generateOTP_positiveLength_returnsRequestedLengthAndAllowedCharacters() {
        String otp = OTPGenerator.generateOTP(32);

        assertEquals(32, otp.length());
        assertTrue(otp.matches("[0-9A-Za-z]+"));
    }
}