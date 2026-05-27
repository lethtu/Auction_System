package com.auction.client.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GoogleOAuthThreadNameStaticTest {

    @Test
    void googleOAuthCallbackThreads_shouldBeNamedAndDaemon() throws Exception {
        Path source = Path.of("src/main/java/com/auction/client/util/GoogleOAuthService.java");
        String code = Files.readString(source, StandardCharsets.UTF_8);

        assertTrue(code.contains("google-oauth-success-callback"));
        assertTrue(code.contains("google-oauth-failure-callback"));
        assertTrue(code.contains("google-oauth-stop-server"));
        assertTrue(code.contains("successCallbackThread.setDaemon(true);"));
        assertTrue(code.contains("failureCallbackThread.setDaemon(true);"));
        assertTrue(code.contains("stopServerThread.setDaemon(true);"));

        assertFalse(code.contains("new Thread(() -> callback.onSuccess(finalCode, redirectUri)).start();"));
        assertFalse(code.contains("new Thread(() -> callback.onFailure(finalError)).start();"));
        assertFalse(code.contains("new Thread(this::stopServer).start();"));
    }
}
