package com.auction.client.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginControllerComingSoonStaticTest {

    @Test
    void facebookComingSoon_shouldUseJavaFxPauseTransitionInsteadOfSleepingThread() throws Exception {
        Path source = Path.of("src/main/java/com/auction/client/controller/LoginController.java");
        String text = Files.readString(source, StandardCharsets.UTF_8);
        String method = extractMethodByBraceDepth(text, "private void handleComingSoonButton(Button button)");

        assertTrue(method.contains("PauseTransition restoreButton = new PauseTransition(Duration.seconds(2));"),
                "handleComingSoonButton should use JavaFX PauseTransition for temporary button feedback.");
        assertFalse(method.contains("Thread.sleep(2000)"),
                "handleComingSoonButton should not block a background thread just to restore a temporary button label.");
        assertFalse(method.contains("new Thread(() ->"),
                "handleComingSoonButton should not create an ad-hoc thread for a simple JavaFX UI delay.");
    }

    private static String extractMethodByBraceDepth(String text, String methodSignature) {
        int start = text.indexOf(methodSignature);
        assertTrue(start >= 0, methodSignature + " not found.");

        int openingBrace = text.indexOf('{', start);
        assertTrue(openingBrace > start, "Method opening brace not found.");

        int depth = 0;
        for (int i = openingBrace; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }

        throw new AssertionError("Method closing brace not found.");
    }
}
