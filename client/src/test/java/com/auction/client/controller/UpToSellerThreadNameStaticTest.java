package com.auction.client.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpToSellerThreadNameStaticTest {
    @Test
    void upgradeWorkerThread_shouldBeNamedAndDaemon() throws Exception {
        Path sourcePath = Path.of("src/main/java/com/auction/client/controller/UpToSellerController.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8).replace("\r\n", "\n");

        assertTrue(source.contains("up-to-seller-worker"),
                "Upgrade worker thread should have a clear name for debugging.");
        assertTrue(source.contains("upgradeThread.setDaemon(true);"),
                "Upgrade worker thread should be daemon so it cannot keep the app alive on exit.");
        assertFalse(source.contains("        new Thread(() -> {\n            try {"),
                "Do not start the upgrade worker as an anonymous unnamed thread.");
    }
}
