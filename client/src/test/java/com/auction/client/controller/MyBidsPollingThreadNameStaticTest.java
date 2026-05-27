package com.auction.client.controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyBidsPollingThreadNameStaticTest {

    @Test
    void myBidsPollingExecutor_shouldUseNamedDaemonThread() throws IOException {
        Path source = Path.of("src/main/java/com/auction/client/controller/MyBidsController.java");
        String text = Files.readString(source);

        assertTrue(text.contains("new Thread(r, \"my-bids-polling-worker\")"),
                "MyBidsController polling executor should create a named worker thread.");
        assertFalse(text.contains("Thread t = new Thread(r);"),
                "Avoid anonymous polling threads in MyBidsController; name them for easier debugging.");
    }
}
