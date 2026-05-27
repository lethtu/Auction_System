package com.auction.client.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class LoginActiveProductsThreadNameStaticTest {

    @Test
    void loadActiveProducts_shouldUseNamedDaemonThreadHelper() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/auction/client/controller/LoginController.java"));
        String method = extractMethod(source, "private void loadActiveProducts()");

        assertTrue(method.contains("startDaemonThread(\"login-active-products-loader\""),
                "loadActiveProducts should use a named daemon helper thread.");
        assertFalse(method.contains("new Thread("),
                "loadActiveProducts should not create an unnamed thread directly.");
        assertTrue(source.contains("thread.setDaemon(true);"),
                "LoginController should mark helper-created worker threads as daemon threads.");
    }

    private static String extractMethod(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, signature + " not found.");

        int braceStart = source.indexOf('{', start);
        assertTrue(braceStart >= 0, signature + " opening brace not found.");

        int depth = 0;
        for (int i = braceStart; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(start, i + 1);
                }
            }
        }

        fail(signature + " closing brace not found.");
        return "";
    }
}
