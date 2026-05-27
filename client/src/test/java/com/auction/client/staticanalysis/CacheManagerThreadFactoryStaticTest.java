package com.auction.client.staticanalysis;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheManagerThreadFactoryStaticTest {

    @Test
    void cacheExecutorThreadsShouldBeNamedAndDaemon() throws Exception {
        Path source = Path.of("src/main/java/com/auction/client/util/CacheManager.java");
        String text = Files.readString(source, StandardCharsets.UTF_8);

        assertTrue(
                text.contains("new Thread(runnable, \"cache-manager-worker\")"),
                "CacheManager executor threads should have stable names for debugging."
        );
        assertTrue(
                text.contains("thread.setDaemon(true);"),
                "CacheManager executor threads should remain daemon threads."
        );
        assertFalse(
                text.contains("new Thread(runnable);"),
                "Avoid anonymous CacheManager worker threads."
        );
    }
}
