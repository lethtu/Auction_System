package com.auction.client.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepositSubmitThreadDaemonStaticTest {

    @Test
    void depositSubmitWorker_shouldBeDaemonAndNamed() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/auction/client/controller/DepositController.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(source.contains("startDaemonThread(\"deposit-money\""),
                "Deposit submit worker should be started through the daemon helper.");
        assertTrue(source.contains("thread.setDaemon(true);"),
                "Background deposit worker threads should be daemon threads.");
        assertFalse(source.contains("}, \"deposit-money\").start();"),
                "Deposit submit worker should not use chained Thread.start() because daemon cannot be set safely.");
    }
}
