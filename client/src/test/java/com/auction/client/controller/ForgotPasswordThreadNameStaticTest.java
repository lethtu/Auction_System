package com.auction.client.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ForgotPasswordThreadNameStaticTest {

    @Test
    void forgotPasswordWorkerThreads_shouldBeNamedAndDaemon() throws IOException {
        Path sourcePath = Path.of("src/main/java/com/auction/client/controller/ForgotPasswordController.java");
        String source = Files.readString(sourcePath);

        assertTrue(source.contains("forgot-password-otp-worker"),
                "OTP worker thread should have a descriptive name.");
        assertTrue(source.contains("forgot-password-reset-worker"),
                "Reset worker thread should have a descriptive name.");
        assertTrue(source.contains("otpThread.setDaemon(true);"),
                "OTP worker thread should be daemon.");
        assertTrue(source.contains("resetThread.setDaemon(true);"),
                "Reset worker thread should be daemon.");

        String otpMethod = between(source, "public void handleGetOTP", "public void handleResetPassword");
        String resetMethod = between(source, "public void handleResetPassword", "public void setHttpClient");

        assertFalse(otpMethod.contains("}).start();"),
                "handleGetOTP should not start an anonymous thread directly; name the Thread variable first.");
        assertFalse(resetMethod.contains("}).start();"),
                "handleResetPassword should not start an anonymous thread directly; name the Thread variable first.");
    }

    private static String between(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start + startMarker.length());
        assertTrue(start >= 0, "Missing start marker: " + startMarker);
        assertTrue(end > start, "Missing end marker: " + endMarker);
        return source.substring(start, end);
    }
}
