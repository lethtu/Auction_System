package com.auction.client.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionPageThreadNameStaticTest {

    private static final Path SOURCE = Path.of(
            "src/main/java/com/auction/client/controller/AuctionPageController.java");

    @Test
    void auctionPageBackgroundThreads_shouldBeNamedAndDaemonFriendly() throws Exception {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);

        assertTrue(source.contains("auction-bid-history-loader"),
                "Bid history loader thread should have a descriptive name.");
        assertTrue(source.contains("auction-session-refresh"),
                "Session refresh thread should have a descriptive name.");
        assertTrue(source.contains("auction-socket-listener"),
                "Socket listener thread should have a descriptive name.");

        assertFalse(source.contains("listenerThread = new Thread(this::listenToSocketServer);"),
                "Socket listener should not be created as an unnamed thread.");
    }
}
