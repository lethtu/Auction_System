package com.auction.client.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionPageBalanceThreadStaticTest {
    private static final Path SOURCE = Paths.get(
            "src/main/java/com/auction/client/controller/AuctionPageController.java");

    @Test
    void balanceRefreshThread_shouldBeNamedAndDaemon() throws Exception {
        String source = Files.readString(SOURCE);
        String method = extractMethod(source, "private void fetchLatestUserBalance()");

        assertTrue(method.contains("\"auction-user-balance-refresh\""),
                "fetchLatestUserBalance should name its worker thread.");
        assertTrue(method.contains("balanceThread.setDaemon(true);"),
                "fetchLatestUserBalance worker thread should be daemon.");
        assertTrue(method.contains("balanceThread.start();"),
                "fetchLatestUserBalance should start the named worker thread.");
        assertFalse(method.contains("}).start();"),
                "fetchLatestUserBalance should not start an anonymous thread directly.");
    }

    private static String extractMethod(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, signature + " not found.");

        int firstBrace = source.indexOf('{', start);
        assertTrue(firstBrace >= 0, signature + " opening brace not found.");

        int depth = 0;
        for (int i = firstBrace; i < source.length(); i++) {
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
