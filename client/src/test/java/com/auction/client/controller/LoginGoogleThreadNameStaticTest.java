package com.auction.client.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class LoginGoogleThreadNameStaticTest {

    private static final Path SOURCE = Path.of(
            "src/main/java/com/auction/client/controller/LoginController.java");

    @Test
    void googleLoginWorkerThreads_shouldBeNamedAndDaemon() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("google-login-config-worker"),
                "Google config worker thread should have a clear name.");
        assertTrue(source.contains("google-login-mock-submit-worker"),
                "Mock Google submit worker thread should have a clear name.");
        assertTrue(source.contains("google-login-oauth-submit-worker"),
                "OAuth Google submit worker thread should have a clear name.");
        assertTrue(source.contains("private void startDaemonThread(String threadName, Runnable task)"),
                "LoginController should use a small helper for named daemon threads.");
        assertTrue(source.contains("thread.setDaemon(true);"),
                "LoginController worker threads should be daemon threads.");
        assertFalse(source.contains("new Thread(() -> submitGoogleLoginPayload"),
                "Google login submit workers should not be started as unnamed direct threads.");
    }
}
