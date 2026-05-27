package com.auction.client.staticanalysis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ClientLoggerFileIoStaticTest {

    @Test
    void clientLogger_shouldUseUtf8NioFileIo() throws Exception {
        Path source = Path.of("src/main/java/com/auction/client/service/ClientLogger.java");
        String content = Files.readString(source, StandardCharsets.UTF_8);

        assertTrue(content.contains("StandardCharsets.UTF_8"), "ClientLogger should read/write logs with explicit UTF-8.");
        assertTrue(content.contains("Files.writeString"), "ClientLogger should write logs through java.nio.file.Files.");
        assertTrue(content.contains("Files.newBufferedReader"), "ClientLogger should read logs through java.nio.file.Files.");

        assertFalse(content.contains("FileWriter"), "Avoid FileWriter because it uses the platform default charset.");
        assertFalse(content.contains("FileReader"), "Avoid FileReader because it uses the platform default charset.");
        assertFalse(content.contains("new File("), "Prefer Path/Files over legacy File API in ClientLogger.");
    }
}
