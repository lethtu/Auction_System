package com.auction.client.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellerDashboardUploadThreadStaticTest {

    @Test
    void sellerUploadThreads_shouldUseNamedDaemonHelper() throws Exception {
        Path sourcePath = Path.of("src/main/java/com/auction/client/controller/SellerDashboardController.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);

        assertTrue(source.contains("private void startDaemonThread(String threadName, Runnable task)"),
                "SellerDashboardController should have a small daemon-thread helper.");
        assertTrue(source.contains("thread.setDaemon(true);"),
                "SellerDashboard upload helper should mark background threads as daemon.");

        String imageMethod = extractMethod(source, "private void startImageUpload(File file)");
        assertTrue(imageMethod.contains("startDaemonThread(\"seller-image-upload-worker\""),
                "startImageUpload should use a named daemon thread.");
        assertFalse(imageMethod.contains("}).start();"),
                "startImageUpload should not start an anonymous thread directly.");

        String modelMethod = extractMethod(source, "private void startModelUpload(File file)");
        assertTrue(modelMethod.contains("startDaemonThread(\"seller-model-upload-worker\""),
                "startModelUpload should use a named daemon thread.");
        assertFalse(modelMethod.contains("}).start();"),
                "startModelUpload should not start an anonymous thread directly.");
    }

    private static String extractMethod(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, signature + " not found.");

        int openBrace = source.indexOf('{', start);
        assertTrue(openBrace >= 0, signature + " opening brace not found.");

        int depth = 0;
        for (int i = openBrace; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(start, i + 1);
                }
            }
        }

        throw new AssertionError(signature + " closing brace not found.");
    }
}
