package com.auction.client.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DepositThreadNameStaticTest {

    @Test
    void fetchLatestBalanceThread_shouldBeNamedAndDaemon() throws Exception {
        Path source = Path.of("src/main/java/com/auction/client/controller/DepositController.java");
        String text = Files.readString(source, StandardCharsets.UTF_8);

        int start = text.indexOf("private void fetchLatestBalance()");
        assertTrue(start >= 0, "fetchLatestBalance method should exist");

        int end = text.indexOf("    @FXML", start);
        assertTrue(end > start, "fetchLatestBalance method end should be detectable");

        String method = text.substring(start, end);
        assertTrue(method.contains("deposit-balance-refresh"), "balance refresh thread should have a readable name");
        assertTrue(method.contains("balanceThread.setDaemon(true);"), "balance refresh thread should be daemon");
        assertTrue(method.contains("balanceThread.start();"), "balance refresh thread should be started explicitly");
    }
}
