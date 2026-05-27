package com.auction.client.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SignUpThreadNameStaticTest {

    @Test
    void signUpBackgroundThreads_shouldBeNamedAndDaemon() throws Exception {
        Path sourcePath = Path.of("src/main/java/com/auction/client/controller/SignUpController.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);

        assertTrue(source.contains("signup-submit-worker"),
                "Signup submit worker thread should have a clear name.");
        assertTrue(source.contains("signup-carousel-loader"),
                "Signup carousel loader thread should have a clear name.");
        assertTrue(source.contains("signupThread.setDaemon(true);"),
                "Signup submit thread should be daemon.");
        assertTrue(source.contains("carouselThread.setDaemon(true);"),
                "Signup carousel loader thread should be daemon.");
        assertFalse(source.contains("}).start();"),
                "SignUpController should not start anonymous background threads directly.");
    }
}
